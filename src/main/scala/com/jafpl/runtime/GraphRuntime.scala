package com.jafpl.runtime

import akka.actor.{ActorRef, ActorSystem, Props}
import com.jafpl.exceptions.{GraphException, PipelineException}
import com.jafpl.graph.{AtomicNode, Binding, Buffer, CatchStart, ChooseStart, ContainerEnd, ContainerStart, ForEachStart, Graph, GraphInput, GraphOutput, GroupStart, Joiner, PipelineStart, Splitter, TryCatchStart, TryStart, ViewportStart, WhenStart, WhileStart}
import com.jafpl.runtime.GraphMonitor.{GNode, GRun, GWatchdog}
import com.jafpl.steps.{BindingProvider, DataConsumer, DataProvider}
import com.jafpl.util.UniqueId
import org.slf4j.LoggerFactory

import scala.collection.immutable.HashMap
import scala.collection.mutable

/** Execute a pipeline.
  *
  * The graph runtime executes a pipeline.
  *
  * If the specified graph is open, it will be closed. If it is not valid, an exception is thrown.
  *
  * @constructor A graph runtime.
  * @param graph The graph to execute.
  * @param dynamicContext Runtime context information for the execution.
  */
class GraphRuntime(val graph: Graph, val dynamicContext: RuntimeConfiguration) {
  protected[jafpl] val logger = LoggerFactory.getLogger(this.getClass)
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
    * This mapping from names (strings) to [[com.jafpl.steps.DataProvider]]s is the set of inputs that
    * the pipeline expects from the outside world. If you do not provide an input, an
    * empty sequence of items will be provided.
    *
    * @return A map of the expected inputs.
    */
  def inputs: Map[String, DataProvider] = Map() ++ _graphInputs

  /** A map of the variable bindings that the pipeline expects.
    *
    * This mapping from names (strings) to [[com.jafpl.steps.DataProvider]]s is the set of variable
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
    * This mapping from names (strings) to [[com.jafpl.steps.DataConsumer]]s is the set of outputs that
    * the pipeline. If you do not call the `setProvider` method, the output will be
    * discarded.
    *
    * @return A map of the expected inputs.
    */
  def outputs: Map[String, DataConsumer] = Map() ++ _graphOutputs

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
    for (provider <- inputs.values) {
      provider.close()
    }
    for ((name, provider) <- _graphBindings) {
      if (!provider.closed) {
        throw new PipelineException("nobinding", "No binding was provided for " + name)
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
      _exception = Some(new PipelineException("notstarted", "Pipeline execution has not started"))
      throw _exception.get
    }

    if (!finished) {
      val watchdog = dynamicContext.watchdogTimeout
      if (watchdog < 0) {
        throw new PipelineException("invwatchdog", "The watchdog timer value must have a non-negative value")
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

  private def makeActors(): Unit = {
    _system = ActorSystem("jafpl-com-" + UniqueId.nextId)
    _monitor = _system.actorOf(Props(new GraphMonitor(graph, this)), name = "monitor")

    for (node <- graph.nodes) {
      var actorName = "_" * (7 - node.id.length) + node.id

      val actor = node match {
        case split: Splitter =>
          _system.actorOf(Props(new SplitterActor(_monitor, this, split)), actorName)
        case join: Joiner =>
          _system.actorOf(Props(new JoinerActor(_monitor, this, join)), actorName)
        case buf: Buffer =>
          _system.actorOf(Props(new BufferActor(_monitor, this, buf)), actorName)
        case forEach: ForEachStart =>
          _system.actorOf(Props(new ForEachActor(_monitor, this, forEach)), actorName)
        case viewport: ViewportStart =>
          _system.actorOf(Props(new ViewportActor(_monitor, this, viewport)), actorName)
        case choose: ChooseStart =>
          _system.actorOf(Props(new ChooseActor(_monitor, this, choose)), actorName)
        case when: WhenStart =>
          _system.actorOf(Props(new WhenActor(_monitor, this, when)), actorName)
        case trycatch: TryCatchStart =>
          _system.actorOf(Props(new TryCatchActor(_monitor, this, trycatch)), actorName)
        case trycatch: TryStart =>
          _system.actorOf(Props(new StartActor(_monitor, this, trycatch)), actorName)
        case trycatch: CatchStart =>
          _system.actorOf(Props(new CatchActor(_monitor, this, trycatch)), actorName)
        case pipe: PipelineStart =>
          _system.actorOf(Props(new PipelineActor(_monitor, this, pipe)), actorName)
        case group: GroupStart =>
          _system.actorOf(Props(new StartActor(_monitor, this, group)), actorName)
        case wstart: WhileStart =>
          _system.actorOf(Props(new WhileActor(_monitor, this, wstart)), actorName)
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
            case foreach: ForEachStart =>
              _system.actorOf(Props(new ForEachEndActor(_monitor, this, end)), actorName)
            case wstart: WhileStart =>
              _system.actorOf(Props(new WhileEndActor(_monitor, this, end)), actorName)
            case viewport: ViewportStart =>
              _system.actorOf(Props(new ViewportEndActor(_monitor, this, end)), actorName)
            case _ =>
              _system.actorOf(Props(new EndActor(_monitor, this, end)), actorName)
          }
        case req: GraphInput =>
          if (_graphInputs.contains(req.name)) {
            throw new PipelineException("dupname", "Input port name repeated: " + req.name)
          }
          val ip = new InputProxy(_monitor, this, node)
          _graphInputs.put(req.name, ip)
          _system.actorOf(Props(new InputActor(_monitor, this, node, ip)), actorName)
        case req: GraphOutput =>
          if (_graphOutputs.contains(req.name)) {
            throw new PipelineException("dupname", "Output port name repeated: " + req.name)
          }
          val op = new OutputProxy(_monitor, this, node)
          _graphOutputs.put(req.name, op)
          _system.actorOf(Props(new OutputActor(_monitor, this, node, op)), actorName)
        case req: Binding =>
          if (req.expression.isDefined) {
            _system.actorOf(Props(new VariableActor(_monitor, this, req)), actorName)
          } else {
            if (_graphBindings.contains(req.name)) {
              throw new PipelineException("dupname", "Input binding name repeated: " + req.name)
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
          throw new PipelineException("unexpected", "Unexpected step type: " + node)
      }

      _monitor ! GNode(node, actor)
    }
  }
}
