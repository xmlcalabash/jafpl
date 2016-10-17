package com.jafpl.test

import com.jafpl.calc.{AddExpr, Doubler, IterateIntegers, MultExpr, NumberLiteral, UnaryExpr}
import com.jafpl.graph.{Graph, InputNode, LoopStart, Node, OutputNode, Runtime}
import com.jafpl.items.NumberItem
import com.jafpl.xpath.{CalcParser, XdmNodes}
import net.sf.saxon.s9api.{Processor, QName, XdmNode, XdmNodeKind}
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

@RunWith(classOf[JUnitRunner])
class CalcSpec extends FlatSpec {
  val _MultiplicativeExpr = new QName("", "MultiplicativeExpr")
  val _UnaryExpr = new QName("", "UnaryExpr")
  val _AdditiveExpr = new QName("", "AdditiveExpr")
  val _IntegerLiteral = new QName("", "IntegerLiteral")
  val _TOKEN = new QName("", "TOKEN")
  val _VarRef = new QName("", "VarRef")
  val _QName = new QName("", "QName")
  val _FunctionCall = new QName("", "FunctionCall")
  val _ArgumentList = new QName("", "ArgumentList")
  val _nid = new QName("", "nid")

  val multOps = Array("*", "div")
  val addOps = Array("+", "-")

  val processor = new Processor(false)

  val nodeMap = mutable.HashMap.empty[String, Node]
  val varMap = mutable.HashMap.empty[String, InputNode]
  var firstElement = true
  var output: OutputNode = null

  "A Calc with 1+2" should "return 3" in {
    val output = runGraph("1+2")

    var count = 1
    var item = output.read()
    while (item.isDefined) {
      assert(count === 1)
      item.get match {
        case n: NumberItem =>
          assert(n.get === 3)
        case _ =>
          assert(false)
      }
      count += 1
      item = output.read()
    }
  }

  "A Calc with ((1+2)*(3+4+5) div 9) * $foo" should "return 8 when $foo=2" in {
    val bind = mutable.HashMap("foo" -> 2)
    val output = runGraph("(((1+2)*(3+4+5)) div 9) * $foo", bind.toMap)

    var count = 1
    var item = output.read()
    while (item.isDefined) {
      assert(count === 1)
      item.get match {
        case n: NumberItem =>
          assert(n.get === 8)
        case _ =>
          assert(false)
      }
      count += 1
      item = output.read()
    }
  }

  "A Calc with double(1,2,3)" should "return 2,4,6" in {
    val output = runGraph("double(1,2,3)")

    var count = 1
    var item = output.read()
    while (item.isDefined) {
      assert(count < 4)
      item.get match {
        case n: NumberItem =>
          assert(n.get === count * 2)
        case _ =>
          assert(false)
      }
      count += 1
      item = output.read()
    }
  }

  def runGraph(expr: String): OutputNode = {
    runGraph(expr, mutable.HashMap.empty[String, Int].toMap)
  }

  def runGraph(expr: String, bindings: Map[String,Int]): OutputNode = {
    val calc = new CalcParser(processor, expr)
    if (calc.parseTree.isEmpty) {
      throw new IllegalArgumentException("Lexical error in expression: " + expr)
    }

    val graph = new Graph()
    firstElement = true
    output = graph.createOutputNode("OUTPUT")
    varMap.clear()
    nodeMap.clear()

    makeNodes(graph, calc.simplifiedTree.get)
    makeEdges(graph, calc.simplifiedTree.get)

    val valid = graph.valid()
    if (!valid) {
      throw new IllegalStateException("Graph isn't valid?")
    }

    val runtime = new Runtime(graph)
    runtime.start()

    for (varname <- varMap.keySet) {
      val node = varMap(varname)
      if (bindings.contains(varname)) {
        node.write(new NumberItem(bindings(varname)))
        node.close()
      } else {
        throw new IllegalArgumentException("Expression uses unbound variable: " + varname)
      }
    }

    Thread.sleep(500) // Give the pipeline a chance to finish
    while (runtime.running) {
      graph.status()
      Thread.sleep(100)
    }

    output
  }

