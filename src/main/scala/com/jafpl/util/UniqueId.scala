package com.jafpl.util

private[jafpl] object UniqueId {
  private var theNextId: Long = 0
  def nextId: Long = {
    this.synchronized {
      val id = theNextId
      theNextId = theNextId + 1
      id
    }
  }
}
