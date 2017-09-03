package com.jafpl.runtime

import akka.actor.{ActorRef, ActorSystem, DeadLetter, Props}
import com.jafpl.exceptions.{GraphException, PipelineException}
import com.jafpl.graph.{AtomicNode, Binding, Buffer, CatchStart, ChooseStart, ContainerEnd, ContainerStart, EmptySource, FinallyStart, Graph, GraphInput, GraphOutput, GroupStart, Joiner, LoopEachStart, LoopForStart, LoopUntilStart, LoopWhileStart, PipelineStart, Sink, Splitter, TryCatchStart, TryStart, ViewportStart, WhenStart}
import com.jafpl.runtime.GraphMonitor.{GAbortExecution, GException, GNode, GRun, GWatchdog}
import com.jafpl.steps.{BindingProvider, DataConsumer, DataConsumerProxy}
import com.jafpl.util.{DeadLetterListener, UniqueId}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

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
  private var _system: ActorSystem = _
  private var _monitor: ActorRef = _
  private val sleepInterval = 100
  private var _started = false
  private var _finished = false
  private var _exception = Option.empty[Throwable]
  private var _graphInputs = mutable.HashMap.empty[String, InputProxy]
  private var _graphBindings = mutable.HashMap.empty[String, BindingProxy]
  private var _graphOutputs = mutable.HashMap.empty[String, OutputProxy]

  graph.close()

  if (!graph.valid) {
    throw new GraphException("Cannot run an invalid graph", None)
  }

  makeActors()

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
  def inputs: Map[String, DataConsumer] = Map() ++ _graphInputs

  /** A map of the variable bindings that the pipeline expects.
    *
    * This mapping from names (strings) to [[com.jafpl.steps.BindingProvider]]s is the set of variable
    * bindings that
    * the pipeline expects from the outside world. If you do not provide an input,
    * the name will be unbound. The result of referring to an unbound variable
    * is undefined.
    *
    * @return A map of the expected variable bindings.
    */
  def bindings: Map[String, BindingProvider] =  Map() ++ _graphBindings

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

  protected[runtime] def finish(cause: Throwable): Unit = {
    finish()
    _exception = Some(cause)
  }

  /** Runs the pipeline.
    *
    * This method will block until pipeline execution finishes.
    *
    */
  def run(): Unit = {
    for ((port, proxy) <- inputs) {
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
    for ((name, provider) <- _graphBindings) {
      if (!provider.closed) {
        throw new PipelineException("nobinding", "No binding was provided for " + name, None)
      }
    }

    _monitor ! GRun()
    _started = true
  }


  /** Wait for pipeline execution to complete.
    *
    * If you start a pipeline int he background and then wish to block until execution
    * completes, call this method.
    */
  def waitForPipeline(): Unit = {
    if (!started) {
      _exception = Some(new PipelineException("notstarted", "Pipeline execution has not started", None))
      throw _exception.get
    }
    waitForTeardown()
  }

  private def waitForTeardown(): Unit = {
    if (!finished) {
      val watchdog = runtime.watchdogTimeout
      if (watchdog < 0) {
        _monitor ! GException(None,
          new PipelineException("invwatchdog", "The watchdog timer value must have a non-negative value", None))
      }

      var ticker = watchdog
      while (!_finished) {
        if (ticker <= 0) {
          ticker = watchdog
          if (watchdog != 0) {
            _monitor ! GWatchdog(ticker)
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
    _monitor ! GAbortExecution()
    waitForTeardown()
  }

  private def makeActors(): Unit = {
    _system = ActorSystem("jafpl-com-" + UniqueId.nextId)
    _monitor = _system.actorOf(Props(new GraphMonitor(graph, this)), name = "monitor")

    val listener = _system.actorOf(Props[DeadLetterListener])
    _system.eventStream.subscribe(listener, classOf[DeadLetter])

    for (node <- graph.nodes) {
      var actorName = "_" * (7 - node.id.length) + node.id

      val actor = node match {
        case split: Splitter =>
          _system.actorOf(Props(new SplitterActor(_monitor, this, split)), actorName)
        case join: Joiner =>
          _system.actorOf(Props(new JoinerActor(_monitor, this, join)), actorName)
        case buf: Buffer =>
          _system.actorOf(Props(new BufferActor(_monitor, this, buf)), actorName)
        case sink: Sink =>
          _system.actorOf(Props(new SinkActor(_monitor, this, sink)), actorName)
        case source: EmptySource =>
          _system.actorOf(Props(new EmptySourceActor(_monitor, this, source)), actorName)
        case forEach: LoopEachStart =>
          _system.actorOf(Props(new LoopEachActor(_monitor, this, forEach)), actorName)
        case viewport: ViewportStart =>
          _system.actorOf(Props(new ViewportActor(_monitor, this, viewport)), actorName)
        case choose: ChooseStart =>
          _system.actorOf(Props(new ChooseActor(_monitor, this, choose)), actorName)
        case when: WhenStart =>
          _system.actorOf(Props(new WhenActor(_monitor, this, when)), actorName)
        case forLoop: LoopForStart =>
          _system.actorOf(Props(new LoopForActor(_monitor, this, forLoop)), actorName)
        case trycatch: TryCatchStart =>
          _system.actorOf(Props(new TryCatchActor(_monitor, this, trycatch)), actorName)
        case trycatch: TryStart =>
          _system.actorOf(Props(new StartActor(_monitor, this, trycatch)), actorName)
        case trycatch: CatchStart =>
          _system.actorOf(Props(new CatchActor(_monitor, this, trycatch)), actorName)
        case fin: FinallyStart =>
          _system.actorOf(Props(new FinallyActor(_monitor, this, fin)), actorName)
        case pipe: PipelineStart =>
          _system.actorOf(Props(new PipelineActor(_monitor, this, pipe)), actorName)
        case group: GroupStart =>
          _system.actorOf(Props(new StartActor(_monitor, this, group)), actorName)
        case wstart: LoopWhileStart =>
          _system.actorOf(Props(new LoopWhileActor(_monitor, this, wstart)), actorName)
        case start: LoopUntilStart =>
          _system.actorOf(Props(new LoopUntilActor(_monitor, this, start)), actorName)
        case start: ContainerStart =>
          throw new GraphException("Attempt to instantiate naked container: " + start, start.location)
        case end: ContainerEnd =>
          end.start.get match {
            case trycatch: TryCatchStart =>
              _system.actorOf(Props(new TryCatchEndActor(_monitor, this, end)), actorName)
            case trycatch: TryStart =>
              _system.actorOf(Props(new ConditionalEndActor(_monitor, this, end)), actorName)
            case trycatch: CatchStart =>
              _system.actorOf(Props(new ConditionalEndActor(_monitor, this, end)), actorName)
            case trycatch: WhenStart =>
              _system.actorOf(Props(new ConditionalEndActor(_monitor, this, end)), actorName)
            case foreach: LoopEachStart =>
              _system.actorOf(Props(new LoopEachEndActor(_monitor, this, end)), actorName)
            case wstart: LoopWhileStart =>
              _system.actorOf(Props(new LoopWhileEndActor(_monitor, this, end)), actorName)
            case start: LoopUntilStart =>
              _system.actorOf(Props(new LoopUntilEndActor(_monitor, this, end)), actorName)
            case viewport: ViewportStart =>
              _system.actorOf(Props(new ViewportEndActor(_monitor, this, end)), actorName)
            case _ =>
              _system.actorOf(Props(new EndActor(_monitor, this, end)), actorName)
          }
        case req: GraphInput =>
          if (_graphInputs.contains(req.name)) {
            throw new PipelineException("dupname", s"Input port name repeated: ${req.name}", req.location)
          }
          val ip = new InputProxy(_monitor, this, node)
          _graphInputs.put(req.name, ip)
          _system.actorOf(Props(new InputActor(_monitor, this, node, ip)), actorName)
        case req: GraphOutput =>
          if (_graphOutputs.contains(req.name)) {
            throw new PipelineException("dupname", s"Output port name repeated: ${req.name}", req.location)
          }
          val op = new OutputProxy(_monitor, this, node)
          _graphOutputs.put(req.name, op)
          _system.actorOf(Props(new OutputActor(_monitor, this, node, op)), actorName)
        case req: Binding =>
          if (req.expression.isDefined) {
            _system.actorOf(Props(new VariableActor(_monitor, this, req)), actorName)
          } else {
            if (_graphBindings.contains(req.name)) {
              throw new PipelineException("dupname", s"Input binding name repeated: ${req.name}", req.location)
            }
            val ip = new BindingProxy(_monitor, this, req)
            _graphBindings.put(req.name, ip)
            _system.actorOf(Props(new BindingActor(_monitor, this, req, ip)), actorName)
          }
        case atomic: AtomicNode =>
          if (node.step.isDefined) {
            val cp = new ConsumingProxy(_monitor, this, node)
            node.step.get.setConsumer(cp)
            _system.actorOf(Props(new NodeActor(_monitor, this, node, cp)), actorName)
          } else {
            _system.actorOf(Props(new NodeActor(_monitor, this, node)), actorName)
          }

        case _ =>
          throw new PipelineException("unexpected", s"Unexpected step type: $node", node.location)
      }

      _monitor ! GNode(node, actor)
    }
  }
}
