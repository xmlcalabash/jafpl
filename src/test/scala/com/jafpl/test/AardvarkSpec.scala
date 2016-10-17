package com.jafpl.test

import net.sf.saxon.s9api.Processor
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner

// This just tests my understanding of the test framework

@RunWith(classOf[JUnitRunner])
class AardvarkSpec extends FlatSpec {
  val processor = new Processor(false)

  "ScalaTest " should "be able to add two small numbers" in {
    assert(1 + 2 === 4)
  }

}