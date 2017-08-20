package com.jafpl.primitive

import com.jafpl.runtime.ExpressionEvaluator

class PrimitiveExpressionEvaluator() extends ExpressionEvaluator() {
  override def value(expr: String, context: Option[Any], bindings: Option[Map[String,Any]]): Any = {
    expr
  }

  override def booleanValue(expr: String, context: Option[Any], bindings: Option[Map[String,Any]]): Boolean = {
    ! ( (expr == "") || (expr == "false") || (expr == "0") )
  }
}
