package com.jafpl.drivers

import java.io.{File, PrintWriter}

import com.jafpl.graph.Graph
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.steps.{Identity, Producer}
import com.jafpl.util.DefaultErrorListener

/** Build graphs for the website docs.
  *
  */
object Website extends App {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()
  val graph = new Graph(new DefaultErrorListener())

  //val pw = new PrintWriter(new File("/projects/github/xproc/jafpl/pg.xml"))
  //pw.write(graph.asXML.toString)
  //pw.close()

  figureTwo()

  def figureTwo(): Unit = {

    val pipeline        = graph.addPipeline()
    val doSomething     = pipeline.addAtomic(new Identity(), "Do_Something")
    val doSomethingElse = pipeline.addAtomic(new Identity(), "Do_Something_Else")

    //val prod = pipeline.addAtomic(new Producer("foo"), "foo")
    //graph.addEdge(prod, "result", doSomething, "source")
    //graph.addEdge(prod, "result", doSomethingElse, "source")

    graph.addInput(pipeline, "source")
    graph.addOutput(pipeline.end, "result")
    graph.addEdge(pipeline, "source", doSomething, "source")
    graph.addEdge(pipeline, "source", doSomethingElse, "source")

    graph.addEdge(doSomething, "result", pipeline.end, "result")
    graph.addEdge(doSomethingElse, "result", pipeline.end, "result")

    graph.close()

    val pw = new PrintWriter(new File("/projects/github/xproc/jafpl/pg.xml"))
    pw.write(graph.asXML.toString)
    pw.close()
  }

}
