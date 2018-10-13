package com.jafpl.exceptions

import com.jafpl.graph.Location
import com.jafpl.steps.PortCardinality

object JafplException {
  val CHILD_FORBIDDEN = 1
  val BAD_CONTAINER_END = 2
  val DUP_CONTAINER_START = 3
  val DIFF_GRAPHS = 4
  val DUP_PRIORITY_EDGE = 5
  val VARNAME_NOT_IN_SCOPE = 6
  val INPUT_PORT_MISSING = 7
  val VARIABLE_BINDING_MISSING = 8
  val BAD_LOOP_INPUT_PORT = 9
  val INVALID_INPUTS = 10
  val INVALID_OUTPUTS = 11
  val UNEXPECTED_CARDINALITY = 12
  val DUP_INPUT_PORT = 13
  val DUP_OUTPUT_PORT = 14
  val DUP_OPTION_NAME = 15
  val READ_INSIDE_CONTAINER = 16
  val LOOP_IN_GRAPH = 17
  val GRAPH_CLOSED = 18
  val UNTIL_LOOP_TEST_REQUIRED = 19
  val INVALID_INPUT_PORT = 20
  val WHILE_LOOP_TEST_REQUIRED = 21
  val START_REDEF = 22
  val INVALID_GRAPH = 23
  val ABSTRACT_CONTAINER = 24
  val INVALID_CARDINALITY = 25
  val WILDCARD_CARDINALITY = 26
  val EMPTY_SOURCE_INPUT = 27
  val UNEXPECTED_MESSAGE = 28
  val NO_INPUT_ON_LOOP = 29
  val UNCONFIGURED = 30
  val CONFIG_CLOSED = 31
  val SINGLETON_CONTEXT_EXPECTED = 32
  val UNEXPECTED_EXPRESSION_OBJECT = 33
  val NO_BINDING = 34
  val UNEXPECTED_VALUE_OBJECT = 35
  val WATCHDOG_TIMEOUT = 36
  val UNKNOWN_OPTION = 37
  val NOT_RUNNING = 38
  val INVALID_CONFIG_VALUE = 39
  val UNEXPECTED_STEP_TYPE = 40
  val INTERNAL_ERROR = 41
  private val THE_ANSWER = 42
  val UNEXPECTED_SEQUENCE = 43
  val CARDINALITY_ERROR = 44
  private val RACIST_MISOGYNIST_THUG = 45
  val BAD_PORT = 46
  val UNEXPECTED_ITEM_TYPE = 47

