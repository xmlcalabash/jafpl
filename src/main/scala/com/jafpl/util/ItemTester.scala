package com.jafpl.util

trait ItemTester {
  def test(item: List[Any], bindings: Map[String,Any]): Boolean
}
