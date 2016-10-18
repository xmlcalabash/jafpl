package com.jafpl.test

import com.jafpl.calc.Calculator
import com.jafpl.graph.OutputNode
import com.jafpl.items.NumberItem
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner

import scala.collection.mutable

@RunWith(classOf[JUnitRunner])
class CalcSpec extends FlatSpec {
  "A Calc with 1+2" should "return 3" in {
    val output = runGraph("1+2")

    var count = 1
    var item = output.read()
    while (item.isDefined) {
      assert(count === 1)
      item.get match {
        case n: NumberItem =>
          assert(n.get === 3)
        case _ =>
          assert(false)
      }
      count += 1
      item = output.read()
    }
  }

  "A Calc with ((1+2)*(3+4+5) div 9) * $foo" should "return 8 when $foo=2" in {
    val bind = mutable.HashMap("foo" -> 2)
    val output = runGraph("(((1+2)*(3+4+5)) div 9) * $foo", bind.toMap)

    var count = 1
    var item = output.read()
    while (item.isDefined) {
      assert(count === 1)
      item.get match {
        case n: NumberItem =>
          assert(n.get === 8)
        case _ =>
          assert(false)
      }
      count += 1
      item = output.read()
    }
  }

  def runGraph(expr: String): OutputNode = {
    runGraph(expr, mutable.HashMap.empty[String, Int].toMap)
  }

  def runGraph(expr: String, bindings: Map[String,Int]): OutputNode = {
    val calculator = new Calculator(expr, bindings)
    val calc = calculator.parse()
    calculator.run()
  }
}