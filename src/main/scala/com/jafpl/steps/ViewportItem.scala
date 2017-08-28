package com.jafpl.steps

import com.jafpl.messages.Metadata

/** A representation of a sub-portion of a resource for viewport processing.
  *
  * Each viewport item will be used by the viewport step to process a sub-portion of a
  * larger resource.
  *
  */
trait ViewportItem {
  /** The item to process.
    *
    * This item should be a sub-resource of the original resource passed to the
    * [[com.jafpl.steps.ViewportComposer]].
    *
    * @return The item.
    */
  def getItem: Any

  /** Metadata item to process.
    *
    * This should be metadata about the corresponding sub-resource of the original resource passed to the
    * [[com.jafpl.steps.ViewportComposer]].
    *
    * @return The metadata.
    */
  def getMetadata: Metadata

  /** The transformed item(s).
    *
    * After processing is complete, the transformed item is returned by calling
    * this method.
    *
    * @param items The item(s) that the original item was transformed into.
    */
  def putItems(items: List[Any]): Unit
}
