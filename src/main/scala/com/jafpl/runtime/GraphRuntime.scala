package com.jafpl.runtime

import java.time.{Duration, Instant}

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
  private var system: ActorSystem = _
  private var monitor: ActorRef = _
  private var reaper: ActorRef = _
  private val sleepInterval = 100
  private var _started = false
  private var _finished = false
  private val _graphInputs = mutable.HashMap.empty[String, InputProxy]
  private val _graphOutputs = mutable.HashMap.empty[String, OutputProxy]
  private val _graphOptions = mutable.HashMap.empty[String, OptionBinding]
  private var _traceEventManager: TraceEventManager = new DefaultTraceEventManager()
  private var _exception = Option.empty[Exception]

  // I'm not absolutely certain this is necessary, but ...
  // The other way to do this would be to just pass messages for it.
  private[this] val messageLock = new Object()
  private var lastMessage = Instant.now()

  graph.close()

  if (!graph.valid) {
    throw JafplException.invalidGraph()
  }

  try {
    makeActors()
  } catch {
    case ex: Exception =>
      for (actor <- actorList) {
        system.stop(actor)
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
    monitor ! NRunIfReady()
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
      val watchdog = runtime.watchdogTimeout
      if (watchdog < 0) {
        monitor ! NWatchdogTimeout()
      }

      var ticker = watchdog
      while (!_finished) {
        if (ticker <= 0) {
          ticker = watchdog
          // Ignore the watchdog if it'ss set to zero
          if (watchdog != 0) {
            monitor ! NWatchdog(ticker)
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
    monitor ! NAbortExecution()
    waitForTeardown()
  }

  protected[runtime] def noteMessageTime(): Unit = {
    messageLock.synchronized {
      lastMessage = Instant.now()
    }
  }

  protected[runtime] def lastMessageAge: Long = {
    Duration.between(lastMessage, Instant.now()).toMillis
  }

  private def makeActors(): Unit = {
    val name = "jafpl-com-" + UniqueId.nextId

    logger.debug(s"Creating $name for $graph ($this)")

    system = ActorSystem(name)
    monitor = system.actorOf(Props(new GraphMonitor(graph, this)), name = "monitor")
    reaper = system.actorOf(Props(new Reaper(runtime)), name = "reaper")

    val listener = system.actorOf(Props[DeadLetterListener])
    system.eventStream.subscribe(listener, classOf[DeadLetter])

    for (node <- graph.nodes) {
      var actorName = "_" * (7 - node.id.length) + node.id

      val actor = node match {
        case act: PipelineStart => system.actorOf(Props(new PipelineActor(monitor, this, act)), actorName)
        case act: Splitter => system.actorOf(Props(new SplitterActor(monitor, this, act)), actorName)
        case act: Joiner => system.actorOf(Props(new JoinerActor(monitor, this, act)), actorName)
        case act: GroupStart => system.actorOf(Props(new GroupActor(monitor, this, act)), actorName)
        case act: LoopEachStart => system.actorOf(Props(new LoopForEachActor(monitor, this, act)), actorName)
        case act: LoopForStart => system.actorOf(Props(new LoopForCountActor(monitor, this, act)), actorName)
        case act: Buffer => system.actorOf(Props(new BufferActor(monitor, this, act)), actorName)
        case act: Sink => system.actorOf(Props(new SinkActor(monitor, this, act)), actorName)
        case act: EmptySource => system.actorOf(Props(new EmptySourceActor(monitor, this, act)), actorName)
        case act: LoopWhileStart => system.actorOf(Props(new LoopWhileActor(monitor, this, act)), actorName)
        case act: LoopUntilStart => system.actorOf(Props(new LoopUntilActor(monitor, this, act)), actorName)
        case act: ChooseStart => system.actorOf(Props(new ChooseActor(monitor, this, act)), actorName)
        case act: WhenStart => system.actorOf(Props(new WhenActor(monitor, this, act)), actorName)
        case act: TryCatchStart => system.actorOf(Props(new TryCatchActor(monitor, this, act)), actorName)
        case act: TryStart => system.actorOf(Props(new TryActor(monitor, this, act)), actorName)
        case act: CatchStart => system.actorOf(Props(new CatchActor(monitor, this, act)), actorName)
        case act: FinallyStart => system.actorOf(Props(new FinallyActor(monitor, this, act)), actorName)
        case act: ViewportStart => system.actorOf(Props(new ViewportActor(monitor, this, act)), actorName)
        case act: ContainerStart => throw JafplException.abstractContainer(act.toString, act.location)
        case req: GraphInput =>
          if (_graphInputs.contains(req.name)) {
            throw JafplException.dupInputPort(req.name, req.location)
          }
          val ip = new InputProxy(monitor, this, node)
          _graphInputs.put(req.name, ip)
          system.actorOf(Props(new InputActor(monitor, this, req, ip)), actorName)
        case req: GraphOutput =>
          if (_graphOutputs.contains(req.name)) {
            throw JafplException.dupOutputPort(req.name, req.location)
          }
          val op = new OutputProxy(monitor, this, node)
          _graphOutputs.put(req.name, op)
          system.actorOf(Props(new OutputActor(monitor, this, req, op)), actorName)
        case req: OptionBinding =>
          _graphOptions.put(req.name, req)
          system.actorOf(Props(new VariableActor(monitor, this, req)), actorName)
        case req: Binding =>
          system.actorOf(Props(new VariableActor(monitor, this, req)), actorName)
        case atomic: AtomicNode =>
          system.actorOf(Props(new AtomicActor(monitor, this, atomic)), actorName)

        case _ =>
          throw JafplException.unexpecteStepType(node.toString, node.location)
      }

      actorList += actor
      reaper ! WatchMe(actor)
      monitor ! NNode(node, actor)
    }
  }
}
