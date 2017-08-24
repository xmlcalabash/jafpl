package com.jafpl.steps

/** A data provider.
  *
  * This trait is used to expose the input requirements of a pipeline.
  *
  */
trait DataProvider extends Provider {
  /** Provide an input item.
    *
    * Calling this method sends the specified `item` as an input.
    *
    * @param item The item.
    */
  def send(item: Any): Unit

  /** Close the provider.
    *
    * This will be called after the last `input` has been sent.
    *
    */
  def close(): Unit
}