  protected[jafpl] def childForbidden(parent: String, child: String, location: Option[Location]): JafplException
  = new JafplException(CHILD_FORBIDDEN, location, List(parent, child), "Cannot add $1 to $2")
  protected[jafpl] def badContainerEnd(expectedEnd: String, actualEnd: String, location: Option[Location]): JafplException
  = new JafplException(BAD_CONTAINER_END, location, List(expectedEnd, actualEnd), "Unexpected end: end of $1 is $2")
  protected[jafpl] def dupContainerStart(start: String, dupStart: String, location: Option[Location]): JafplException
  = new JafplException(DUP_CONTAINER_START, location, List(start, dupStart), "Unexpected start: start of $1 is already defined: $2")
  protected[jafpl] def differentGraphs(from: String, to: String, location: Option[Location]): JafplException
  = new JafplException(DIFF_GRAPHS, location, List(from,to), "Cannot add edge, $1 and $2 are in different graphs")
  protected[jafpl] def dupPriorityEdge(from: String, to: String, location: Option[Location]): JafplException
  = new JafplException(DUP_PRIORITY_EDGE, location, List(from,to), "Illegal priority edge $1 to $2: only the first edge can be a priority edge")
  protected[jafpl] def variableNotInScope(varname: String, to: String, location: Option[Location]): JafplException
  = new JafplException(VARNAME_NOT_IN_SCOPE, location, List(varname, to), "No in-scope binding for $1 from $2")
  protected[jafpl] def requiredInputMissing(port: String, step: String, location: Option[Location]): JafplException
  = new JafplException(INPUT_PORT_MISSING, location, List(port, step), "Required input port $1 missing: $2")
  protected[jafpl] def requiredVariableBindingMissing(varname: String, step: String, location: Option[Location]): JafplException
  = new JafplException(VARIABLE_BINDING_MISSING, location, List(varname, step), "Required variable binding $1 missing: $2")
  protected[jafpl] def badLoopInputPort(port: String, step: String, location: Option[Location]): JafplException
  = new JafplException(BAD_LOOP_INPUT_PORT, location, List(port, step), "Input port $1 not allowed on loop $2")
  protected[jafpl] def invalidInputs(step: String, location: Option[Location]): JafplException
  = new JafplException(INVALID_INPUTS, location, List(step), "Invalid input ports on $1")
  protected[jafpl] def invalidOutputs(step: String, location: Option[Location]): JafplException
  = new JafplException(INVALID_OUTPUTS, location, List(step), "Invalid output ports on $1")
  protected[jafpl] def unexpectedCardinality(step: String, port: String, cardinality: String, location: Option[Location]): JafplException
  = new JafplException(UNEXPECTED_CARDINALITY, location, List(step,port,cardinality), "Unexpected cardinality on $1/$2: $3")
  protected[jafpl] def dupInputPort(port: String, location: Option[Location]): JafplException
  = new JafplException(DUP_INPUT_PORT, location, List(port), "Repeated input port name: $1")
  protected[jafpl] def dupOutputPort(port: String, location: Option[Location]): JafplException
  = new JafplException(DUP_OUTPUT_PORT, location, List(port), "Repeated output port name: $1")
  protected[jafpl] def dupOptionName(option: String, location: Option[Location]): JafplException
  = new JafplException(DUP_OPTION_NAME, location, List(option), "Repeated option name: $1")
  protected[jafpl] def readInsideContainer(from: String, to: String, fromDepth: String, toDepth: String, location: Option[Location]): JafplException
  = new JafplException(READ_INSIDE_CONTAINER, location, List(from, to, fromDepth, toDepth), "Port is not in scope (inside container) attempting to read $2 from 1")
  protected[jafpl] def loopDetected(loop: String, location: Option[Location]): JafplException
  = new JafplException(LOOP_IN_GRAPH, location, List(loop), "Loop detected in graph")
  protected[jafpl] def graphClosed(location: Option[Location]): JafplException
  = new JafplException(GRAPH_CLOSED, location, List(), "Changes cannot be made to a graph after it is closed")
  protected[jafpl] def untilLoopTestRequired(location: Option[Location]): JafplException
  = new JafplException(UNTIL_LOOP_TEST_REQUIRED, location, List(), "An 'until' loop must have a test")
  protected[jafpl] def invalidInputPort(port: String, step: String, location: Option[Location]): JafplException
  = new JafplException(INVALID_INPUT_PORT, location, List(port), "Invalid input port $1 on $2")
  protected[jafpl] def whileLoopTestRequired(location: Option[Location]): JafplException
  = new JafplException(WHILE_LOOP_TEST_REQUIRED, location, List(), "A 'while' loop must have a test")
  protected[jafpl] def startRedefined(step: String, location: Option[Location]): JafplException
  = new JafplException(START_REDEF, location, List(step), "Attempt to redefine parent of $1")
  protected[jafpl] def invalidGraph(): JafplException
  = new JafplException(INVALID_GRAPH, None, List(), "Cannot create runtime for invalid graph")
  protected[jafpl] def abstractContainer(actor: String, location: Option[Location]): JafplException
  = new JafplException(ABSTRACT_CONTAINER, location, List(), "Attempt to instantiate abstract container: $1")
  protected[jafpl] def invalidCardinality(port: String, cardinality: String, location: Option[Location]): JafplException
  = new JafplException(INVALID_CARDINALITY, location, List(port,cardinality), "Invalid cardinality on port $1: $2")
  protected[jafpl] def invalidWildcardCardinality(): JafplException
  = new JafplException(WILDCARD_CARDINALITY, None, List(), "Only cardinality '*' is allowed on wildcard ports")
  protected[jafpl] def inputOnEmptySource(from: String, fromPort: String, toPort: String, item: String, location: Option[Location]): JafplException
  = new JafplException(EMPTY_SOURCE_INPUT, location, List(from, fromPort, toPort, item), "EmptySource received input $4 on port $3 from $1/$2")
  protected[jafpl] def unexpectedMessage(item: String, port: String, location: Option[Location]): JafplException
  = new JafplException(UNEXPECTED_MESSAGE, location, List(item, port), "Unexpected message on $2: $1")
  protected[jafpl] def noInputOnLoop(port: String, location: Option[Location]): JafplException
  = new JafplException(NO_INPUT_ON_LOOP, location, List(port), "Unexpected input on loop port: $1")
  protected[jafpl] def unconfigured(thing: String): JafplException
  = new JafplException(UNCONFIGURED, None, List(thing), "Attempt to use $1 but it is not configured")
  protected[jafpl] def configurationClosed(): JafplException
  = new JafplException(CONFIG_CLOSED, None, List(), "Changes cannot be made to a Jafpl configuration after it is closed")
  protected[jafpl] def singletonContextExpected(): JafplException
  = new JafplException(SINGLETON_CONTEXT_EXPECTED, None, List(), "Expression context must contain a single item")
  protected[jafpl] def unexpectedExpressionObject(expr: String): JafplException
  = new JafplException(UNEXPECTED_EXPRESSION_OBJECT, None, List(expr), "Unexpected expression object: $1")
  protected[jafpl] def noBindingFor(thing: String): JafplException
  = new JafplException(NO_BINDING, None, List(thing), "No binding for $1")
  protected[jafpl] def unexpectedValueObject(value: String): JafplException
  = new JafplException(UNEXPECTED_VALUE_OBJECT, None, List(value), "Unexpected value object: $1")
  protected[jafpl] def watchdogTimeout(): JafplException
  = new JafplException(WATCHDOG_TIMEOUT, None, List(), "Pipeline appears stuck: watchdog timer expired")
  protected[jafpl] def setUnknownOption(optname: String): JafplException
  = new JafplException(UNKNOWN_OPTION, None, List(optname), "Attempt to set value for option that doesn't exist: $1")
  protected[jafpl] def notRunning(): JafplException
  = new JafplException(NOT_RUNNING, None, List(), "Pipeline is not running")
  protected[jafpl] def invalidConfigurationValue(thing: String, value: String): JafplException
  = new JafplException(INVALID_CONFIG_VALUE, None, List(thing, value), "Invalid configuration: $1 cannot have the value $2")
  protected[jafpl] def unexpecteStepType(node: String, location: Option[Location]): JafplException
  = new JafplException(UNEXPECTED_STEP_TYPE, location, List(node), "Unexpected step type $1")
  protected[jafpl] def internalError(msg: String, location: Option[Location]): JafplException
  = new JafplException(INTERNAL_ERROR, location, List(msg), "$1")
  protected[jafpl] def unexpectedSequence(step: String, port: String, location: Option[Location]): JafplException
  = new JafplException(UNEXPECTED_SEQUENCE, location, List(step, port), "Unexpected sequence on $2 port of $1")
  protected[jafpl] def cardinalityError(port: String, count: String, spec: PortCardinality): JafplException
  = new JafplException(CARDINALITY_ERROR, None, List(port, count, spec), "Cardinality error: $2 documents sent to $1 (spec: $3)")
  protected[jafpl] def badPort(port: String): JafplException
  = new JafplException(BAD_PORT, None, List(port), "Port named $1 is not allowed")
  protected[jafpl] def unexpectedItemType(item: String, port: String, location: Option[Location]): JafplException
  = new JafplException(UNEXPECTED_ITEM_TYPE, location, List(item, port), "Unexpected item $1 in message on port $2")


  /*
  protected[jafpl] def ( location: Option[Location]): JafplException
  = new JafplException(, location, List(), "")

  */
}

class JafplException private (val code: Int, val location: Option[Location], val details: List[Any], val template: String) extends RuntimeException with JafplExceptionCode {
  override def jafplExceptionCode: Any = code

  override def getMessage: String = toString

  override def toString: String = {
    var message = s"JafplException $code: "
    var parse = template
    val detail = "^(.*?)\\$(\\d+)(.*)$".r

    var matched = true
    while (matched) {
      matched = false
      parse match {
        case detail(pre, detno, post) =>
          matched = true
          message += pre
          val detnum = detno.toInt - 1
          if (details.length > detnum) {
            message += stringify(details(detnum))
          }
          parse = post
        case _ =>
      }
    }

    message + parse
  }

  private def stringify(any: Any): String = {
    any match {
      case list: List[Any] =>
        var str = "["
        var sep = ""
        for (item <- list) {
          str = str + sep + item.toString
          sep = ", "
        }
        str = str + "]"
        str
      case _ => any.toString
    }
  }

}
