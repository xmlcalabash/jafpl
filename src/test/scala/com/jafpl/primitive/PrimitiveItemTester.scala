package com.jafpl.primitive

import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.util.ItemTester

class PrimitiveItemTester(runtimeConfig: RuntimeConfiguration, expr: String) extends ItemTester {
  override def test(item: Option[Any], bindings: Option[Map[String, Any]]): Boolean = {
    runtimeConfig.expressionEvaluator().booleanValue(expr, item, bindings)
  }
}
