package com.jafpl.calc

/**
  * Created by ndw on 10/9/16.
  */
class CalcException(val msg: String) extends RuntimeException {
  println("Error: " + msg)
}
