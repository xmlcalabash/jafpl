package com.jafpl.util

import com.jafpl.messages.Message

trait ItemTester {
  def test(item: List[Message], bindings: Map[String,Message]): Boolean
}
