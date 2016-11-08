package com.jafpl.calc

import com.jafpl.graph.{Graph, InputNode, LoopStart, Node, OutputNode}
import com.jafpl.items.NumberItem
import com.jafpl.steps.GenerateLiteral
import com.jafpl.xpath.{CalcParser, XdmNodes}
import net.sf.saxon.s9api.{Processor, QName, XdmNode, XdmNodeKind}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Created by ndw on 10/17/16.
  */
class Calculator(expr: String, bindings: Map[String, Int]) {
  private val processor = new Processor(false)
  private val _MultiplicativeExpr = new QName("", "MultiplicativeExpr")
  private val _UnaryExpr = new QName("", "UnaryExpr")
  private val _AdditiveExpr = new QName("", "AdditiveExpr")
  private val _IntegerLiteral = new QName("", "IntegerLiteral")
  private val _TOKEN = new QName("", "TOKEN")
  private val _VarRef = new QName("", "VarRef")
  private val _QName = new QName("", "QName")
  private val _FunctionCall = new QName("", "FunctionCall")
  private val _ArgumentList = new QName("", "ArgumentList")
  private val _nid = new QName("", "nid")

  private val multOps = Array("*", "div")
  private val addOps = Array("+", "-")

  private val nodeMap = mutable.HashMap.empty[String, Node]
  private val varMap = mutable.HashMap.empty[String, InputNode]
  private var firstElement = true

  val graph = new Graph()
  private val output = graph.createOutputNode("result")

  def parse(): CalcParser = {
    val calc = new CalcParser(processor, expr)
    if (calc.parseTree.isEmpty) {
      throw new CalcException("Lexical error in expression: " + expr)
    }

    makeNodes(calc.simplifiedTree.get)
    makeEdges(calc.simplifiedTree.get)

    if (!graph.valid()) {
      throw new CalcException("Calculator graph is invalid")
    }

    calc
  }

  def run(): OutputNode = {
    val runtime = graph.runtime

    for (varname <- varMap.keySet) {
      val node = varMap(varname)
      if (bindings.contains(varname)) {
        node.write(new NumberItem(bindings(varname)))
        //node.close()
      } else {
        throw new CalcException("Expression contains unbound variable: " + varname)
      }
    }

    runtime.run()
    runtime.waitForPipeline()
    output
  }

  def makeNodes(node: XdmNode): Unit = {
    val children = XdmNodes.children(node)

    node.getNodeKind match {
      case XdmNodeKind.DOCUMENT =>
        for (child <- children) { makeNodes(child) }
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
          gnode = Some(graph.createNode(new GenerateLiteral(node.getStringValue.toInt)))
        } else if (node.getNodeName == _VarRef) {
          if (!varMap.contains(nodeId)) {
            gnode = Some(graph.createInputNode(nodeId))
            varMap.put(node.getStringValue, gnode.get.asInstanceOf[InputNode])
          }
        } else if (node.getNodeName == _TOKEN || node.getNodeName == _QName || node.getNodeName == _ArgumentList) {
          // nop
        } else {
          halt("Unexpected node name: " + node.getNodeName)
        }

        if (gnode.isDefined) {
          if (firstElement) {
            graph.addEdge(linkFrom(gnode.get), "result", output, "source")
            firstElement = false
          }
          nodeMap.put(nodeId, gnode.get)
        }

        for (child <- children) {
          makeNodes(child)
        }
      case _ => Unit
    }
  }

  def makeEdges(node: XdmNode): Unit = {
    val children = XdmNodes.children(node)

    node.getNodeKind match {
      case XdmNodeKind.DOCUMENT =>
        for (child <- children) { makeEdges(child) }
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
        } else if (node.getNodeName == _UnaryExpr) {
          val expr = nodeMap(name(children(1)))
          graph.addEdge(expr, "result", gnode.get, "source")
        }
        for (child <- children) { makeEdges(child) }
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
      halt("Unexpected token")
      ""
    }
  }

  def name(node: XdmNode): String = {
    node.getNodeName + "_" + node.getAttributeValue(_nid)
  }

  def halt(msg: String): Unit = {
    println(msg)
    System.exit(0)
  }

}
