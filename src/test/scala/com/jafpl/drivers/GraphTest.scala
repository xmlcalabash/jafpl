package com.jafpl.drivers

import java.io.{File, PrintWriter}

import com.jafpl.graph.Graph
import com.jafpl.io.BufferConsumer
import com.jafpl.messages.Metadata
import com.jafpl.primitive.{PrimitiveItemComparator, PrimitiveItemTester, PrimitiveRuntimeConfiguration}
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.{BufferSink, Decrement, Identity, Producer, StringComposer, Uppercase}

object GraphTest extends App {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  //val pw = new PrintWriter(new File("/projects/github/xproc/jafpl/pg.xml"))
  //pw.write(graph.asXML.toString)
  //pw.close()

  runSeven()

  def runSeven(): Unit = {
    val graph = new Graph()
    val bc = new BufferSink()

    val pipeline = graph.addPipeline()

    val prod     = pipeline.addAtomic(new Producer(List("Now is the time; just do it.")), "prod")
    val viewport = pipeline.addViewport(new StringComposer(), "viewport")
    val uc       = viewport.addAtomic(new Uppercase(), "uc")
    val consumer = pipeline.addAtomic(bc, "consumer")

    graph.addEdge(prod, "result", viewport, "source")
    graph.addEdge(viewport, "current", uc, "source")
    graph.addEdge(uc, "result", viewport, "fribble")
    graph.addEdge(viewport, "fribble", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    graph.close()
    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == "NOW IS THE TIME; JUST DO IT.")
  }

  def runSix(): Unit = {
    val graph = new Graph()

    val pipeline  = graph.addPipeline()
    val producer1 = pipeline.addAtomic(new Producer("ONE"), "producer1")
    val producer2 = pipeline.addAtomic(new Producer("TWO"), "producer2")

    val bc1 = new BufferSink()
    val bc2 = new BufferSink()
    val consumer1 = pipeline.addAtomic(bc1, "consumer1")
    val consumer2 = pipeline.addAtomic(bc2, "consumer2")

    graph.addEdge(producer1, "result", pipeline, "result1")
    graph.addEdge(producer2, "result", pipeline, "result2")
    graph.addEdge(pipeline, "result1", consumer1, "source")
    graph.addEdge(pipeline, "result2", consumer2, "source")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc1.items.size == 1)
    assert(bc1.items.head == "ONE")

    assert(bc2.items.size == 1)
    assert(bc2.items.head == "TWO")
  }

  def runFive(): Unit = {
    val graph = new Graph()

    val pipeline = graph.addPipeline()
    val producer = pipeline.addAtomic(new Producer(List("1", "2", "3")), "producer")
    val forEach  = pipeline.addForEach("for-each")
    val ident    = forEach.addAtomic(new Identity(), "ident")

    val bc1 = new BufferSink()
    val bc2 = new BufferSink()
    val consumer1 = pipeline.addAtomic(bc1, "consumer1")
    val consumer2 = pipeline.addAtomic(bc2, "consumer2")

    graph.addEdge(producer, "result", forEach, "source")
    graph.addEdge(forEach, "current", ident, "source")
    graph.addEdge(ident, "result", forEach, "result1")
    graph.addEdge(ident, "result", forEach, "result2")
    graph.addEdge(forEach, "result1", pipeline, "result1")
    graph.addEdge(forEach, "result2", pipeline, "result2")
    graph.addEdge(pipeline, "result1", consumer1, "source")
    graph.addEdge(pipeline, "result2", consumer2, "source")

    graph.close()
    val pw = new PrintWriter(new File("/projects/github/xproc/jafpl/pg.xml"))
    pw.write(graph.asXML.toString)
    pw.close()

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    var count = 1
    for (buf <- bc1.items) {
      assert(buf.toString == count.toString)
      count += 1
    }
    assert(count == 4)

    count = 1
    for (buf <- bc2.items) {
      assert(buf.toString == count.toString)
      count += 1
    }
    assert(count == 4)
  }

  def runFour(): Unit = {
    val graph = new Graph()
    val pipeline = graph.addPipeline()
    val p1       = pipeline.addAtomic(new Producer(List("Hello")), "p1")

    graph.close()
    val pw = new PrintWriter(new File("/projects/github/xproc/jafpl/pg.xml"))
    pw.write(graph.asXML.toString)
    pw.close()

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()
  }

  def runThree(): Unit = {
    val graph = new Graph()
    val pipeline = graph.addPipeline()
    val p1       = pipeline.addAtomic(new Producer(List(0)), "p1")

    val tester   = new PrimitiveItemTester(runtimeConfig, ". > 0")
    val wstep    = pipeline.addWhile(tester)
    val decr     = wstep.addAtomic(new Decrement(), "decr")

    graph.addEdge(p1, "result", wstep, "source")
    graph.addEdge(wstep, "source", decr, "source")
    graph.addEdge(decr, "result", wstep, "result")

    graph.addEdge(wstep, "result", pipeline, "result")

    graph.addOutput(pipeline, "result")

    graph.close()
    val pw = new PrintWriter(new File("/projects/github/xproc/jafpl/pg.xml"))
    pw.write(graph.asXML.toString)
    pw.close()

    val runtime = new GraphRuntime(graph, runtimeConfig)
    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)
    runtime.run()

    println(bc.items.size)
    println(bc.items.head)
  }

  def runTwo(): Unit = {
    val graph = new Graph()
    val pipeline = graph.addPipeline()
    val p1       = pipeline.addAtomic(new Producer(List(7)), "p1")


    val comp = new PrimitiveItemComparator()

    val ustep    = pipeline.addUntil(comp)
    val decr     = ustep.addAtomic(new Decrement(), "decr")

    graph.addEdge(p1, "result", ustep, "source")
    graph.addEdge(ustep, "source", decr, "source")
    graph.addEdge(decr, "result", ustep, "result")

    graph.addEdge(ustep, "result", pipeline, "result")

    graph.addOutput(pipeline, "result")

    graph.close()
    val pw = new PrintWriter(new File("/projects/github/xproc/jafpl/pg.xml"))
    pw.write(graph.asXML.toString)
    pw.close()

    val runtime = new GraphRuntime(graph, runtimeConfig)
    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)
    runtime.run()

    println(bc.items.size)
    println(bc.items.head)
  }

  def runOne(): Unit = {
    val graph = new Graph()
    val pipeline = graph.addPipeline()
    val p1       = pipeline.addAtomic(new Producer(List(7)), "p1")

    val tester   = new PrimitiveItemTester(runtimeConfig, ". > 0")
    val wstep    = pipeline.addWhile(tester)
    val decr     = wstep.addAtomic(new Decrement(), "decr")

    graph.addEdge(p1, "result", wstep, "source")
    graph.addEdge(wstep, "source", decr, "source")
    graph.addEdge(decr, "result", wstep, "result")

    graph.addEdge(wstep, "result", pipeline, "result")

    graph.addOutput(pipeline, "result")

    graph.close()
    val pw = new PrintWriter(new File("/projects/github/xproc/jafpl/pg.xml"))
    pw.write(graph.asXML.toString)
    pw.close()

    val runtime = new GraphRuntime(graph, runtimeConfig)
    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)
    runtime.run()

    println(bc.items.size)
    println(bc.items.head)
  }
}
