package com.jafpl.calc

import com.jafpl.items.{NumberItem, StringItem}
import com.jafpl.runtime.DefaultStep

/**
  * Created by ndw on 10/7/16.
  */
class StringLiteral(str: String) extends DefaultStep {
  label = "str_lit"

  override def run(): Unit = {
    val item = new StringItem(str)
    controller.send("result", item)
  }
}
