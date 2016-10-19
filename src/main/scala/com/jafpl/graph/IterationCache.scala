package com.jafpl.graph

import com.jafpl.runtime.Cacher

/**
  * Created by ndw on 10/2/16.
  */
class IterationCache(graph: Graph) extends Node(graph, Some(new Cacher())) {
  label = Some("_iter_cache")
}
