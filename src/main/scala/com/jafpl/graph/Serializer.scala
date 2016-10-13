package com.jafpl.graph

/**
  * Created by ndw on 10/9/16.
  */
object Serializer {
  val NS_JAFPL_PG = "http://jafpl.com/ns/graph"

  val _name = "name"
  val _step = "step"
  val _uid = "uid"
  val _source = "source"
  val _destination = "destination"
  val _input_port = "input-port"
  val _output_port = "output-port"
  val _boundary = "boundary"
  val _compound_start = "compound-start"
  val _compound_end = "compound-end"
  val _compound_children = "compound-children"

  val pg_graph = "pg:graph"
  val pg_in_edge = "pg:in-edge"
  val pg_inputs = "pg:inputs"
  val pg_node = "pg:node"
  val pg_out_edge = "pg:out-edge"
  val pg_outputs = "pg:outputs"
}
