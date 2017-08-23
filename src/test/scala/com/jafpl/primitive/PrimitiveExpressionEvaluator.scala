package com.jafpl.primitive

import com.jafpl.runtime.ExpressionEvaluator

class PrimitiveExpressionEvaluator() extends ExpressionEvaluator() {
  override def value(expr: String, context: Option[Any], bindings: Option[Map[String,Any]]): Any = {
    expr
  }

  override def booleanValue(expr: String, context: Option[Any], bindings: Option[Map[String,Any]]): Boolean = {
    val patn = ". ([<=>]) ([0-9]+)".r
    expr match {
      case patn(cond, num) =>
        val value = num.toLong
        if (context.isDefined) {
          context.get match {
            case cnum: Long =>
              cond match {
                case "<" => cnum < value
                case ">" => cnum > value
                case _ => cnum == value
              }
            case cnum: Int =>
              cond match {
                case "<" => cnum < value
                case ">" => cnum > value
                case _ => cnum == value
              }
            case _ => false
          }
        } else {
          false
        }
      case _ =>
        ! ( (expr == "") || (expr == "false") || (expr == "0") )
    }
  }
}
