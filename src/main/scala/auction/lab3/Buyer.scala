package auction.lab3

import java.time.LocalTime
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef}
import auction.lab3.Auction.{AuctionWon, BidRejected}
import auction.lab3.AuctionSearch.{Query, SearchResult}

import scala.collection.mutable
import scala.util.Random

class Buyer(keyword: String) extends Actor {

  var auctions: mutable.MutableList[ActorRef] = mutable.MutableList.empty[ActorRef]

  var lastPrices: mutable.Map[ActorRef, Integer] = mutable.Map.empty[ActorRef, Integer]
  def multiplier: Int = 2 + Random.nextInt(3)
  def maxPrice = 15 + Random.nextInt(100)

  TimeUnit.MILLISECONDS.sleep(Random.nextInt(1000))
  context.actorSelection("../auctionSearch") ! Query(keyword)

  override def receive: Receive = {
    case SearchResult(auctionList) => {
      for(auction <- auctionList) {
        val price = 1 + Random.nextInt(3)
        lastPrices += (auction -> price)
        auctions :+ auction
        bid(auction)
      }
    }
    case(BidRejected) => {
      val lastPrice = lastPrices(sender)
      val newPrice = lastPrice * multiplier
      if (newPrice <= maxPrice) {
        log("Bid rejected, retrying with " + newPrice)
        lastPrices(sender) = newPrice
        bid(sender)
      } else {
        log("Bid rejected, cannot offer more. Fold.")
      }
    }

    case(AuctionWon) => {
      log("Hurray! I bought " + sender.path.name + " for " + lastPrices(sender))
    }
  }


  def bid(auction: ActorRef): Unit = {
    TimeUnit.MILLISECONDS.sleep(Random.nextInt(1000))
    log("bidding auction " + auction.path.name + " with price " + lastPrices(auction))

    auction ! Auction.Bid(self, lastPrices(auction))
  }

  def log(msg: String): Unit = {
    println ("" + Thread.currentThread().getName() + " [" + LocalTime.now().toString + "] " + self.path.name + " > " + msg)
  }
}
