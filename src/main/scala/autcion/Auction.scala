package autcion

import java.time.LocalDateTime

import akka.actor.{ActorRef, FSM}
import autcion.Auction._

import java.lang.Thread

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

// TODO: make actor and timer event stateful with regard of version, to make sure we're reacting to right events
// ToDo: Or maybe cancel scheduled job?

class Auction(seller: ActorRef, startingPrice: Int, durationSeconds: Int) extends FSM[State, AuctionDetails] {

  startWith(Created, InitialState(startingPrice))
  val system = akka.actor.ActorSystem("system")
  import system.dispatcher
  system.scheduler.scheduleOnce(durationSeconds seconds, self, BidTimerExpired)

  when(Created) {
    case(Event(BidTimerExpired, _)) => {
      system.scheduler.scheduleOnce(durationSeconds seconds, self, DeleteTimerExpired)
      goto(Ignored)
    }
    case(Event(Bid(bidder, offeredPrice), InitialState(initialPrice))) if offeredPrice > initialPrice => {
      bidder ! BidSuccessful
      goto(Activated) using Bidded(bidder, offeredPrice)
    }
  }

  when(Activated) {
    case(Event(Bid(newBidder, offeredPrice), Bidded(previousBidder, currentPrice))) if offeredPrice > currentPrice => {
      newBidder ! BidSuccessful
      stay using Bidded(newBidder, offeredPrice)
    }
    case(Event(BidTimerExpired, Bidded(winner, finalOffer))) => {
      winner ! AuctionWon
      seller ! ItemSold
      system.scheduler.scheduleOnce(durationSeconds seconds, self, DeleteTimerExpired)
      goto(Sold)
    }
  }

  when(Ignored) {
    case(Event(DeleteTimerExpired, _)) => {
      log(self.path.name + " completed: no bid")
      stop
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
    println ("" + Thread.currentThread().getName + " [" + new LocalDateTime().toString + "] > " + msg)
  }

}