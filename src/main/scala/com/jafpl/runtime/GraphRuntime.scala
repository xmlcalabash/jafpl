package com.jafpl.runtime

import akka.actor.{ActorRef, ActorSystem, DeadLetter, Props}
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{AtomicNode, Binding, Buffer, CatchStart, ChooseStart, ContainerStart, EmptySource, FinallyStart, Graph, GraphInput, GraphOutput, GroupStart, Joiner, LoopEachStart, LoopForStart, LoopUntilStart, LoopWhileStart, OptionBinding, PipelineStart, Sink, Splitter, TryCatchStart, TryStart, ViewportStart, WhenStart}
import com.jafpl.runtime.NodeActor.{NAbortExecution, NNode, NRunIfReady, NWatchdog, NWatchdogTimeout}
import com.jafpl.runtime.Reaper.WatchMe
import com.jafpl.steps.{DataConsumerProxy, DataProvider}
import com.jafpl.util.{DeadLetterListener, DefaultTraceEventManager, TraceEventManager, UniqueId}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/** Execute a pipeline.
  *
  * The graph runtime executes a pipeline.
  *
  * If the specified graph is open, it will be closed. If it is not valid, an exception is thrown.
  *
  * Constructing the runtime builds the network of Akka actors. If for some reason you decide not
  * to run the pipeline, you must call `stop` to tear it down.
  *
  * @constructor A graph runtime.
  * @param graph The graph to execute.
  * @param runtime Runtime context information for the execution.
  */
class GraphRuntime(val graph: Graph, val runtime: RuntimeConfiguration) {
  protected[jafpl] val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val actorList = ListBuffer.empty[ActorRef]
  private var _system: ActorSystem = _
  private var _monitor: ActorRef = _
  private var reaper: ActorRef = _
  private val sleepInterval = 100
  private var _started = false
  private var _finished = false
  private val _graphInputs = mutable.HashMap.empty[String, InputProxy]
  private val _graphOutputs = mutable.HashMap.empty[String, OutputProxy]
  private val _graphOptions = mutable.HashMap.empty[String, OptionBinding]
  private var _traceEventManager: TraceEventManager = new DefaultTraceEventManager()
  private var _exception = Option.empty[Exception]

  graph.close()

  if (!graph.valid) {
    throw JafplException.invalidGraph()
  }

  try {
    makeActors()
  } catch {
    case ex: Exception =>
      for (actor <- actorList) {
        _system.stop(actor)
      }
      throw ex
  }

  def traceEventManager: TraceEventManager = _traceEventManager
  def traceEventManager_=(manager: TraceEventManager): Unit = {
    _traceEventManager = manager
  }

  /** Returns true if the pipeline execution has started.
    */
  def started: Boolean = _started

  /** Returns true if the pipeline execution has finished.
    */
  def finished: Boolean = _finished

  /** Returns an optional exception raised during execution.
    *
    * If the pipeline execution halts because an uncaught exception is thrown, the
    * exception is available from here after the pipeline finishes.
    */
  def exception: Option[Throwable] = _exception

  /** A map of the inputs that the pipeline expects.
    *
    * This mapping from names (strings) to [[com.jafpl.steps.DataConsumer]]s is the set of inputs that
    * the pipeline expects from the outside world. If you do not provide an input, an
    * empty sequence of items will be provided.
    *
    * @return A map of the expected inputs.
    */
  def inputs: Map[String, DataProvider] = Map() ++ _graphInputs

  /** A map of the variable bindings that the pipeline expects.
    *
    * This mapping from names (strings) to [[com.jafpl.steps.BindingProvider]]s is the set of variable
    * bindings that the pipeline expects from the outside world. If you do not provide an input,
    * the name will be unbound. The result of referring to an unbound variable
    * is undefined.
    *
    * @return A map of the expected variable bindings.
    */
  //def bindings: Map[String, BindingProvider] =  Map() ++ _graphBindings

  /** A map of the outputs that the pipeline produces.
    *
    * This mapping from names (strings) to [[com.jafpl.steps.DataConsumerProxy]]s is the set of outputs that
    * the pipeline. If you do not call the `setProvider` method, the output will be
    * discarded.
    *
    * @return A map of the expected inputs.
    */
  def outputs: Map[String, DataConsumerProxy] = Map() ++ _graphOutputs

  protected[runtime] def finish(): Unit = {
    _finished = true
  }

  protected[runtime] def finish(cause: Exception): Unit = {
    _exception = Some(cause)
    finish()
  }

  /** Runs the pipeline.
    *
    * This method will block until pipeline execution finishes.
    *
    */
  def run(): Unit = {
    for (proxy <- inputs.values) {
      proxy.asInstanceOf[InputProxy].close()
    }

    if (graph.valid) {
      runInBackground()
      waitForPipeline()
    }
  }

  /** Runs the pipeline.
    *
    * This method starts execution of the pipeline and then returns immediately.
    * The pipeline will continue to run in other threads.
    *
    * To determine if execution has completed, check the `finished` value.
    */
  def runInBackground(): Unit = {
    _monitor ! NRunIfReady()
    _started = true
  }

