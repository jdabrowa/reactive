package auction.lab2

import java.time.LocalTime
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef}
import auction.lab2.Auction.{AuctionWon, BidRejected, BidSuccessful}

import scala.util.Random

class Buyer(auction: ActorRef) extends Actor {

  var lastPrice: Integer = 1 + Random.nextInt(2)
  def multiplier: Int = 2 + Random.nextInt(3)
  def maxPrice = 15 + Random.nextInt(100)

  bid(lastPrice)

  override def receive: Receive = {
    case(BidRejected) => {
      val newPrice = lastPrice * multiplier
      if (newPrice <= maxPrice) {
        log("Bid rejected, retrying with " + newPrice)
        lastPrice = newPrice
        bid(newPrice)
      } else {
        log("Bid rejected, cannot offer more. Fold.")
      }
    }
    case(AuctionWon) => {
      log("Hurray! I bought " + sender.path.name + " for " + lastPrice)
    }
  }


  def bid(newPrice: Integer): Unit = {
    TimeUnit.MILLISECONDS.sleep(Random.nextInt(1000))
    log("Bidding auction " + auction.path.name + " with price " + lastPrice)

    auction ! Auction.Bid(self, newPrice)
  }

  def log(msg: String): Unit = {
    println ("" + Thread.currentThread().getName() + " [" + LocalTime.now().toString + "] " + self.path.name + " > " + msg)
  }
}
