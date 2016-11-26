package auction.lab2

import akka.actor.{ActorRef, ActorSystem, Props}

import scala.util.Random

object Main extends App {

  var system = ActorSystem("system")

  runLab2

  def runLab2: Unit = {

    val seller: ActorRef = system.actorOf(Props[Seller])
    val items = List("cat", "doll", "rug", "ball", "car") map { createActor(_, seller) }

    for(item <- items; count <- 1 to Random.nextInt(4)) {
      system.actorOf(Props(new Buyer(item)), s"${item.path.name}_buyer_$count")
    }
  }

  def createActor(name: String, seller: ActorRef): ActorRef = {
    system.actorOf(Props(new NativeAkkaAuction(seller, 1 + Random.nextInt(3), 5 + Random.nextInt(5), name)), name)
  }
}