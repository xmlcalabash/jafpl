package com.jafpl.steps

import com.jafpl.items.{GenericItem, NumberItem, StringItem}
import com.jafpl.runtime.DefaultStep

/**
  * Created by ndw on 10/7/16.
  */
class GenerateLiteral(item: GenericItem) extends DefaultStep {
  def this(num: Int) {
    this(new NumberItem(num))
  }
  def this(str: String) {
    this(new StringItem(str))
  }

  label = item match {
    case m: NumberItem => "gen_lit_" + m.get
    case m: StringItem => "gen_lit_" + m.get
    case _ => "gen_lit"
  }

  override def run(): Unit = {
    controller.send("result", item)
  }
}
