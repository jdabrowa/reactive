package autcion

import java.time.{LocalDateTime, LocalTime}
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef}
import akka.actor.Actor.Receive
import autcion.Auction.{AuctionWon, BidRejected}

import scala.util.Random


class Buyer(auctions: List[ActorRef]) extends Actor {

  private var _auction: ActorRef = null
  def auction = _auction
  def auction_= (r: ActorRef): Unit = _auction = r
  var lastPrice: Int = 1 + Random.nextInt(3)
  def multiplier:Int = 2 + Random.nextInt(3)
  def maxPrice = 15 + Random.nextInt(100)

  doWhatBuyerDoes()

  override def receive: Receive = {

    case(BidRejected) => {
      val newPrice = lastPrice * multiplier
      if (newPrice <= maxPrice) {
        log("Bid rejected, retrying with " + newPrice)
        lastPrice = newPrice
        bid()
      } else {
        log("Bid rejected, cannot offer more. Fold.")
      }
    }

    case(AuctionWon) => {
      log("Hurray! I bought " + auction.path.name + " for " + lastPrice)
    }

  }

  def doWhatBuyerDoes(): Unit = {

    TimeUnit.MILLISECONDS.sleep(Random.nextInt(1000))
    auction = auctions(Random.nextInt(auctions.size))
    bid()
  }

  def bid(): Unit = {
    TimeUnit.MILLISECONDS.sleep(Random.nextInt(1000))
    log("bidding auction " + auction.path.name + " with price " + lastPrice)

    auction ! Auction.Bid(self, lastPrice)
  }

  def log(msg: String): Unit = {
    println ("" + Thread.currentThread().getName() + " [" + LocalTime.now().toString + "] " + self.path.name + " > " + msg)
  }
}
