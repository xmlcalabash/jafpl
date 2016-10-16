package com.jafpl.drivers

import java.io.FileWriter

import com.jafpl.calc._
import com.jafpl.graph.{Graph, LoopStart, Node, Runtime}
import net.sf.saxon.s9api._

object GroupDemo extends App {
  val processor = new Processor(false)
  val graph = new Graph()

  val dumpGraph = Some("pg.xml")

  val input = graph.createNode(new NumberLiteral(4))
  val output = graph.createOutputNode("OUTPUT")

  val double = graph.createNode(new Doubler())

  val group = graph.createGroupNode(List(double))

  graph.addEdge(input, "result", double, "source")

  graph.addEdge(double, "result", group.compoundEnd, "I_result")
  graph.addEdge(group.compoundEnd, "result", output, "source")

  val valid = graph.valid()
  if (!valid) {
    halt("Graph isn't valid?")
  }

  if (dumpGraph.isDefined) {
    if (dumpGraph.get == "") {
      println(graph.dump())
    } else {
      val pw = new FileWriter(dumpGraph.get)
      pw.write(graph.dump())
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
      case l: LoopStart => l.compoundEnd
      case _ => node
    }
  }

  def halt(msg: String): Unit = {
    println(msg)
    System.exit(0)
  }
}
