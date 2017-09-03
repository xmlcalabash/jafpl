package com.jafpl.steps

import com.jafpl.messages.Metadata

/** Interface for sending external data to a pipeline.
  *
  * Call the `send` method on this object to deliver data.
  *
  * (This really exists just to simplify the interface to the pipeline. There's a simple
  * proxy that sends provided data to an appropriate consumer.)
  */
trait DataProvider {
  /** Send data to a pipeline.
    *
    * Calling this method sends the specified `item` as an output on the specified `port`.
    *
    * @param item The item.
    * @param metadata Item metadata.
    */
  def send(item: Any, metadata: Metadata): Unit
}
