package com.jafpl.xpath

import java.net.URI

import com.jafpl.util.TreeWriter
import com.jafpl.util.UniqueId
import net.sf.saxon.s9api.{Processor, QName, XdmNode, XdmNodeKind}

class CalcParser(processor: Processor, expr: String) {
  private var tree = new TreeWriter(processor)
  private val handler = new CalcHandler()
  private val parser = new CR_xpath_31_20151217(expr, handler)
  private var result: Option[XdmNode] = None
  private var simplified: Option[XdmNode] = None
  private val _XPath = new QName("", "XPath")
  private val _IntegerLiteral = new QName("", "IntegerLiteral")
  private val _QName = new QName("", "QName")
  private val _ParenthesizedExpr = new QName("", "ParenthesizedExpr")
  private val _TOKEN = new QName("", "TOKEN")
  private val _EOF = new QName("", "EOF")
  private val _nid = new QName("", "nid")

  try {
    tree.startDocument(URI.create("http://example.com/"))
    parser.parse_XPath()
    tree.endDocument()
    result = Some(tree.getResult)
    tree = new TreeWriter(processor)
    tree.startDocument(URI.create("http://example.com/"))
    simplify(tree, result.get)
    tree.endDocument()
    simplified = Some(tree.getResult)
  } catch {
    case t: Throwable =>
      print(t)
      t.printStackTrace()
  }

  def parseTree = result
  def simplifiedTree = simplified

  private def simplify(tree: TreeWriter, node: XdmNode): Unit = {
    node.getNodeKind match {
      case XdmNodeKind.DOCUMENT =>
        for (child <- XdmNodes.children(node)) {
          simplify(tree, child)
        }
      case XdmNodeKind.ELEMENT =>
        val children = XdmNodes.children(node)

        var keep = (children.size != 1)
        keep = keep || node.getNodeName == _IntegerLiteral
        keep = keep || node.getNodeName == _TOKEN
        keep = keep || node.getNodeName == _QName
        keep = keep && node.getNodeName != _ParenthesizedExpr
        keep = keep && node.getNodeName != _XPath
        keep = keep && node.getNodeName != _EOF

        if (node.getNodeName == _TOKEN
          && (node.getStringValue == "("
              || node.getStringValue == ")"
              || node.getStringValue == "$")) {
          // nop
        } else {
          if (keep) {
            tree.addStartElement(node)
            tree.addAttribute(_nid, UniqueId.nextId.toString)
            for (child <- children) {
              simplify(tree, child)
            }
            tree.addEndElement()
          } else {
            for (child <- children) {
              simplify(tree, child)
            }
          }
        }
      case _ => tree.addSubtree(node)
    }
  }

  private class CalcHandler extends CR_xpath_31_20151217.EventHandler {
    private var chars: CharSequence = ""
    private var indent = ""

    override def reset(str: CharSequence): Unit = {
      chars = str
    }

    override def startNonterminal(name: String, begin: Int): Unit = {
      tree.addStartElement(new QName("", name))
    }

    override def endNonterminal(name: String, end: Int): Unit = {
      tree.addEndElement()
    }

    override def terminal(name: String, begin: Int, end: Int): Unit = {
      val tag = if (name(0) == '\'') "TOKEN" else name
      tree.addStartElement(new QName("", tag))
      tree.addText(chars.subSequence(begin, end).toString)
      tree.addEndElement()
    }

    override def whitespace(begin: Int, end: Int): Unit = {
      // nop
    }
  }
}
