package com.jafpl.runtime

import com.jafpl.graph.Node
import com.jafpl.items.GenericItem

/**
  * Created by ndw on 10/3/16.
  */
trait StepController {
  def send(port: String, item: GenericItem)
  def close(port: String)
  def tell(node: Node, msg: Any)
  def stop()
}
