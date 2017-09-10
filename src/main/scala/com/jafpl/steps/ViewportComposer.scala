package com.jafpl.steps

import com.jafpl.messages.{ItemMessage, Message, Metadata}

/** Decompose and recompose data for viewport processing.
  *
  * Viewports operate on sub-portions of a resource (some of the children in an XML document,
  * some of the properties in a JSON object, etc.).
  *
  * The `decompose` method is called before the viewport processing begins. It extracts the sub-portions
  * that should be processed and returns them. Each returned item is mutable and will be updated by
  * the viewport processing.
  *
  * The method by which sub-portions are selected is irrelevant to the pipeline engine.
  * Often it will be convenient to pass an expression or other data to the implementation of
  * the composer to control the selection.
  *
  * Note: it's an implementation detail whether the items returned are pointers into the original
  * data structure or copies of them. If they are pointers into the original data structure, then
  * viewport steps may be able to navigate outside the confines of the sub-portion selected. For better
  * or worse.
  *
  * The `recompose` method is called after viewport processing is complete. The composer
  * reassembles the resource using the transformed portions and returns the resulting resource.
  *
  */
trait ViewportComposer {
  /** Decompose the specified item into sub-portions for viewport processing.
    *
    * @param message The message containing the item to be processed by the viewport.
    * @return A list of sub-portions to process in the viewport.
    */
  def decompose(message: Message): List[ViewportItem]

  /** Recompose the item.
    *
    * @return The recomposed item.
    */
  def recompose(): Message
}
