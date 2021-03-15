package com.jafpl.runtime

import com.jafpl.graph.{EmptySource, Node}

class EmptySourceAction(override val node: EmptySource) extends AbstractAction(node) {
  override def initialize(scheduler: Scheduler, node: Node): Unit = {
    super.initialize(scheduler, node)
  }

  override def run(): Unit = {
    super.run()
    scheduler.finish(node)
  }
}
