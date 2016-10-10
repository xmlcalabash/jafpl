package com.jafpl.drivers

import java.io.FileWriter

import com.jafpl.calc.{AddExpr, IterateIntegers, NumberLiteral}
import com.jafpl.graph.{Graph, Runtime}
import com.jafpl.items.{NumberItem, StringItem}
import net.sf.saxon.s9api.Processor

object GraphTest extends App {
  val processor = new Processor(false)

  val graph = new Graph()

  nodes012(graph)

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

  def nodes011(graph: Graph): Unit = {
    val a = graph.createNode("a")
    val s1 = graph.createNode("s1")
    val s2 = graph.createNode("s2")
    val foreach = graph.createIteratorNode(List(s1,s2))
    val c = graph.createNode("c")

    val outside = graph.createNode("outer")

    graph.addEdge(a, "output", foreach, "source")
    graph.addEdge(s1, "result", s2, "source")
    graph.addEdge(outside, "result", s1, "secondary")
    graph.addEdge(foreach, "current", s1, "source")
    graph.addEdge(s2, "result", foreach.endNode, "I_result")
    graph.addEdge(foreach.endNode, "result", c, "source")
  }

  def nodes012(graph: Graph): Unit = {
    val a = graph.createNode("a")
    val alt1 = graph.createNode("alt1")
    //val alt2 = graph.createNode("alt2")
    val alt3 = graph.createNode("alt3")
    val when1 = graph.createWhenNode(List(alt1))
    //val when2 = graph.createWhenNode(List(alt2))
    val other = graph.createWhenNode(List(alt3))
    val choose = graph.createChooseNode(List(when1,other)) //when2
    val c = graph.createNode("c")

    graph.addEdge(a, "result", choose, "source")
    graph.addEdge(choose.endNode, "result", c, "source")
    graph.addEdge(alt1, "result", when1.endNode, "I_result")
    //graph.addEdge(alt2, "result", when2.endNode, "I_result")
    graph.addEdge(alt3, "result", other.endNode, "I_result")

    graph.addEdge(choose, "when1_condition", when1, "condition")
    //graph.addEdge(choose, "when2_condition", when2, "condition")
    graph.addEdge(choose, "true_condition", other, "condition")

    graph.addEdge(when1.endNode, "result", choose.endNode, "I_result")
    //graph.addEdge(when2.endNode, "result", choose.endNode, "I_result")
    graph.addEdge(other.endNode, "result", choose.endNode, "I_result")

    val outside = graph.createNode("outer")
    graph.addEdge(outside, "result", alt3, "source")

    /*
    graph.addEdge(a, "output", foreach, "source")
    graph.addEdge(s1, "result", s2, "source")
    graph.addEdge(outside, "result", s1, "secondary")
    graph.addEdge(foreach, "current", s1, "source")
    graph.addEdge(s2, "result", foreach.endNode, "I_result")
    graph.addEdge(foreach.endNode, "result", c, "source")
    */
  }


}
