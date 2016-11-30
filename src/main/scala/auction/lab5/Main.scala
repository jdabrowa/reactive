package auction.lab5

import akka.actor.{ActorSystem, Props}

import scala.util.Random

object Main extends App {

  var system = ActorSystem("system")

  runLab5

  def runLab5: Any = {
    val auctionNames = List(
      ("Opel Astra Igla Polecam", "opel"),
      ("HIT! Wyciskarka do czosnku z bluetooth", "wyciskarka"),
      ("Samochodzik zdalnie niby opel sterowany na bluetooth", "samochodzik")
    )

    system.actorOf(Props[AuctionSearch], "auctionSearch")
    system.actorOf(Props(new Seller(auctionNames, Seller.defaultChildMaker(system))), "seller")

    Thread.sleep(100L)

    for(keyword <- List("hit", "bluetooth", "opel")) {
      for(i <- 1 to 2 + Random.nextInt(3)) {
        system.actorOf(Props(new Buyer(keyword)), s"${keyword}_buyer_$i")
      }
    }
  }

}