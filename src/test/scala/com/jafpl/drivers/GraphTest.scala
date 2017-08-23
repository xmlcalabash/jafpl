package com.jafpl.drivers

import java.io.{File, PrintWriter}

import com.jafpl.graph.Graph
import com.jafpl.io.{BufferConsumer, PrintingConsumer}
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.{BufferSink, Count, Identity, LogBinding, ProduceBinding, Producer, RaiseError, Sink, Sleep}

object GraphTest extends App {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  //val pw = new PrintWriter(new File("/projects/github/xproc/jafpl/pg.xml"))
  //pw.write(graph.asXML.toString)
  //pw.close()

  runEleven()

  def runEleven(): Unit = {
    val graph = new Graph()
    val bc = new BufferSink()

    val pipeline = graph.addPipeline()
    val p1       = pipeline.addAtomic(new Producer(List("P1")), "P1")
    val p2       = pipeline.addAtomic(new Producer(List("P2")), "P2")
    val p3       = pipeline.addAtomic(new Producer(List("P3")), "P3")
    val sleep    = pipeline.addAtomic(new Sleep(500), "sleep")
    val consumer = pipeline.addAtomic(bc, "consumer")

    graph.addEdge(p1, "result", pipeline, "result")
    graph.addEdge(p2, "result", pipeline, "result")
    graph.addEdge(p3, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")
    p2.dependsOn(sleep)
    p3.dependsOn(sleep)

    graph.close()
    val pw = new PrintWriter(new File("/projects/github/xproc/jafpl/pg.xml"))
    pw.write(graph.asXML.toString)
    pw.close()

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.size == 3)
    assert(bc.items.head == "P1")
    // P2 and P3 can occur in either order
  }

  def runTen(): Unit = {
    val graph = new Graph()

    val binding  = graph.addBinding("foo")
    val pipeline = graph.addPipeline()

    val pb1      = pipeline.addAtomic(new ProduceBinding("foo"), "pb")
    val pb2      = pipeline.addAtomic(new ProduceBinding("foo"), "pb")
    val count    = pipeline.addAtomic(new Count(), "count")

    graph.addBindingEdge(binding, pb1)
    graph.addBindingEdge(binding, pb2)

    graph.addEdge(pb1, "result", count, "source")
    graph.addEdge(pb2, "result", count, "source")

    graph.addEdge(count, "result", pipeline, "result")
    graph.addOutput(pipeline, "result")

    graph.close()

    val pw = new PrintWriter(new File("/projects/github/xproc/jafpl/pg.xml"))
    pw.write(graph.asXML.toString)
    pw.close()

    val runtime = new GraphRuntime(graph, runtimeConfig)

    runtime.bindings("foo").set("Spoon!")

    val bc = new BufferConsumer()
    runtime.outputs("result").setProvider(bc)
    runtime.run()

    println(bc.items.head)

  }

  def runNine(): Unit = {
    val graph = new Graph()

    val pipeline = graph.addPipeline()
    val producer = pipeline.addAtomic(new Producer(List("DOCUMENT")), "producer")
    val ident1 = pipeline.addAtomic(new Identity(), "ident1")
    val ident2 = pipeline.addAtomic(new Identity(), "ident2")
    val consumer = pipeline.addAtomic(new Sink(), "consumer")

    graph.addEdge(producer, "result", pipeline, "source")
    graph.addEdge(pipeline, "source", ident1, "source")

    graph.addEdge(ident1, "result", ident2, "source")
    graph.addEdge(ident2, "result", ident1, "source")

    graph.addEdge(pipeline, "result", consumer, "source")

    graph.close()
    assert(!graph.valid)
  }

