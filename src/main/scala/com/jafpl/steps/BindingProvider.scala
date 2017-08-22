package com.jafpl.steps

/** A data provider.
  *
  * This trait is used to expose the input requirements of a pipeline.
  *
  */
trait BindingProvider extends Provider {
  /** Provide the binding.
    *
    * Calling this method sets the value for the binding. This method must
    * only be called once.
    *
    * @param item The item.
    */
  def set(item: Any): Unit
}
