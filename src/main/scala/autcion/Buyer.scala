package autcion

import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef}
import akka.actor.Actor.Receive
import autcion.Auction.{AuctionWon, BidRejected}

import scala.util.Random

object Buyer {


}

class Buyer(auctions: List[ActorRef]) extends Actor {

  var auction: ActorRef
  var lastPrice: Int = Random.nextInt(3)
  def multiplier:Int = 2 + Random.nextInt(3)
  def maxPrice = Random.nextInt(100)

  doWhatBuyerDoes()

  override def receive: Receive = {

    case(BidRejected) => {
      val newPrice = lastPrice * multiplier
      log("Offer rejected, retrying with " + newPrice)
      lastPrice = newPrice
      bid()
    }

    case(AuctionWon) => {
      log("Hurray! I bought " + self.path.name + " for " + lastPrice)
    }

  }

  def doWhatBuyerDoes(): Unit = {

    TimeUnit.SECONDS.sleep(Random.nextInt(10))
    auction = auctions(Random.nextInt(auctions.size))
    bid()
  }

  def bid(): Unit = {
    log(self.path.name + ": bidding auction" + auction.path.name + " with price " + lastPrice)
    auction ! Auction.Bid(self, lastPrice)
  }

  def log(msg: String): Unit = {
    println ("" + Thread.currentThread().getName + " [" + new LocalDateTime().toString + "] > " + msg)
  }
}