  def runEight(): Unit = {
    val graph = new Graph()

    val binding  = graph.addBinding("foo")
    val pipeline = graph.addPipeline()

    val pb       = pipeline.addAtomic(new ProduceBinding("foo"), "pb")

    graph.addBindingEdge(binding, pb)
    graph.addEdge(pb, "result", pipeline, "result")
    graph.addOutput(pipeline, "result")

    graph.close()

    val pw = new PrintWriter(new File("/projects/github/xproc/jafpl/pg.xml"))
    pw.write(graph.asXML.toString)
    pw.close()


    val runtime = new GraphRuntime(graph, runtimeConfig)

    runtime.bindings("foo").set("Spoon!")

    val bc = new BufferConsumer()
    runtime.outputs("result").setProvider(bc)

    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == "Spoon!")
  }

  def runSeven(): Unit = {
    val graph = new Graph()

    val pipeline = graph.addPipeline(None)
    val ident = pipeline.addAtomic(new Identity(), "ident")

    graph.addEdge(pipeline, "source", ident, "source")
    graph.addEdge(ident, "result", pipeline, "result")

    graph.addInput(pipeline, "source")
    graph.addOutput(pipeline, "result")

    graph.close()
    //val pw = new PrintWriter(new File("/projects/github/xproc/jafpl/pg.xml"))
    //pw.write(graph.asXML.toString)
    //pw.close()

    val runtime = new GraphRuntime(graph, runtimeConfig)

    runtime.inputs("source").send("Input document")

    val bc = new BufferConsumer()
    runtime.outputs("result").setProvider(bc)

    runtime.run()

    println(bc.items.head)

  }

  def runTwo(): Unit = {
    val graph = new Graph()
    val bc = new BufferSink()

    val pipeline = graph.addPipeline()
    val p1       = pipeline.addAtomic(new Producer(List("P1", "P2")), "producer")
    val ident    = pipeline.addAtomic(new Identity(false), "identity")
    val consumer = pipeline.addAtomic(bc, "consumer")

    graph.addEdge(p1, "result", pipeline, "source")
    graph.addEdge(pipeline, "source", ident, "source")
    graph.addEdge(ident, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    graph.close()
    println(graph.asXML)
  }

  def runThree(): Unit = {
    val graph = new Graph()

    val pipeline = graph.addPipeline(None)
    val producer = pipeline.addAtomic(new Producer(List("SomeDocument")), "producer")
    val choose = pipeline.addChoose("choose")
    val when1 = choose.addWhen("true", "when1")
    val when2 = choose.addWhen("false", "when2")

    val p1 = when1.addAtomic(new Producer(List("WHEN1")), "p1")
    val p2 = when2.addAtomic(new Producer(List("WHEN2")), "p2")

    val bc = new BufferSink()
    val consumer = pipeline.addAtomic(bc, "finalconsumer")

    graph.addEdge(producer, "result", when1, "condition")
    graph.addEdge(producer, "result", when2, "condition")

    graph.addEdge(p1, "result", when1, "result")
    graph.addEdge(p2, "result", when2, "result")

    graph.addEdge(when1, "result", choose, "result")
    graph.addEdge(when2, "result", choose, "result")

    graph.addEdge(choose, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    graph.close()
    println(graph.asXML)

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == "WHEN1")

  }

  def runFour(): Unit = {
    val bc = new BufferSink()

    val graph = new Graph()
    val pipeline = graph.addPipeline()
    //val p1       = pipeline.addAtomic(new Producer(List("doc1")), "prod")
    val p2       = pipeline.addAtomic(new LogBinding(), "logbinding")
    //val p3       = pipeline.addAtomic(new Sink(), "sink")
    val binding  = pipeline.addVariable("message", "this is the bound value")

    //graph.addEdge(p1, "result", p3, "source")
    graph.addBindingEdge(binding, p2)

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()
  }

  def runFive(): Unit = {
    val graph = new Graph()
    val pipeline = graph.addPipeline()
    val sink = pipeline.addAtomic(new Sink(), "sink")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()
  }

  def runSix(): Unit = {
    val bc = new BufferSink()

    val graph = new Graph()
    val pipeline = graph.addPipeline()
    val p1       = pipeline.addAtomic(new Producer(List("doc1")), "p1")
    val p2       = pipeline.addAtomic(new Producer(List("doc2")), "p2")
    val p3       = pipeline.addAtomic(new Producer(List("doc3")), "p3")

    val trycatch = pipeline.addTryCatch("trycatch")
    val try1     = trycatch.addTry("try")
    val ident    = try1.addAtomic(new RaiseError("e2"), "e2")
    val catch1   = trycatch.addCatch("catch1", List("e1","e2"))
    val ident1   = catch1.addAtomic(new Identity(), "ident1")
    val catch2   = trycatch.addCatch("catch2")
    val ident2   = catch2.addAtomic(new Identity(), "ident2")
    val consumer = pipeline.addAtomic(bc, "consumer")

    graph.addEdge(p1, "result", ident, "source")
    graph.addEdge(ident, "result", try1, "result")
    graph.addEdge(try1, "result", trycatch, "result")

    graph.addEdge(p2, "result", ident1, "source")
    graph.addEdge(ident1, "result", catch1, "result")
    graph.addEdge(catch1, "result", trycatch, "result")

    graph.addEdge(p3, "result", ident2, "source")
    graph.addEdge(ident2, "result", catch2, "result")
    graph.addEdge(catch2, "result", trycatch, "result")

    graph.addEdge(trycatch, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    graph.close()
    //println(graph.asXML)

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == "doc2")

  }

}
