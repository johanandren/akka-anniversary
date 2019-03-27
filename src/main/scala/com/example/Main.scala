package com.example

import java.util.concurrent.ThreadLocalRandom

import akka.actor.Timers
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.routing.ActorRefRoutee
import akka.routing.RoundRobinRoutingLogic
import akka.routing.Router

import scala.concurrent.duration._
import scala.io.StdIn

object Main extends App {

  val system = ActorSystem("anniversary")

  val collectingActor = system.actorOf(Props(new RecievingActor), "collector-1")

  val fanout1 = system.actorOf(
    Props(new FanOut(30, false, Props(new Chain(2, collectingActor)))),
    "fanout-1")

  val fanout2 = system.actorOf(
    Props(new FanOut(20, false,
      Props(new FanOut(10, true,
        Props(new RespondingActor))))),
    "fanout-2")

  val fanout3 = system.actorOf(
    Props(new FanOut(
      20,
      false,
      Props(new FanOut(
        10,
        false,
        Props(new RecievingActor))))),
    "fanout-3")

  val fanout4 = system.actorOf(Props(new FanOut(
    20,
    false,
    Props(new ForwardingActor(collectingActor)))), "fanout-4")


  val chain1 = system.actorOf(Props(new Chain(1, fanout1)), "chain-1")
  val chain2 = system.actorOf(Props(new Chain(1, fanout2)), "chain-2")

  val emit1 = system.actorOf(Props(new RandomEmit(chain1, 150.millis)), "emit-1")
  val emit2 = system.actorOf(Props(new RandomEmit(chain1, 230.millis)), "emit-2")
  val emit3 = system.actorOf(Props(new RandomEmit(chain2, 120.millis)), "emit-3")
  val emit4 = system.actorOf(Props(new RandomEmit(fanout3, 80.millis)), "emit-4")
  val emit5 = system.actorOf(Props(new RandomEmit(fanout4, 130.millis)), "emit-5")

  StdIn.readLine()
  system.terminate()
}

class RandomEmit(recipient: ActorRef, rate: FiniteDuration) extends Actor with Timers {

  timers.startPeriodicTimer("timer", "timer", rate)

  def receive: Receive = {
    case "timer" =>
      if (ThreadLocalRandom.current().nextBoolean()) recipient ! "msg"
    case msg =>
  }
}

class Chain(links: Int, endLink: ActorRef) extends Actor with Timers {

  def startChain(links: Int): ActorRef = {
    (0 to links).foldLeft(endLink) { case (previousLink, n) =>
      context.actorOf(Props(new ForwardingActor(previousLink)))
    }
  }

  val nextLink = startChain(links)

  def receive: Receive = {
    case "msg" => nextLink.forward("msg")
  }
}

class FanOut(n: Int, respondToParent: Boolean, to: Props) extends Actor {

  var router = {
    val routees = Vector.fill(5) {
      val r = context.actorOf(to)
      context watch r
      ActorRefRoutee(r)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  override def receive: Receive = {
    case "response" =>
      if (respondToParent) context.parent ! "response"
    case "msg" => router.route("msg", self)
  }
}

class ForwardingActor(next: ActorRef) extends Actor {
  def receive: Receive = {
    case "msg" => next ! "msg"
    case msg =>
  }
}



class RecievingActor extends Actor {
  def receive: Receive = Actor.ignoringBehavior
}

class RespondingActor extends Actor {
  def receive: Receive = {
    case "msg" =>
      sender() ! "response"
    case _ =>
  }
}