  /** Wait for pipeline execution to complete.
    *
    * If you start a pipeline in the background and then wish to block until execution
    * completes, call this method.
    */
  def waitForPipeline(): Unit = {
    if (!started) {
      _exception = Some(JafplException.notRunning())
      throw _exception.get
    }
    waitForTeardown()
  }

  private def waitForTeardown(): Unit = {
    if (!finished) {
      val watchdog = runtime.watchdogTimeout + 1000 //FIXME:
      if (watchdog < 0) {
        _monitor ! NWatchdogTimeout()
      }

      var ticker = watchdog
      while (!_finished) {
        if (ticker <= 0) {
          ticker = watchdog
          if (watchdog != 0) {
            _monitor ! NWatchdog(ticker)
          }
        }
        Thread.sleep(sleepInterval)
        ticker -= sleepInterval
      }

      if (exception.isDefined) {
        throw exception.get
      }
    }
  }

  def stop(): Unit = {
    _monitor ! NAbortExecution()
    waitForTeardown()
  }

  private def makeActors(): Unit = {
    val name = "jafpl-com-" + UniqueId.nextId

    logger.debug(s"Creating $name for $graph ($this)")

    _system = ActorSystem(name)
    _monitor = _system.actorOf(Props(new GraphMonitor(graph, this)), name = "monitor")
    reaper = _system.actorOf(Props(new Reaper(runtime)), name = "reaper")

    val listener = _system.actorOf(Props[DeadLetterListener])
    _system.eventStream.subscribe(listener, classOf[DeadLetter])

    for (node <- graph.nodes) {
      var actorName = "_" * (7 - node.id.length) + node.id

      val actor = node match {
        case act: PipelineStart => _system.actorOf(Props(new PipelineActor(_monitor, this, act)), actorName)
        case act: Splitter => _system.actorOf(Props(new SplitterActor(_monitor, this, act)), actorName)
        case act: Joiner => _system.actorOf(Props(new JoinerActor(_monitor, this, act)), actorName)
        case act: GroupStart => _system.actorOf(Props(new GroupActor(_monitor, this, act)), actorName)
        case act: LoopEachStart => _system.actorOf(Props(new LoopForEachActor(_monitor, this, act)), actorName)
        case act: LoopForStart => _system.actorOf(Props(new LoopForCountActor(_monitor, this, act)), actorName)
        case act: Buffer => _system.actorOf(Props(new BufferActor(_monitor, this, act)), actorName)
        case act: Sink => _system.actorOf(Props(new SinkActor(_monitor, this, act)), actorName)
        case act: EmptySource => _system.actorOf(Props(new EmptySourceActor(_monitor, this, act)), actorName)
        case act: LoopWhileStart => _system.actorOf(Props(new LoopWhileActor(_monitor, this, act)), actorName)
        case act: LoopUntilStart => _system.actorOf(Props(new LoopUntilActor(_monitor, this, act)), actorName)
        case act: ChooseStart => _system.actorOf(Props(new ChooseActor(_monitor, this, act)), actorName)
        case act: WhenStart => _system.actorOf(Props(new WhenActor(_monitor, this, act)), actorName)
        case act: TryCatchStart => _system.actorOf(Props(new TryCatchActor(_monitor, this, act)), actorName)
        case act: TryStart => _system.actorOf(Props(new TryActor(_monitor, this, act)), actorName)
        case act: CatchStart => _system.actorOf(Props(new CatchActor(_monitor, this, act)), actorName)
        case act: FinallyStart => _system.actorOf(Props(new FinallyActor(_monitor, this, act)), actorName)
        case act: ViewportStart => _system.actorOf(Props(new ViewportActor(_monitor, this, act)), actorName)
        case act: ContainerStart => throw JafplException.abstractContainer(act.toString, act.location)
        case req: GraphInput =>
          if (_graphInputs.contains(req.name)) {
            throw JafplException.dupInputPort(req.name, req.location)
          }
          val ip = new InputProxy(_monitor, this, node)
          _graphInputs.put(req.name, ip)
          _system.actorOf(Props(new InputActor(_monitor, this, req, ip)), actorName)
        case req: GraphOutput =>
          if (_graphOutputs.contains(req.name)) {
            throw JafplException.dupOutputPort(req.name, req.location)
          }
          val op = new OutputProxy(_monitor, this, node)
          _graphOutputs.put(req.name, op)
          _system.actorOf(Props(new OutputActor(_monitor, this, req, op)), actorName)
        case req: OptionBinding =>
          _graphOptions.put(req.name, req)
          _system.actorOf(Props(new VariableActor(_monitor, this, req)), actorName)
        case req: Binding =>
          _system.actorOf(Props(new VariableActor(_monitor, this, req)), actorName)
        case atomic: AtomicNode =>
          _system.actorOf(Props(new AtomicActor(_monitor, this, atomic)), actorName)

        case _ =>
          throw JafplException.unexpecteStepType(node.toString, node.location)
      }

      actorList += actor
      reaper ! WatchMe(actor)
      _monitor ! NNode(node, actor)
    }
  }
}
