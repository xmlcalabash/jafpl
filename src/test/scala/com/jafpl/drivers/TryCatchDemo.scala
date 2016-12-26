package com.jafpl.drivers

import java.io.FileWriter

import com.jafpl.graph.{Graph, LoopStart, Node}
import com.jafpl.steps.{Bang, Doubler, GenerateLiteral}
import net.sf.saxon.s9api._

object TryCatchDemo extends App {
  val processor = new Processor(false)
  val graph = new Graph()

  val dumpGraph = Some("pg.xml")

  val input = graph.createNode(new GenerateLiteral(3))
  val output = graph.createOutputNode("OUTPUT")

  val double = graph.createNode(new Doubler())
  val bang = graph.createNode(new Bang())

  val group = graph.createGroupNode(List(bang))
  val catchNode = graph.createCatchNode(List(double))
  val tryNode = graph.createTryNode(group, List(catchNode))

  graph.addEdge(input, "result", bang, "source")
  graph.addEdge(bang, "result", group.compoundEnd, "I_result")

  graph.addEdge(input, "result", double, "source")
  graph.addEdge(double, "result", catchNode.compoundEnd, "I_result")

  graph.addEdge(group.compoundEnd, "result", tryNode.compoundEnd, "I_result")
  graph.addEdge(catchNode.compoundEnd, "result", tryNode.compoundEnd, "I_result")

  graph.addEdge(tryNode.compoundEnd, "result", output, "source")

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
