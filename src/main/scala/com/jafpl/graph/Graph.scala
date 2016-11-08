package com.jafpl.graph

import akka.actor.{ActorRef, ActorSystem, Props}
import com.jafpl.graph.GraphMonitor.{GDump, GRun, GTrace}
import com.jafpl.runtime._
import com.jafpl.util.{UniqueId, XmlWriter}
import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer
import scala.collection.{immutable, mutable}

/**
  * Created by ndw on 10/2/16.
  */
class Graph() {
  protected val logger = LoggerFactory.getLogger(this.getClass)
  private val _nodes = mutable.HashSet.empty[Node]
  private val fans = mutable.HashMap.empty[Node, Node]
  private val edges = mutable.HashSet.empty[Edge]
  private var except: Option[Throwable] = None
  private var exceptNode: Option[Node] = None
  private var _validated = false
  private var _valid = true
  private var _finished = false
  private var _system: ActorSystem = _
  private var _monitor: ActorRef = _

  private[graph] def finished = _finished
  private[graph] def system = _system
  private[jafpl] def monitor = _monitor
  private[graph] def nodes = _nodes.toSet

  def exception = except
  def exceptionNode = exceptNode

  def chkValid() = {
    if (_validated) {
      throw new GraphException("Attempt to change validated graph")
    }
  }

  def createNode(): Node = {
    createNode(None)
  }

  def createNode(name: String): Node = {
    createNode(Some(name))
  }

  def runtime: GraphRuntime = {
    if (!valid()) {
      throw new GraphException("Attempt to get runtime for invalid graph")
    }
    new Runtime(this)
  }

  private def createNode(name: Option[String]): Node = {
    chkValid()
    val node = new Node(this, None)
    node.label = name
    _nodes.add(node)
    node
  }

  def createNode(step: Step): Node = {
    chkValid()
    val node = new Node(this, Some(step))
    _nodes.add(node)
    node
  }

  def createInputNode(name: String): InputNode = {
    chkValid()
    val node = new InputNode(this, name)
    _nodes.add(node)
    node
  }

  def createOutputNode(name: String): OutputNode = {
    chkValid()
    val node = new OutputNode(this, name)
    _nodes.add(node)
    node
  }

  def createVariableNode(step: Step): Node = {
    chkValid()
    val node = new Node(this, Some(step))
    _nodes.add(node)
    node
  }

  def createIteratorNode(subpipeline: List[Node]): LoopStart = {
    createIteratorNode(None, subpipeline)
  }

  def createIteratorNode(step: CompoundStep, subpipeline: List[Node]): LoopStart = {
    createIteratorNode(Some(step), subpipeline)
  }

  private def createIteratorNode(step: Option[CompoundStep], subpipeline: List[Node]): LoopStart = {
    chkValid()
    val loopStart = new LoopStart(this, step, subpipeline)
    val loopEnd   = new LoopEnd(this, step)

    loopStart.compoundEnd = loopEnd
    loopEnd.compoundStart = loopStart

    _nodes.add(loopStart)
    _nodes.add(loopEnd)

    loopStart
  }

  private[graph] def createIterationCacheNode(): IterationCache = {
    val node = new IterationCache(this)
    _nodes.add(node)
    node
  }

  def createChooseNode(subpipeline: List[Node]): ChooseStart = {
    createChooseNode(Some(new Chooser()), subpipeline)
  }

  private def createChooseNode(step: Option[CompoundStep], subpipeline: List[Node]): ChooseStart = {
    chkValid()
    val chooseStart = new ChooseStart(this, step, subpipeline)
    val chooseEnd   = new ChooseEnd(this, step)

    chooseStart.compoundEnd = chooseEnd
    chooseEnd.compoundStart = chooseStart

    _nodes.add(chooseStart)
    _nodes.add(chooseEnd)

    chooseStart
  }

  def createWhenNode(subpipeline: List[Node]): WhenStart = {
    createWhenNode(Some(new WhenTrue()), subpipeline)
  }

  def createWhenNode(step: WhenStep, subpipeline: List[Node]): WhenStart = {
    createWhenNode(Some(step), subpipeline)
  }

