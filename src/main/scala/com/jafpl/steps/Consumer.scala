package com.jafpl.steps

/** Interface for sending outputs from atomic steps.
  *
  * Atomic step implementations are independent of the graph execution infrastructure.
  *
  * Steps call the `send` method on this object to deliver output.
  *
  */
trait Consumer {
  /** Send output from a step.
    *
    * Calling this method sends the specified `item` as an output on the specified `port`.
    *
    * @param port The output port.
    * @param item The item.
    */
  def send(port: String, item: Any): Unit
}
