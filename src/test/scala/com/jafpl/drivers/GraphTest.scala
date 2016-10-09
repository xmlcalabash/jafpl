package com.jafpl.drivers

import java.io.FileWriter

import com.jafpl.calc.{AddExpr, NumberLiteral}
import com.jafpl.graph.{Graph, Runtime}
import com.jafpl.items.{NumberItem, StringItem}
import net.sf.saxon.s9api.Processor

object GraphTest extends App {
  val processor = new Processor(false)

  val graph = new Graph()

  nodes010(graph)

  val valid = graph.valid()
  println(valid)

  if (valid) {
    val pgw = new FileWriter("pg.xml")
    val gdump = graph.dump(processor)
    pgw.write(gdump.toString)
    pgw.close()
    println(gdump)
  }

  def nodes000(graph: Graph): Unit = {
    val a = graph.createNode("a")
  }

  def nodes001(graph: Graph): Unit = {
    val a = graph.createNode("a")
    val b = graph.createNode("b")

    graph.addEdge(a, "output", b, "input")
  }

  def nodes002(graph: Graph): Unit = {
    val a = graph.createNode("a")
    val b = graph.createNode("b")
    val c = graph.createNode("c")

    graph.addEdge(a, "output", b, "input")
    graph.addEdge(b, "output", c, "input")
  }

  def nodes003(graph: Graph): Unit = {
    val a = graph.createNode("a")
    val b = graph.createNode("b")
    val c = graph.createNode("c")

    graph.addEdge(a, "output", b, "input")
    graph.addEdge(b, "input", c, "input")
  }

  def nodes004(graph: Graph): Unit = {
    val a = graph.createNode("a")
    val b = graph.createNode("b")
    val c = graph.createNode("c")

    graph.addEdge(a, "output", c, "input")
    graph.addEdge(b, "output", c, "input")
  }

  def nodes005(graph: Graph): Unit = {
    val a = graph.createNode("a")
    val b = graph.createNode("b")
    val c = graph.createNode("c")
    val d = graph.createNode("d")

    graph.addEdge(a, "output", d, "input")
    graph.addEdge(b, "output", d, "input")
    graph.addEdge(c, "output", d, "input")
  }

  def nodes006(graph: Graph): Unit = {
    val a = graph.createNode("a")
    val b = graph.createNode("b")
    val c = graph.createNode("c")

    graph.addEdge(a, "output", b, "input")
    graph.addEdge(a, "output", c, "input")
  }

  def nodes007(graph: Graph): Unit = {
    val a = graph.createNode("a")
    val b = graph.createNode("b")
    val c = graph.createNode("c")
    val d = graph.createNode("d")

    graph.addEdge(a, "output", b, "input")
    graph.addEdge(a, "output", c, "input")
    graph.addEdge(a, "output", d, "input")
  }

  def nodes008(graph: Graph): Unit = {
    val a = graph.createNode("a")
    val b = graph.createNode("b")

    graph.addEdge(a, "output", b, "input")
    graph.addEdge(a, "output", b, "input")
  }

  def nodes009(graph: Graph): Unit = {
    val a = graph.createNode("a")
    val b = graph.createNode("b")

    graph.addEdge(a, "output", b, "input")
    graph.addEdge(a, "output", b, "input")
    graph.addEdge(a, "output", b, "input")
  }

  def nodes010(graph: Graph): Unit = {
    val a = graph.createNode("a")
    val b = graph.createNode("b")
    val c = graph.createNode("c")

    graph.addEdge(a, "output", b, "source")
    graph.addEdge(b, "current", c, "input")
  }
}
