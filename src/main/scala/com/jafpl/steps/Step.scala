package com.jafpl.steps

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
trait Step {
  /** The names of the input ports this step requires.
    *
    * This method returns the names of the input ports that the step requires.
    *
    * @group IOSpecs
    * @return The list of required port names.
    */
  def inputSpec: PortSpecification

  /** The names of the output ports this step requires.
    *
    * This method returns the names of the output ports that the step requires.
    *
    * @group IOSpecs
    * @return The list of required port names.
    */
  def outputSpec: PortSpecification

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
  def setConsumer(consumer: StepDataProvider)

  /** Receive a binding.
    *
    * Receive a variable binding from the pipeline.
    *
    * @param variable The name of the variable.
    * @param value The computed value of the variable.
    */
  def receiveBinding(variable: String, value: Any)

  /** Receive a document.
    *
    * @param port The input port name.
    * @param item The item.
    */
  def receive(port: String, item: Any)

  /** One time, startup initialization.
    *
    * The pipeline has been constructed and is going to run. This method is called once
    * before execution begins.
    *
    */
  def initialize()

  /** Run!
    *
    * All inputs have been recieved, so your deal.
    *
    */
  def run()

  /** Reset to starting conditions.
    *
    * Inputs are about to begin arriving, you're going to be asked to run again,
    * get yourself ready!
    *
    */
  def reset()

  /** Abort processing.
    *
    * This method will be called if `run` will never be called because an exception
    * has terminated execution of the container that contains this step.
    *
    * Note that `reset` will also called before processing restarts if this step
    * is in a loop.
    *
    */
  def abort()

  /** Stop processing.
    *
    * This method will be called when all processing has finished and the pipeline
    * is about to end.
    */
  def stop()
}
