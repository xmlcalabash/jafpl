package com.jafpl.steps

trait DataProvider extends Consumer {
  /** Provide input on the specified port.
    *
    * Calling this method sends the specified `item` as an output on the specified `port`.
    *
    * @param item The item.
    */
  def send(item: Any): Unit

  /** Close the port.
    *
    * This will be called after the last output has been sent.
    *
    */
  def close(): Unit

}
