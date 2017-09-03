package com.jafpl.steps

import com.jafpl.messages.Metadata

/** Interface for sending outputs from atomic steps.
  *
  * Atomic step implementations are independent of the graph execution infrastructure.
  *
  * Steps call the `receive` method on this object to deliver output.
  *
  */
trait DataConsumer {
  /** Send output from a step.
    *
    * Calling this method sends the specified `item` as an output on the specified `port`.
    *
    * @param port The output port.
    * @param item The item.
    * @param metadata Item metadata.
    */
  def receive(port: String, item: Any, metadata: Metadata): Unit
}
