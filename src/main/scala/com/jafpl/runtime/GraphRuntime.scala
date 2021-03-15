package com.jafpl.runtime

import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{Graph, GraphInput, GraphOutput, OptionBinding}
import com.jafpl.steps.DataProvider
import com.jafpl.util.{DefaultTraceEventManager, TraceEventManager}
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

  private var schedThread = Option.empty[Thread]

  private var _started = false
  private var _finished = false
  private val _graphInputs = mutable.HashMap.empty[String, InputProxy]
  private val _graphOutputs = mutable.HashMap.empty[String, OutputProxy]
  private val _graphOptions = mutable.HashMap.empty[String, OptionBinding]
  private var _traceEventManager: TraceEventManager = new DefaultTraceEventManager()
  private var _exception = Option.empty[Throwable]

  graph.close()

  if (!graph.valid) {
    throw JafplException.invalidGraph()
  }

  val scheduler = new Scheduler(this)

  for (node <- graph.nodes) {
    node match {
      case req: GraphInput =>
        if (_graphInputs.contains(req.name)) {
          throw JafplException.dupInputPort(req.name, req.location)
        }
        val ip = new InputProxy(node, req.name, scheduler)
        _graphInputs.put(req.name, ip)
      case req: GraphOutput =>
        if (_graphOutputs.contains(req.name)) {
          throw JafplException.dupOutputPort(req.name, req.location)
        }
        val op = new OutputProxy(req.name, node)
        _graphOutputs.put(req.name, op)
      case _ => ()
    }
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
  def outputs: Map[String, OutputProxy] = Map() ++ _graphOutputs

  protected[runtime] def finish(): Unit = {
    _finished = true
  }

  protected[runtime] def finish(cause: Throwable): Unit = {
    _exception = Some(cause)
    finish()
  }

  /** Runs the pipeline.
    *
    * This method will block until pipeline execution finishes.
    *
    */
  def runSync(): Unit = {
    for (proxy <- inputs.values) {
      proxy.asInstanceOf[InputProxy].close()
    }

    if (graph.valid) {
      run()
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
  def run(): Unit = {
    schedThread = Some(new Thread(scheduler))
    schedThread.get.start()
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
    schedThread.get.join()
    schedThread = None
    _finished = true
    if (scheduler.exception.isDefined) {
      throw scheduler.exception.get
    }
  }

  def stop(): Unit = {
    scheduler.stop()
    if (schedThread.isDefined) {
      schedThread.get.join()
      schedThread = None
    }
    _finished = true
  }
}
