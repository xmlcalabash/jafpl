package com.jafpl.drivers

import java.io.FileWriter

import com.jafpl.calc.{CalcException, Calculator}

import scala.collection.mutable

object CalcDemo extends App {
  var bindings = mutable.HashMap.empty[String, Int]
  var dumpTree: Option[String] = None
  var dumpSimpleTree: Option[String] = None
  var dumpGraph: Option[String] = None
  var quiet = false
  var expr = ""

  var argpos = 0
  while (argpos < args.length) {
    val arg = args(argpos)
    if (arg.startsWith("-t")) {
      dumpTree = Some(arg.substring(2))
    } else if (arg.startsWith("-s")) {
      dumpSimpleTree = Some(arg.substring(2))
    } else if (arg.startsWith("-g")) {
      dumpGraph = Some(arg.substring(2))
    } else if (arg == "-q") {
      quiet = true
    } else if (arg.startsWith("-v")) {
      val bind = arg.substring(2)
      val pos = bind.indexOf("=")
      if (pos > 0) {
        bindings.put(bind.substring(0, pos), bind.substring(pos + 1).toInt)
      } else {
        throw new CalcException("Unparsable variable binding: " + arg)
      }
    } else {
      expr += " " + arg
    }
    argpos += 1
  }

  if (!quiet) {
    println("Evaluate expression: " + expr)
    if (bindings.nonEmpty) {
      println("Where:")
      for (varname <- bindings.keySet) {
        println("\t" + varname + "=" + bindings(varname))
      }
    }
  }

  val calculator = new Calculator(expr, bindings.toMap)
  val calc = calculator.parse()

  if (dumpTree.isDefined) {
    if (dumpTree.get == "") {
      println(calc.parseTree.get.toString)
    } else {
      val pw = new FileWriter(dumpTree.get)
      pw.write(calc.parseTree.get.toString)
      pw.close()
    }
  }

  if (dumpSimpleTree.isDefined) {
    if (dumpSimpleTree.get == "") {
      println(calc.simplifiedTree.get.toString)
    } else {
      val pw = new FileWriter(dumpSimpleTree.get)
      pw.write(calc.simplifiedTree.get.toString)
      pw.close()
    }
  }

  if (dumpGraph.isDefined) {
    if (dumpGraph.get == "") {
      println(calculator.graph.dump())
    } else {
      val pw = new FileWriter(dumpGraph.get)
      pw.write(calculator.graph.dump())
      pw.close()
    }
  }

  val output = calculator.run()

  var item = output.read()
  while (item.isDefined) {
    println(item.get)
    item = output.read()
  }
}
