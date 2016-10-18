package com.jafpl.drivers

import java.io.FileWriter

import com.jafpl.graph.{Graph, LoopStart, Node}
import com.jafpl.steps.{Bang, GenerateLiteral}
import net.sf.saxon.s9api._

object BangDemo extends App {
  val processor = new Processor(false)
  val graph = new Graph()

  val dumpGraph = Some("pg.xml")

  val input = graph.createNode(new GenerateLiteral(4))
  val output = graph.createOutputNode("OUTPUT")

  val bang = graph.createNode(new Bang())

  val group = graph.createGroupNode(List(bang))

  val outerGroup = graph.createGroupNode(List(group))

  graph.addEdge(input, "result", bang, "source")

  graph.addEdge(bang, "result", group.compoundEnd, "I_result")
  graph.addEdge(group.compoundEnd, "result", outerGroup.compoundEnd, "I_result")
  graph.addEdge(outerGroup.compoundEnd, "result", output, "source")

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

  val runtime = graph.runtime
  runtime.run()
  runtime.waitForPipeline()

  if (graph.exception.isDefined) {
    println("Execution failed: " + graph.exception.get)
    println("At step: " + graph.exceptionNode.get)
  }

  var item = output.read()
  while (item.isDefined) {
    println(item.get)
    item = output.read()
  }

  runtime.teardown()

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
