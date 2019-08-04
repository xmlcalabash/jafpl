package com.jafpl.primitive

import com.jafpl.messages.Message
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.util.ItemTester

class PrimitiveItemTester(runtimeConfig: RuntimeConfiguration, expr: String) extends ItemTester {
  override def test(item: List[Message], bindings: Map[String, Message]): Boolean = {
    val eval = runtimeConfig.expressionEvaluator.newInstance()
    val pass = eval.booleanValue(expr, item, bindings, None)
    pass
  }
}
