package com.jafpl.runtime

import akka.actor.{ActorRef, ActorSystem, Props}
import com.jafpl.exceptions.{GraphException, PipelineException}
import com.jafpl.graph.{Binding, Buffer, CatchStart, ChooseStart, ContainerEnd, ContainerStart, ForEachStart, Graph, GroupStart, Joiner, PipelineStart, Splitter, TryCatchStart, TryStart, ViewportStart, WhenStart}
import com.jafpl.runtime.GraphMonitor.{GNode, GRun, GWatchdog}
import com.jafpl.util.UniqueId
import org.slf4j.LoggerFactory

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

  graph.close()

  if (!graph.valid) {
    throw new GraphException("Cannot run an invalid graph")
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
        case binding: Binding =>
          _system.actorOf(Props(new BindingActor(_monitor, this, binding)), actorName)
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
        case start: ContainerStart =>
          throw new GraphException("Attempt to instantiate naked container: " + start)
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
            case viewport: ViewportStart =>
              _system.actorOf(Props(new ViewportEndActor(_monitor, this, end)), actorName)
            case _ =>
              _system.actorOf(Props(new EndActor(_monitor, this, end)), actorName)
          }
        case _ =>
          _system.actorOf(Props(new NodeActor(_monitor, this, node)), actorName)
      }

      if (node.step.isDefined) {
        node.step.get.setConsumer(new ConsumingProxy(_monitor, this, node))
      }

      _monitor ! GNode(node, actor)
    }
  }
}
