package com.jafpl.graph

import akka.actor.{ActorRef, Props}
import com.jafpl.graph.GraphMonitor.{GFinish, GWatch}
import com.jafpl.items.GenericItem
import com.jafpl.messages.{CloseMessage, ItemMessage, RanMessage}
import com.jafpl.runtime.{Step, StepController}
import com.jafpl.util.{UniqueId, XmlWriter}
import org.slf4j.LoggerFactory

import scala.collection.{Set, immutable, mutable}

/**
  * Created by ndw on 10/2/16.
  */
class Node(val graph: Graph, step: Option[Step]) extends StepController {
  protected val logger = LoggerFactory.getLogger(this.getClass)
  private val inputPort = mutable.HashMap.empty[String, Option[Edge]]
  private val outputPort = mutable.HashMap.empty[String, Option[Edge]]
  private val sequenceNos = mutable.HashMap.empty[String, Long]
  private val dependents = mutable.HashSet.empty[Node]
  private var constructionOk = true
  private val actors = mutable.HashMap.empty[String, ActorRef]
  protected var _actor: ActorRef = _
  protected var _finished = false
  val worker = step
  private[graph] var label: Option[String] = if (step.isDefined) {
    Some(step.get.label)
  } else {
    None
  }

  private[graph] val dependsOn = mutable.HashSet.empty[Node]
  private[graph] def actor = _actor

  val uid = UniqueId.nextId

  def inputs(): Set[String] = {
    inputPort.keySet
  }

  def input(port: String): Option[Edge] = {
    inputPort.getOrElse(port, None)
  }

  def outputs(): Set[String] = {
    outputPort.keySet
  }

  def output(port: String): Option[Edge] = {
    outputPort.getOrElse(port, None)
  }

  private[graph] def addInput(port: String, edge: Option[Edge]): Unit = {
    if (inputPort.getOrElse(port, None).isDefined) {
      constructionOk = false
      throw new GraphException("Input port '" + port + "' already in use")
    } else {
      inputPort.put(port, edge)
    }
  }

  private[graph] def addOutput(port: String, edge: Option[Edge]): Unit = {
    if (outputPort.getOrElse(port, None).isDefined) {
      constructionOk = false
      throw new GraphException("Output port '" + port + "' already in use")
    } else {
      outputPort.put(port, edge)
    }
  }

  private[graph] def removeInput(port: String): Unit = {
    if (inputPort.getOrElse(port, None).isDefined) {
      inputPort.remove(port)
    } else {
      constructionOk = false
      throw new GraphException("Attempt to remove unconnected input")
    }
  }

  def removeOutput(port: String): Unit = {
    if (outputPort.getOrElse(port, None).isDefined) {
      outputPort.remove(port)
    } else {
      constructionOk = false
      throw new GraphException("Attempt to remove unconnected output")
    }
  }

  def addDependancy(node: Node): Unit = {
    dependsOn.add(node)
    node.addDependent(this)
  }

  private def addDependent(node: Node): Unit = {
    dependents.add(node)
  }

  private[graph] def valid = constructionOk
  private[graph] def finished = _finished

  def noCycles(seen: immutable.HashSet[Node]): Boolean = {
    if (seen.contains(this)) {
      throw new GraphException("Graph contains a cycle!")
      false
    } else {
      var valid = true
      for (edge <- outputPort.values) {
        if (valid && edge.isDefined) {
          valid = valid && edge.get.destination.noCycles(seen + this)
        }
      }
      for (depends <- dependsOn) {
        valid = valid && depends.noCycles(seen + this)
      }
      valid
    }
  }

  def connected(): Boolean = {
    var valid = true
    for (port <- inputPort.keySet) {
      if (inputPort.get(port).isEmpty) {
        valid = false
        throw new GraphException("Unconnected input port")
      }
    }
    for (port <- outputPort.keySet) {
      if (outputPort.get(port).isEmpty) {
        valid = false
        throw new GraphException("Unconnected output port")
      }
    }
    valid
  }

  private[graph] def addIterationCaches(): Unit = {
    // nop
  }

  private[graph] def addWhenCaches(): Unit = {
    // nop
  }

  private[graph] def addChooseCaches(): Unit = {
    // nop
  }

  override def toString: String = {
    if (label.isDefined) {
      "[Node: " + label.get + " " + uid.toString + "]"
    } else {
      "[Node: " + uid.toString + "]"
    }
  }

  def receive(port: String, msg: ItemMessage): Unit = {
    if (worker.isDefined) {
      this match {
        case le: LoopEnd =>
          le.receiveOutput(port, msg)
        case _ => worker.get.receive(port, msg)
        }
    } else {
      logger.info("No worker: {}", this)
    }
  }

