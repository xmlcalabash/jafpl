package com.jafpl.test

import com.jafpl.config.Jafpl
import com.jafpl.exceptions.JafplLoopDetected
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.{BufferSink, Identity, Manifold, Producer, Sink, Sleep}
import org.scalatest.flatspec.AnyFlatSpec

class DependsSpec extends AnyFlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  "Two independent steps " should " be unordered" in {
    val graph    = Jafpl.newInstance().newGraph()
    val bc = new BufferSink()

    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val prodA = pipeline.addAtomic(new Producer(List("A")), "docA")
    graph.addEdge(prodA, "result", pipeline, "result")
    val prodB = pipeline.addAtomic(new Producer(List("B")), "docB")
    graph.addEdge(prodB, "result", pipeline, "result")
    graph.addOutput(pipeline, "result")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.outputs("result").setConsumer(bc)
    runtime.runSync()

    assert(bc.items.size == 2)
    assert(bc.items.head == "A" || bc.items.head == "B")
    if (bc.items.head == "A") {
      assert(bc.items(1) == "B")
    } else {
      assert(bc.items(1) == "A")
    }
  }

  "If A depends on B, B " should " always be first" in {
    val graph    = Jafpl.newInstance().newGraph()
    val bc = new BufferSink()

    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val prodA = pipeline.addAtomic(new Producer(List("A")), "docA")
    graph.addEdge(prodA, "result", pipeline, "result")
    val prodB = pipeline.addAtomic(new Producer(List("B")), "docB")
    graph.addEdge(prodB, "result", pipeline, "result")

    prodA.dependsOn(prodB)

    graph.addOutput(pipeline, "result")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.outputs("result").setConsumer(bc)
    runtime.runSync()

    assert(bc.items.size == 2)
    assert(bc.items.head == "B")
    assert(bc.items(1) == "A")
  }

  "If B depends on A, A " should " always be first" in {
    val graph    = Jafpl.newInstance().newGraph()
    val bc = new BufferSink()

    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val prodA = pipeline.addAtomic(new Producer(List("A")), "docA")
    graph.addEdge(prodA, "result", pipeline, "result")
    val prodB = pipeline.addAtomic(new Producer(List("B")), "docB")
    graph.addEdge(prodB, "result", pipeline, "result")

    prodB.dependsOn(prodA)

    graph.addOutput(pipeline, "result")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.outputs("result").setConsumer(bc)
    runtime.runSync()

    assert(bc.items.size == 2)
    assert(bc.items.head == "A")
    assert(bc.items(1) == "B")
  }

  "If A depends on B, B depends on C, and C depends on A, that " should " be a loop error" in {
    val graph    = Jafpl.newInstance().newGraph()
    val bc = new BufferSink()

    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val prodA = pipeline.addAtomic(new Producer(List("A")), "docA")
    val identB = pipeline.addAtomic(new Identity(), "identB")
    val identC = pipeline.addAtomic(new Identity(), "identC")

    graph.addEdge(prodA, "result", identB, "source")
    graph.addEdge(identB, "result", identC, "source")
    graph.addEdge(identC, "result", pipeline, "result")

    prodA.dependsOn(identC)

    graph.addOutput(pipeline, "result")

    try {
      val runtime = new GraphRuntime(graph, runtimeConfig)
      runtime.outputs("result").setConsumer(bc)
      runtime.runSync()
      fail()
    } catch {
      case _: JafplLoopDetected => assert(true)
      case _: Throwable => fail("Expected JafplLoopDetected")
    }
  }

  "A complicated example from an actual pipeline" should "fail" in {
    /*
  <p:try>
    <p:group depends="last">
      <p:identity>
        <p:with-input><doc xmlns=""/></p:with-input>
      </p:identity>
    </p:group>
    <p:catch>
      <p:identity/>
    </p:catch>
  </p:try>
  <p:identity name="last"/>
     */

    val graph    = Jafpl.newInstance().newGraph()
    val bc = new BufferSink()

    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val prodA = pipeline.addAtomic(new Producer(List("A")), "docA")

    val p_try = pipeline.addTryCatch("try", Manifold.ALLOW_ANY)
    val p_group = p_try.addTry("group", Manifold.ALLOW_ANY)
    val p_gid = p_group.addAtomic(new Identity(), "group_ident")
    val p_catch = p_try.addCatch("catch", Manifold.ALLOW_ANY)
    val p_cid = p_catch.addAtomic(new Identity(), "catch_ident")
    val p_final = pipeline.addAtomic(new Identity(), label="final_ident")

    graph.addEdge(prodA, "result", p_gid, "source")
    graph.addEdge(prodA, "result", p_cid, "source")
    graph.addEdge(p_gid, "result", p_group, "result")
    graph.addEdge(p_group, "result", p_try, "result")
    graph.addEdge(p_cid, "result", p_catch, "result")
    graph.addEdge(p_catch, "result", p_try, "result")

    graph.addEdge(p_try, "result", p_final, "source")
    graph.addEdge(p_final, "result", pipeline, "result")

    p_group.dependsOn(p_final)

    graph.addOutput(pipeline, "result")

    graph.dump()

    try {
      val runtime = new GraphRuntime(graph, runtimeConfig)
      runtime.outputs("result").setConsumer(bc)
      runtime.runSync()
    } catch {
      case _: JafplLoopDetected => assert(true)
      case ex: Exception => fail("Expected JafplLoopDetected")
    }
  }
}