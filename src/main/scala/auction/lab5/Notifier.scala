package auction.lab5

import java.time.LocalTime

import akka.actor.Actor

object Notifier {
  sealed case class Notify(auctionTitle: String, buyerName: String, currentPrice: Integer)
}

class Notifier extends Actor {

  override def receive: Receive = {
    ???
  }

  def log(msg: String): Unit = {
    println(" [" + LocalTime.now() + "] " + self.path.name + " > " + msg)
  }
}