  def send(port: String, item: GenericItem): Unit = {
    if (outputPort.get(port).isDefined) {
      val edge = output(port).get
      val targetPort = edge.inputPort
      val targetNode = edge.destination

      var seqNo: Long = 1
      if (sequenceNos.get(port).isDefined) {
        seqNo = sequenceNos(port) + 1
      }
      sequenceNos.put(port, seqNo)

      val msg = new ItemMessage(targetPort, uid, seqNo, item)
      targetNode.actor ! msg
    } else {
      this match {
        case ls: CompoundStart =>
          ls.endNode.send(port, item)
        case _ =>
          logger.info(this + " writes to unknown output port: " + port)
      }
    }
  }

  def close(port: String): Unit = {
    val edge = outputPort(port).get
    val targetPort = edge.inputPort
    val targetNode = edge.destination
    val msg = new CloseMessage(targetPort)
    targetNode.actor ! msg
  }

  def tell(node: Node, msg: Any): Unit = {
    node.actor ! msg
  }

  private[graph] def reset() = {
    if (worker.isDefined) {
      worker.get.reset()
    }
  }

  def stop(): Unit = {
    for (port <- outputPort.keySet) {
      val edge = outputPort(port).get
      val targetPort = edge.inputPort
      val targetNode = edge.destination
      val msg = new CloseMessage(targetPort)
      targetNode.actor ! msg
    }

    for (rest <- dependents) {
      if (!rest.finished) {
        rest.actor ! new RanMessage(this)
      }
    }

    _finished = true
    graph.monitor ! GFinish(this)
  }

  def finish(): Unit = {
    _finished = true
    graph.monitor ! GFinish(this)
  }

  def finish(node: Node): Unit = {
    node.finish()
    node match {
      case s: CompoundStart =>
        for (node <- s.subpipeline) {
          node.finish()
        }
    }
  }

  private[graph] def run(): Unit = {
    if (worker.isEmpty) {
      logger.info("No worker: {}", this)
      return
    }

    worker.get.run()

    this match {
      case cs: CompoundStart => Unit
      case ce: CompoundEnd => Unit
      case _ =>
        stop()
    }
  }

  private[graph] def makeActors(): Unit = {
    var actorName = if (label.isDefined) {
      label.get + "_" + UniqueId.nextId
    } else {
      "unknown" + "_" + UniqueId.nextId
    }

    actorName = actorName.replace("{", "+OC")
    actorName = actorName.replace("}", "+CC")
    actorName = actorName.replace("$", "+DS")

    _actor = graph.system.actorOf(Props(new NodeActor(this)), actorName)

    // The steps that represent the end of a compound step are special.
    // The worker associated with them is irrelevant and they aren't watched.
    this match {
      case _: CompoundEnd => Unit
      case _ =>
        if (worker.isDefined) {
          worker.get.setup(this, inputs().toList, outputs().toList)
        }
        graph.monitor ! GWatch(this)
    }
  }

  def dumpExtraAttr(tree: XmlWriter): Unit = {
    // nop
  }

  def dump(tree: XmlWriter): Unit = {
    tree.addStartElement(Serializer.pg_node)
    if (label.isDefined) {
      tree.addAttribute(Serializer._name, label.get)
    }
    if (step.isDefined) {
      tree.addAttribute(Serializer._step, step.get.toString)
    }
    tree.addAttribute(Serializer._uid, uid.toString)
    dumpExtraAttr(tree)

    if (inputs().nonEmpty) {
      tree.addStartElement(Serializer.pg_inputs)
      for (portName <- inputs()) {
        val port = inputPort(portName)
        if (port.isDefined) {
          tree.addStartElement(Serializer.pg_in_edge)
          tree.addAttribute(Serializer._source, port.get.source.uid.toString)
          tree.addAttribute(Serializer._input_port, port.get.inputPort)
          tree.addAttribute(Serializer._output_port, port.get.outputPort)
          tree.addEndElement()
        }
      }
      tree.addEndElement()
    }

    if (outputs().nonEmpty) {
      tree.addStartElement(Serializer.pg_outputs)
      for (portName <- outputs()) {
        val port = outputPort(portName)
        if (port.isDefined) {
          tree.addStartElement(Serializer.pg_out_edge)
          tree.addAttribute(Serializer._destination, port.get.destination.uid.toString)
          tree.addAttribute(Serializer._input_port, port.get.inputPort)
          tree.addAttribute(Serializer._output_port, port.get.outputPort)
          tree.addEndElement()
        }
      }
      tree.addEndElement()
    }

    tree.addEndElement()
  }
}
