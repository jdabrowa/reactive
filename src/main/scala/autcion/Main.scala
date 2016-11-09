import akka.actor.{ActorSystem, Props}
import autcion._

object Main extends App {

  var system = ActorSystem("system")

  val auctionNames = List(
    ("Audi A6 - Niemiec plakal jak sprzedawal", "audiA6"),
    ("Opel Astra Igla Polecam", "opel"),
    ("HIT! Wyciskarka do czosnku z bluetooth", "wyciskarka"),
    ("Samochodzik zdalnie sterowany na bluetooth", "samochodzik"),
    ("Blok rysunkowy format A6", "blok")
  )

  system.actorOf(Props[AuctionSearch], "auctionSearch")
  system.actorOf(Props(new Seller(auctionNames)), "seller")
}