  private def createWhenNode(step: Option[WhenStep], subpipeline: List[Node]): WhenStart = {
    chkValid()

    val whenStep = if (step.isDefined) {
      Some(new Whener(step.get))
    } else {
      step
    }

    val whenStart = new WhenStart(this, whenStep, subpipeline)
    val whenEnd   = new WhenEnd(this, whenStep)

    whenStart.compoundEnd = whenEnd
    whenEnd.compoundStart = whenStart

    _nodes.add(whenStart)
    _nodes.add(whenEnd)

    whenStart
  }

  def createGroupNode(subpipeline: List[Node]): GroupStart = {
    chkValid()
    val step       = Option(new Grouper())
    val groupStart = new GroupStart(this, step, subpipeline)
    val groupEnd   = new GroupEnd(this, step)

    groupStart.compoundEnd = groupEnd
    groupEnd.compoundStart = groupStart

    _nodes.add(groupStart)
    _nodes.add(groupEnd)

    var count = 0
    for (node <- subpipeline) {
      count += 1
      addEdge(groupStart, "!latch_" + count, node, "!latch")
    }

    groupStart
  }

  def getSourceEdge(source: Node, outputPort: String): Option[Edge] = {
    for (edge <- edges) {
      if (edge.source == source && edge.outputPort == outputPort) {
        return Some(edge)
      }
    }
    None
  }

  def addEdge(from: Port, to: Port): Unit = {
    chkValid()
    addEdge(from.node, from.name, to.node, to.name)
  }

  def addEdge(source: Node, outputPort: String, destination: Node, inputPort: String): Unit = {
    chkValid()

    //logger.info("addEdge: " + source + "." + outputPort + " -> " + destination + "." + inputPort)

    val from =
      if (source.output(outputPort).isDefined) {
        val edge = source.output(outputPort).get

        if (fans.contains(edge.source)) {
          val fanOut = fans(edge.source).asInstanceOf[FanOut]
          fanOut.nextPort
        } else {
          val fanOut = new FanOut(this)
          _nodes.add(fanOut)
          fans.put(edge.source, fanOut)

          removeEdge(edge)
          addEdge(source, outputPort, fanOut, "source")

          val targetPort = new Port(edge.destination, edge.inputPort)
          addEdge(fanOut.nextPort, targetPort)
          fanOut.nextPort
        }
    } else {
        new Port(source, outputPort)
      }
    val to =
      if (destination.input(inputPort).isDefined) {
        val edge = destination.input(inputPort).get

        if (fans.contains(edge.destination)) {
          val fanIn = fans(edge.destination).asInstanceOf[FanIn]
          fanIn.nextPort
        } else {
          val fanIn = new FanIn(this)
          _nodes.add(fanIn)
          fans.put(edge.destination, fanIn)
          edge.source.removeOutput(edge.outputPort)
          edge.destination.removeInput(edge.inputPort)
          edges.remove(edge)
          addEdge(fanIn, "result", destination, inputPort)
          val sourcePort = new Port(edge.source, edge.outputPort)
          addEdge(sourcePort, fanIn.nextPort)
          fanIn.nextPort
        }
      } else {
        new Port(destination, inputPort)
      }

    val edge = new Edge(this, from, to)

    //logger.info("         " + from + " -> " + to)

    edges.add(edge)
  }

  private[graph] def removeEdge(edge: Edge): Unit = {
    //logger.info("rmvEdge: " + edge.source + "." + edge.outputPort + " -> " + edge.destination + "." + edge.inputPort)

    edge.source.removeOutput(edge.outputPort)
    edge.destination.removeInput(edge.inputPort)
    edges.remove(edge)
  }

  def addDependency(node: Node, dependsOn: Node): Unit = {
    chkValid()
    node.addDependancy(dependsOn)
  }

  private[graph] def finish(): Unit = {
    _finished = true
  }

  private[graph] def abort(srcNode: Option[Node], exception: Throwable): Unit = {
    _finished = true
    except = Some(exception)
    exceptNode = srcNode
  }

