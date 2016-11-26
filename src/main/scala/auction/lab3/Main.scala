package auction.lab3

import akka.actor.{ActorRef, ActorSystem, Props}

object Main extends App {

  var system = ActorSystem("system")

  def runLab3: ActorRef = {
    val auctionNames = List(
      ("Audi A6 - Niemiec plakal jak sprzedawal", "audiA6"),
      ("Opel Astra Igla Polecam", "opel"),
      ("HIT! Wyciskarka do czosnku z bluetooth", "wyciskarka"),
      ("Samochodzik zdalnie sterowany na bluetooth", "samochodzik"),
      ("Blok rysunkowy format A6", "blok")
    )

    system.actorOf(Props[AuctionSearch], "auctionSearch")
    system.actorOf(Props(new Seller(auctionNames)), "seller")

    Thread.sleep(100L)

    system.actorOf(Props(new Buyer("a6")), "A6_buyer")
    system.actorOf(Props(new Buyer("bluetooth")), "Bluetoot_buyer")
    system.actorOf(Props(new Buyer("igla")), "Igla_buyer")
  }

}