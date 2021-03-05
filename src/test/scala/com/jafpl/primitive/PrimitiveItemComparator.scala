package com.jafpl.primitive

import com.jafpl.util.ItemComparator

class PrimitiveItemComparator extends ItemComparator {
  override def areTheSame(a: Any, b: Any) = {
    var same = false

    a match {
      case num: Long => same = same || (num == 0)
      case num: Int => same = same || (num == 0)
      case _ => ()
    }

    b match {
      case num: Long => same = same || (num == 0)
      case num: Int => same = same || (num == 0)
      case _ => ()
    }

    //println("Compare: " + a + "=" + b + ": " + same)

    same
  }
}
