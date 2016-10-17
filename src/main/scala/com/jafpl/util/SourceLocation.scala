package com.jafpl.util

import java.net.URI

/**
  * Created by ndw on 10/16/16.
  */
trait SourceLocation {
  def baseURI: URI
  def lineNumber: Int
  def columnNumber: Int
}
