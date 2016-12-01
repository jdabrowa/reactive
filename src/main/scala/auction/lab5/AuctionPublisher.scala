package auction.lab5

import java.time.LocalTime

import akka.actor.Actor
import auction.lab5.Notifier.{AuctionWonNotify, Notify, NotifyReceived}

class AuctionPublisher extends Actor {

  log("Creating publisher")

  override def receive: Receive = {
    case Notify(auctionTitle, buyerName, currentPrice) => {
      log(s"$buyerName bidded auction $auctionTitle for $currentPrice")
      sender ! NotifyReceived
    }
    case AuctionWonNotify(auctionTitle, buyerName, currentPrice) => {
      log(s"$buyerName WON auction $auctionTitle for $currentPrice")
      sender ! NotifyReceived
    }
    case _ => {
      log("Default handler")
    }
  }

  def log(msg: String): Unit = {
    println (" [" + LocalTime.now().toString + "] " + self.path.name + " > " + msg)
  }
}