  def valid(): Boolean = {
    if (_validated) {
      return _valid
    }

    val srcPorts = mutable.HashSet.empty[Port]
    val dstPorts = mutable.HashSet.empty[Port]
    for (edge <- edges) {
      val src = new Port(edge.source, edge.outputPort)
      val dst = new Port(edge.destination, edge.inputPort)

      srcPorts += src
      dstPorts += dst

      if (srcPorts.contains(dst)) {
        _valid = false
        throw new GraphException("Attempt to write to an input port: " + dst)
      }

      if (dstPorts.contains(src)) {
        _valid = false
        throw new GraphException("Attempt to read to an output port: " + dst)
      }
    }

    for (node <- _nodes) {
      _valid = _valid && node.valid
      _valid = _valid && node.noCycles(immutable.HashSet.empty[Node])
      _valid = _valid && node.connected()
    }

    if (_valid) {
      for (node <- _nodes) {
        node.addIterationCaches()
        node.addWhenCaches()
        node.addChooseCaches()
      }
    }

    _validated = true
    _valid
  }

  private def roots(): Set[Node] = {
    val roots = mutable.HashSet.empty[Node]
    for (node <- _nodes) {
      if (node.inputs.isEmpty) {
        roots.add(node)
      }
    }
    roots.toSet
  }

  private[graph] def inputs(): List[InputNode] = {
    val inodes = mutable.ListBuffer.empty[InputNode]
    for (node <- _nodes) {
      node match {
        case n: InputNode => inodes += n
        case _ => Unit
      }
    }
    inodes.toList
  }

  private[graph] def outputs(): List[OutputNode] = {
    val onodes = mutable.ListBuffer.empty[OutputNode]
    for (node <- _nodes) {
      node match {
        case n: OutputNode => onodes += n
        case _ => Unit
      }
    }
    onodes.toList
  }

  private[graph] def makeActors(): Unit = {
    _system = ActorSystem("jafpl-com-" + UniqueId.nextId)
    _monitor = _system.actorOf(Props(new GraphMonitor(this)), name = "monitor")

    val trace = Option(System.getProperty("com.xmlcalabash.trace"))
    _monitor ! GTrace(trace.isDefined && List("true", "yes", "1").contains(trace.get))

    for (node <- _nodes) {
      node.makeActors()
    }
  }

  private[graph] def run() {
    monitor ! GRun()
  }

  private [graph] def reset(): Unit = {
    for (node <- _nodes) {
      node.reset()
    }
    _finished = false
    except = None
    exceptNode = None
  }

  private [graph] def teardown(): Unit = {
    for (node <- _nodes) {
      node match {
        case end: CompoundEnd => Unit
        case other: Node => other.teardown()
      }
    }
    _finished = true
    except = None
    exceptNode = None
  }

  def trace(enable: Boolean): Unit = {
    _monitor ! GTrace(enable)
  }

  def status(): Unit = {
    _monitor ! GDump()
  }

  def topology(): List[String] = {
    val nodeIds = mutable.HashMap.empty[Node, String]
    val lines = ListBuffer.empty[String]
    var count = 0

    for (node <- nodes.toList.sortWith(_.uid < _.uid)) {
      val id = base26ish(count)
      nodeIds.put(node, id)
      count += 1
      lines += id
    }

    for (edge <- edges.toList.sortWith(sortEdge)) {
      val source = nodeIds(edge.source) + "." + edge.outputPort
      val target = nodeIds(edge.destination) + "." + edge.inputPort
      lines += source + " -> " + target
    }

    lines.toList
  }

  private def sortEdge(e1: Edge, e2:Edge) = {
    val e1suid = e1.source.uid
    val e2suid = e2.source.uid
    val e1duid = e1.destination.uid
    val e2duid = e2.destination.uid
    val e1ip = e1.inputPort
    val e1op = e1.outputPort
    val e2ip = e2.inputPort
    val e2op = e2.outputPort

    val s1 = f"$e1suid%6d$e1duid%6d|$e1op|$e1ip"
    val s2 = f"$e2suid%6d$e2duid%6d|$e2op|$e2ip"

    s1 < s2
  }

  private def base26ish(integer: Int): String = {
    val letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    var result = ""
    var value = integer
    while (value >= 0) {
      val ch = value % 26
      result = letters(ch) + result
      value /= 26
      value -= 1
    }
    result
  }


  def dump(): String = {
    val tree = new XmlWriter()
    tree.startDocument()
    tree.addStartElement(Serializer.pg_graph)
    for (node <- _nodes) {
      node.dump(tree)
    }
    tree.addEndElement()
    tree.endDocument()
    tree.getResult
  }
}
