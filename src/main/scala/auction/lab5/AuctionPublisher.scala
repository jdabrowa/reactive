package auction.lab5

import java.time.LocalTime

import akka.actor.Actor
import auction.lab5.Notifier.Notify

class AuctionPublisher extends Actor {

  override def receive: Receive = {
    case Notify(auctionTitle, buyerName, currentPrice) => {
      log(s"$buyerName now leads auction $auctionTitle for $currentPrice")
    }
  }

  def log(msg: String): Unit = {
    println (" [" + LocalTime.now().toString + "] " + self.path.name + " > " + msg)
  }
}
