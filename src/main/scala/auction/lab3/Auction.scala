package auction.lab3

import java.time.LocalTime

import akka.actor.{ActorRef, Cancellable, FSM}
import auction.lab3.Auction._
import auction.lab3.AuctionSearch.RegisterAuction

import scala.concurrent.duration._

object Auction {

  // FSM States
  sealed trait State
  case object Created extends State
  case object Activated extends State
  case object Sold extends State
  case object Ignored extends State

  // FSM Data
  sealed trait AuctionDetails
  case class InitialState(startingPrice: Int) extends AuctionDetails
  case class Bidded(bidder: ActorRef, currentPrice: Int) extends AuctionDetails

  // Ingress events
  case class Bid(bidder: ActorRef, offer: Int)
  case object Relist

  // Egress events
  case class ItemSold(winner: ActorRef, finalPrice: Int)
  case object BidSuccessful
  case object BidRejected
  case object AuctionWon

  // Self-to-Self events
  sealed trait SelfNotfiy
  case object BidTimerExpired
  case object DeleteTimerExpired
}

class Auction {
  private var _scheduledMessage: Cancellable = null
  def scheduledMessage = _scheduledMessage
  def scheduledMessage_=(m: Cancellable): Unit =  _scheduledMessage = m

}

class FSMAuction(seller: ActorRef, startingPrice: Int, durationSeconds: Int, description: String) extends Auction with FSM[State, AuctionDetails] {

  val system = context.system

  import system.dispatcher

  startWith(Created, InitialState(startingPrice))

  context.actorSelection("/user/auctionSearch") ! RegisterAuction(description)

  scheduledMessage = system.scheduler.scheduleOnce(durationSeconds seconds, self, BidTimerExpired)

  when(Created) {
    case(Event(BidTimerExpired, _)) => {
      scheduledMessage = system.scheduler.scheduleOnce(durationSeconds seconds, self, DeleteTimerExpired)
      goto(Ignored)
    }
    case(Event(Bid(bidder, offeredPrice), InitialState(initialPrice))) if offeredPrice >= initialPrice => {
      bidder ! BidSuccessful
      goto(Activated) using Bidded(bidder, offeredPrice)
    }
    case(Event(Bid(bidder, offeredPrice), InitialState(initialPrice))) => {
      bidder ! BidRejected
      stay
    }
  }

  when(Activated) {
    case(Event(Bid(newBidder, offeredPrice), Bidded(previousBidder, currentPrice))) if offeredPrice > currentPrice => {
      newBidder ! BidSuccessful
      previousBidder ! BidRejected
      stay using Bidded(newBidder, offeredPrice)
    }
    case(Event(BidTimerExpired, Bidded(winner, finalOffer))) => {
      winner ! AuctionWon
      seller ! ItemSold(winner, finalOffer)
      scheduledMessage = system.scheduler.scheduleOnce(durationSeconds seconds, self, DeleteTimerExpired)
      goto(Sold)
    }
  }

  when(Ignored) {
    case(Event(DeleteTimerExpired, _)) => {
      log(self.path.name + " completed: no bid")
      stop
    }
    case(Event(Relist, _)) => {
      log(self.path.name + " relisting")
      scheduledMessage.cancel()
      goto(Created) using InitialState(startingPrice)
    }
  }

  when(Sold) {
    case(Event(DeleteTimerExpired, Bidded(winner, price))) => {
      log(self.path.name + " completed: highest bid is " + price + " from " + winner.path.name)
      stop
    }
  }

  whenUnhandled {
    case(Event(Bid(bidder, _), _)) => {
      bidder ! BidRejected
      stay
    }
  }

  def log(msg: String): Unit = {
    println ("" + Thread.currentThread().getName + " [" + LocalTime.now() + "] > " + msg)
  }

}