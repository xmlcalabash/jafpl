package com.jafpl.util

trait ItemTester {
  def test(item: Option[Any], bindings: Option[Map[String,Any]]): Boolean
}
