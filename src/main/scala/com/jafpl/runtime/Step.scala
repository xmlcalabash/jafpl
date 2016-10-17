package com.jafpl.runtime

import com.jafpl.messages.ItemMessage
import com.jafpl.util.SourceLocation

/**
  * Created by ndw on 10/3/16.
  */
trait Step {
  def label: String
  def location: Option[SourceLocation]
  def setup(controller: StepController,
            inputPorts: List[String],
            outputPorts: List[String])
  def reset()
  def run()
  def teardown()
  def receive(port: String, msg: ItemMessage)
}
