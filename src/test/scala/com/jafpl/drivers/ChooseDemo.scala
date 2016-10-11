package com.jafpl.drivers

import java.io.FileWriter

import com.jafpl.calc._
import com.jafpl.graph.{Graph, InputNode, LoopStart, Node, Runtime}
import com.jafpl.items.NumberItem
import com.jafpl.runtime.Chooser
import com.jafpl.xpath.{CalcParser, XdmNodes}
import net.sf.saxon.s9api._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object ChooseDemo extends App {
  val processor = new Processor(false)
  val graph = new Graph()

  val dumpGraph = Some("pg.xml")

  val input = graph.createNode("InputNumber", new NumberLiteral(0))
  val output = graph.createOutputNode("OUTPUT")

  val double = graph.createNode("Double", new Doubler())
  val flip = graph.createNode("FlipSign", new FlipSign())
  val zero = graph.createNode("Zero", new StringLiteral("zero"))

  val when1 = graph.createWhenNode(new WhenSigned("plus", choosePos = false), List(double))
  val when2 = graph.createWhenNode(new WhenSigned("minus", choosePos = true), List(flip))
  val other = graph.createWhenNode(List(zero))
  val choose = graph.createChooseNode(List(when1, when2, other))

  graph.addEdge(input, "result", when1, "condition")
  graph.addEdge(input, "result", when2, "condition")
  graph.addEdge(input, "result", other, "condition")

  graph.addEdge(input, "result", double, "source")
  graph.addEdge(input, "result", flip, "source")
  graph.addEdge(input, "result", zero, "source")

  graph.addEdge(when1.endNode, "result", choose.endNode, "I_result")
  graph.addEdge(when2.endNode, "result", choose.endNode, "I_result")
  graph.addEdge(other.endNode, "result", choose.endNode, "I_result")

  graph.addEdge(double, "result", when1.endNode, "I_result")
  graph.addEdge(flip, "result", when2.endNode, "I_result")
  graph.addEdge(zero, "result", other.endNode, "I_result")

  graph.addEdge(choose.endNode, "result", output, "source")

  val valid = graph.valid()
  if (!valid) {
    halt("Graph isn't valid?")
  }

  if (dumpGraph.isDefined) {
    if (dumpGraph.get == "") {
      println(graph.dump(processor).toString)
    } else {
      val pw = new FileWriter(dumpGraph.get)
      pw.write(graph.dump(processor).toString)
      pw.close()
    }
  }

  val runtime = new Runtime(graph)
  runtime.start()


  while (runtime.running) {
    Thread.sleep(100)
  }

  var item = output.read()
  while (item.isDefined) {
    println(item.get)
    item = output.read()
  }

  def linkFrom(node: Node): Node = {
    node match {
      case l: LoopStart => l.endNode
      case _ => node
    }
  }

  def halt(msg: String): Unit = {
    println(msg)
    System.exit(0)
  }
}
