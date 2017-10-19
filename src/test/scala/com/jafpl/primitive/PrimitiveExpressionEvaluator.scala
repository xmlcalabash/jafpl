package com.jafpl.primitive

import com.jafpl.exceptions.PipelineException
import com.jafpl.messages.{ItemMessage, Message, Metadata}
import com.jafpl.runtime.{ExpressionEvaluator, RuntimeConfiguration}
import org.slf4j.{Logger, LoggerFactory}

class PrimitiveExpressionEvaluator(config: RuntimeConfiguration) extends ExpressionEvaluator() {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def newInstance(): PrimitiveExpressionEvaluator = {
    new PrimitiveExpressionEvaluator(config)
  }

  override def value(expr: Any, context: List[Message], bindings: Map[String,Message], options: Option[Any]): Message = {
    singletonValue(expr, context, bindings, options)
  }

  override def singletonValue(expr: Any, context: List[Message], bindings: Map[String,Message], options: Option[Any]): Message = {
    if (context.size > 1) {
      throw new PipelineException("badconext", "Context contains more than one item", None)
    }

    val strexpr = expr match {
      case str: String => str
      case _ => throw new PipelineException("unexpected", s"Unexpected object as expression value: $expr", None)
    }

    val addPatn = "(\\S+)\\s*([-+*/])\\s*(\\S+)".r
    val numPatn = "(\\S+)".r
    val digits = "([0-9]+)".r
    strexpr match {
      case addPatn(left, op, right) =>
        val leftv = left match {
          case digits(num) => num.toLong
          case _ =>
            if (bindings.contains(left)) {
              numberFromBinding(bindings(left))
            } else {
              throw new PipelineException("nobinding", "No binding for " + left)
            }
        }
        val rightv = right match {
          case digits(num) => num.toLong
          case _ =>
            if (bindings.contains(right)) {
              numberFromBinding(bindings(right))
            } else {
              throw new PipelineException("nobinding", "No binding for " + right)
            }
        }
        val result = op match {
          case "-" => leftv - rightv
          case "+" => leftv + rightv
          case "*" => leftv * rightv
          case "/" => leftv / rightv
        }
        if (config.traceEnabled("ExprEval")) {
          logger.info(s"COMPUTED $strexpr = $result")
        }
        new ItemMessage(result, Metadata.NUMBER)
      case numPatn(num) =>
        num match {
          case digits(dnum) =>
            new ItemMessage(dnum.toLong, Metadata.NUMBER)
          case _ =>
            logger.warn("Expression did not match pattern: returning expression string as value: " + strexpr)
            new ItemMessage(strexpr, Metadata.STRING)
        }
      case _ =>
        logger.warn("Expression did not match pattern: returning expression string as value: " + strexpr)
        new ItemMessage(strexpr, Metadata.STRING)
    }
  }

  private def numberFromBinding(message: Message): Long = {
    message match {
      case item: ItemMessage =>
        item.item.toString.toLong
      case _ =>
        throw new IllegalArgumentException("Binding message is not a number")
    }
  }

  override def booleanValue(expr: Any, context: List[Message], bindings: Map[String,Message], options: Option[Any]): Boolean = {
    if (context.size > 1) {
      throw new PipelineException("badconext", "Context contains more than one item", None)
    }

    val strexpr = expr match {
      case str: String => str
      case _ => throw new PipelineException("unexpected", s"Unexpected object as expression value: $expr", None)
    }

    val patn = ". ([<=>]) ([0-9]+)".r
    strexpr match {
      case patn(cond, num) =>
        val value = num.toLong
        if (context.nonEmpty) {
          context.head match {
            case item: ItemMessage =>
              item.item match {
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
            case _ => false
          }
        } else {
          false
        }
      case _ =>
        ! ( (strexpr == "") || (strexpr == "false") || (strexpr == "0") )
    }
  }
}