  def makeNodes(graph: Graph, node: XdmNode): Unit = {
    val children = XdmNodes.children(node)

    node.getNodeKind match {
      case XdmNodeKind.DOCUMENT =>
        for (child <- children) { makeNodes(graph, child) }
      case XdmNodeKind.ELEMENT =>
        val nodeId = name(node)
        var gnode: Option[Node] = None

        if (node.getNodeName == _MultiplicativeExpr) {
          val ops = ListBuffer.empty[String]
          ops += chkTok(children(1), multOps)
          var pos = 3
          while (pos < children.size) {
            ops += chkTok(children(pos), multOps)
            pos += 2
          }
          gnode = Some(graph.createNode(new MultExpr(ops.toList)))
        } else if (node.getNodeName == _AdditiveExpr) {
          val ops = ListBuffer.empty[String]
          ops += chkTok(children(1), addOps)
          var pos = 3
          while (pos < children.size) {
            ops += chkTok(children(pos), addOps)
            pos += 2
          }
          gnode = Some(graph.createNode(new AddExpr(ops.toList)))
        } else if (node.getNodeName == _UnaryExpr) {
          val op = chkTok(children.head, addOps)
          gnode = Some(graph.createNode(new UnaryExpr(op)))
        } else if (node.getNodeName == _IntegerLiteral) {
          gnode = Some(graph.createNode(new NumberLiteral(node.getStringValue.toInt)))
        } else if (node.getNodeName == _VarRef) {
          if (!varMap.contains(nodeId)) {
            gnode = Some(graph.createInputNode(nodeId))
            varMap.put(node.getStringValue, gnode.get.asInstanceOf[InputNode])
          }
        } else if (node.getNodeName == _FunctionCall) {
          // We only know about the double function...
          if (children.head.getNodeName != _QName || children.head.getStringValue != "double") {
            throw new IllegalArgumentException("Only expecting the 'double' function")
          }

          val fname = name(children.head)
          val doubler = graph.createNode(new Doubler())
          nodeMap.put(fname, doubler)

          val iterate = new IterateIntegers()
          gnode = Some(graph.createIteratorNode(iterate, List(doubler)))

          graph.addEdge(gnode.get, "current", doubler, "source")
          graph.addEdge(doubler, "result", gnode.get.asInstanceOf[LoopStart].compoundEnd, "I_result")

        } else if (node.getNodeName == _TOKEN || node.getNodeName == _QName || node.getNodeName == _ArgumentList) {
          // nop
        } else {
          throw new IllegalArgumentException("Unexpected node name: " + node.getNodeName)
        }

        if (gnode.isDefined) {
          if (firstElement) {
            graph.addEdge(linkFrom(gnode.get), "result", output, "source")
            firstElement = false
          }
          nodeMap.put(nodeId, gnode.get)
        }

        for (child <- children) {
          makeNodes(graph, child)
        }
      case _ => Unit
    }
  }

  def makeEdges(graph: Graph, node: XdmNode): Unit = {
    val children = XdmNodes.children(node)

    node.getNodeKind match {
      case XdmNodeKind.DOCUMENT =>
        for (child <- children) { makeEdges(graph, child) }
      case XdmNodeKind.ELEMENT =>
        val nodeId = name(node)
        var gnode = nodeMap.get(nodeId)

        if (node.getNodeName == _MultiplicativeExpr
          || node.getNodeName == _AdditiveExpr
        ) {
          var count = 1
          var pos = 0
          while (pos < children.size) {
            val operand = nodeMap(name(children(pos)))
            graph.addEdge(operand, "result", gnode.get, "s" + count)
            count += 1
            pos += 2
          }
        } else if (node.getNodeName == _FunctionCall) {
          var count = 1
          var pos = 0
          val arglist = XdmNodes.children(children(1))
          while (pos < arglist.size) {
            val operand = nodeMap(name(arglist(pos)))
            graph.addEdge(operand, "result", gnode.get, "s" + count)
            count += 1
            pos += 2
          }
        } else if (node.getNodeName == _UnaryExpr) {
          val expr = nodeMap(name(children(1)))
          graph.addEdge(expr, "result", gnode.get, "source")
        }
        for (child <- children) { makeEdges(graph, child) }
      case _ => Unit
    }
  }

  def linkFrom(node: Node): Node = {
    node match {
      case l: LoopStart => l.compoundEnd
      case _ => node
    }
  }

  def chkTok(child: XdmNode, tokens: Array[String]): String = {
    if (child.getNodeName == _TOKEN && tokens.contains(child.getStringValue)) {
      child.getStringValue
    } else {
      throw new IllegalArgumentException("Unexpected token: " + child.toString)
      ""
    }
  }

  def name(node: XdmNode): String = {
    node.getNodeName + "_" + node.getAttributeValue(_nid)
  }
}