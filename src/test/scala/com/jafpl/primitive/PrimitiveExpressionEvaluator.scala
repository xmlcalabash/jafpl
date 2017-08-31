package com.jafpl.primitive

import com.jafpl.exceptions.PipelineException
import com.jafpl.runtime.{ExpressionEvaluator, RuntimeConfiguration}
import org.slf4j.{Logger, LoggerFactory}

class PrimitiveExpressionEvaluator(config: RuntimeConfiguration) extends ExpressionEvaluator() {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def value(expr: String, context: List[Any], bindings: Map[String,Any]): Any = {
    if (context.size > 1) {
      throw new PipelineException("badconext", "Context contains more than one item", None)
    }
    val patn = "(\\S+)\\s*([-+*/])\\s*(\\S+)".r
    val digits = "([0-9]+)".r
    expr match {
      case patn(left, op, right) =>
        val leftv = left match {
          case digits(num) => num.toLong
          case _ => bindings(left).toString.toLong
        }
        val rightv = right match {
          case digits(num) => num.toLong
          case _ => bindings(right).toString.toLong
        }
        val result = op match {
          case "-" => leftv - rightv
          case "+" => leftv + rightv
          case "*" => leftv * rightv
          case "/" => leftv / rightv
        }
        if (config.traceEnabled("ExprEval")) {
          logger.info(s"COMPUTED $expr = $result")
        }
        result
      case _ =>
        logger.warn("Expression did not match pattern: returning expression string as value: " + expr)
        expr
    }
  }

  override def booleanValue(expr: String, context: List[Any], bindings: Map[String,Any]): Boolean = {
    if (context.size > 1) {
      throw new PipelineException("badconext", "Context contains more than one item", None)
    }
    val patn = ". ([<=>]) ([0-9]+)".r
    expr match {
      case patn(cond, num) =>
        val value = num.toLong
        if (context.nonEmpty) {
          context.head match {
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
