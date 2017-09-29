package com.jafpl.steps

/** A data provider.
  *
  * This trait is used to expose the input variable requirements of a pipeline.
  *
  */
trait BindingProvider {
  /** Provide the binding.
    *
    * Calling this method sets the value for the binding. This method must
    * only be called once.
    *
    * @param item The item.
    */
  def set(item: Any): Unit

  /** Provide nothing.
    *
    * Calling this method indicates that no binding will be set for this variable.
    * this method must only be called once and must only be called if set is not
    * going to be called.
    */
  def close(): Unit
}
