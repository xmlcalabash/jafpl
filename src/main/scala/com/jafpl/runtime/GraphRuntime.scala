package com.jafpl.runtime

import com.jafpl.graph.{InputNode, OutputNode}
import com.jafpl.items.GenericItem

/**
  * Created by ndw on 10/17/16.
  */
trait GraphRuntime {
  def inputs(): List[InputNode]
  def outputs(): List[OutputNode]
  def run()
  def running: Boolean
  def write(port: String, item: GenericItem)
  def close(port: String)
  def read(port: String): Option[GenericItem]
  def reset()
  def waitForPipeline(): Option[Throwable]
  def teardown()
  def exception: Option[Throwable]
  def exceptionStep: Option[Step]
}
