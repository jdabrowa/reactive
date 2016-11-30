package auction.lab5

import java.time.LocalTime

import akka.actor.Actor
import auction.lab5.Notifier.Notify

object Notifier {
  sealed case class Notify(auctionTitle: String, buyerName: String, currentPrice: Integer)
}

class Notifier extends Actor {

  val publisher = context.actorSelection("akka.tcp://AuctionPublisherSystem@jdabrowa.pl:2552/user/publisher")

  override def receive: Receive = {
    case Notify(title, buyer, price) => {
      publisher ! Notify(title, buyer, price)
    }
  }

  def log(msg: String): Unit = {
    println(" [" + LocalTime.now() + "] " + self.path.name + " > " + msg)
  }
}