package com.jafpl.steps

import com.jafpl.graph.Location
import com.jafpl.messages.{BindingMessage, Metadata}
import com.jafpl.runtime.RuntimeConfiguration

/** An atomic pipeline step.
  *
  * Steps must implement this interface. The step lifecycle is as follows:
  *
  * 1. The `setConsumer` method will be called. The step must hold onto the consumer object.
  * Calling `send` on this object is the step's one and only way to send output to the
  * next steps(s) in the pipeline.
  * 2. The `receive` method will be called for each input item that the step receives.
  * 3. The `run` method will be called after all inputs have been received.
  * 4. If the step is in a loop, the `reset` method will be called between iterations.
  *
  * Steps are not required to wait until their run method is called before beginning execution.
  * Note, however, that if a step has side effects (writing to disk, interacting with a web service,
  * etc.) it is prudent to do so. A step may receive several inputs and then never run if
  * some sibling step raised an exception that caused an entire container to fail.
  *
  * @groupdesc IOSpecs These methods define the input and output contracts for a step: required
  *           inputs and their cardinalities, outputs and their cardinalities, and required
  *           bindings.
  *
  */
trait Step extends DataConsumer with ManifoldSpecification {

  /** The location associated with this step.
    *
    * Many pipelines are constructed from an external, declarative description. In the event
    * that constructing or running the pipeline results in an error, authors will be greatly
    * relieved if the source of their error can be pinpointed exactly.
    */
  def location: Option[Location]

  /** Set the location associated with this step.
    *
    * Many pipelines are constructed from an external, declarative description. In the event
    * that constructing or running the pipeline results in an error, authors will be greatly
    * relieved if the source of their error can be pinpointed exactly.
    *
    * @param location The location associated with this step.
    */
  def location_=(location: Location): Unit

  /** The names of the variable bindings this step requires.
    *
    * This method returns the names of the variables for which
    * the step requires a binding.
    *
    * @group IOSpecs
    * @return The list of variable names.
    */
  def bindingSpec: BindingSpecification

  /** Set the consumer for sending output to the pipeline.
    *
    * @param consumer The consumer.
    */
  def setConsumer(consumer: DataConsumer): Unit

  /** Receive a binding.
    *
    * Receive a variable binding from the pipeline.
    *
    * @param message The binding message.
    */
  def receiveBinding(message: BindingMessage): Unit

  /** One time, startup initialization.
    *
    * The pipeline has been constructed and is going to run. This method is called once
    * before execution begins.
    *
    * @param config The runtime configuration used by this pipeline
    *
    */
  def initialize(config: RuntimeConfiguration): Unit

  /** Run!
    *
    * All inputs have been recieved, so your deal.
    *
    */
  def run(): Unit

  /** Reset to starting conditions.
    *
    * Inputs are about to begin arriving, you're going to be asked to run again,
    * get yourself ready!
    *
    */
  def reset(): Unit

  /** Abort processing.
    *
    * This method will be called if `run` will never be called because an exception
    * has terminated execution of the container that contains this step.
    *
    * Note that `reset` will also called before processing restarts if this step
    * is in a loop.
    *
    */
  def abort(): Unit

  /** Stop processing.
    *
    * This method will be called when all processing has finished and the pipeline
    * is about to end.
    */
  def stop(): Unit
}
