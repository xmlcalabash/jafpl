package com.jafpl.util

import java.io.StringWriter

import scala.collection.mutable

class XmlWriter {
  // FIXME: This should be a sealed trait or enum or something!
  private val UnknownState = 0
  private val StartDocument = 1
  private val StartElement = 2
  private val Content = 3

  val writer = new StringWriter()
  var state = UnknownState
  val namestack = mutable.Stack.empty[String]

  def startDocument(): Unit = {
    if (state == UnknownState) {
      state = StartDocument
    } else {
      throw new IllegalStateException("You can't do that")
    }
  }

  def addStartElement(name: String): Unit = {
    state match {
      case StartDocument =>
        writer.write("<" + name + " xmlns:pg='http://jafpl.com/ns/graph'")
        namestack.push(name)
        state = StartElement
      case StartElement =>
        writer.write(">")
        state = Content
        addStartElement(name)
      case Content =>
        writer.write("\n")
        writer.write("  " * namestack.size)
        writer.write("<" + name)
        namestack.push(name)
        state = StartElement
      case _ =>
    throw new IllegalStateException("You can't do that")
    }
  }

  def addEndElement(): Unit = {
    state match {
      case StartElement =>
        writer.write(">")
        state = Content
        addEndElement()
      case Content =>
        val name = namestack.pop()
        writer.write("\n")
        writer.write("  " * namestack.size)
        writer.write("</" + name + ">")
      case _ =>
        throw new IllegalStateException("You can't do that")
    }
  }

  def addAttribute(name: String, value: String): Unit = {
    if (state == StartElement) {
      writer.write(" ")
      writer.write(name)
      writer.write("=")
      writer.write("\"")
      writer.write(value.replace("\"", "&quot;"))
      writer.write("\"")
    } else {
      throw new IllegalStateException("You can't do that")
    }
  }

  def endDocument(): Unit = {
    state match {
      case StartElement =>
        writer.write(">")
        state = Content
        addEndElement()
      case Content =>
        Unit
      case _ =>
        throw new IllegalStateException("You can't do that")
    }
  }

  def getResult: String = {
    writer.toString
  }
}
