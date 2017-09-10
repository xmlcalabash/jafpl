package com.jafpl.steps

import com.jafpl.messages.{Message, Metadata}

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
    * Calling this method sends the specified `message` on the specified `port`.
    *
    * @param port The output port.
    * @param message The message.
    */
  def receive(port: String, message: Message): Unit
}
