package com.jafpl.graph

import net.sf.saxon.s9api.QName

/**
  * Created by ndw on 10/9/16.
  */
object Serializer {
  val NS_JAFPL_PG = "http://jafpl.com/ns/graph"

  val _name = new QName("", "name")
  val _step = new QName("", "step")
  val _uid = new QName("", "uid")
  val _source = new QName("", "source")
  val _destination = new QName("", "destination")
  val _input_port = new QName("", "input-port")
  val _output_port = new QName("", "output-port")

  val pg_graph = new QName("pg", NS_JAFPL_PG, "graph")
  val pg_in_edge = new QName("pg", NS_JAFPL_PG, "in-edge")
  val pg_inputs = new QName("pg", NS_JAFPL_PG, "inputs")
  val pg_node = new QName("pg", NS_JAFPL_PG, "node")
  val pg_out_edge = new QName("pg", NS_JAFPL_PG, "out-edge")
  val pg_outputs = new QName("pg", NS_JAFPL_PG, "outputs")
}
