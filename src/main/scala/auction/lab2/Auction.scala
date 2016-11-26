package auction.lab2

import java.time.LocalTime

import akka.actor.{Actor, ActorRef, Cancellable, FSM}
import auction.lab2.Auction._

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
  case class ItemSold(winner: ActorRef, finalPrice: Int, name: String)
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

  scheduledMessage = system.scheduler.scheduleOnce(durationSeconds seconds, self, BidTimerExpired)

  when(Created) {
    case(Event(BidTimerExpired, _)) => {
      scheduledMessage = system.scheduler.scheduleOnce(durationSeconds seconds, self, DeleteTimerExpired)
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
    println ("" + Thread.currentThread().getName() + " [" + LocalTime.now() + "] > " + msg)
  }

}

class NativeAkkaAuction(seller: ActorRef, startingPrice: Int, durationSeconds: Int, name: String) extends Auction with Actor {

  val system = context.system

  import system.dispatcher

  var currentPrice : Int = startingPrice
  var currentWinner : ActorRef = _

  system.scheduler.scheduleOnce(durationSeconds seconds, self, BidTimerExpired)

  // created
  override def receive: Receive = {
    case Bid(bidder, offer) if offer >= currentPrice => {
      currentPrice = offer
      currentWinner = bidder
      bidder ! BidSuccessful
      context become activated
    }
    case Bid(bidder, _) => {
      bidder ! BidRejected
    }
    case BidTimerExpired => {
      scheduledMessage = system.scheduler.scheduleOnce(5 seconds, sender(), DeleteTimerExpired)
      context become ignored
    }
  }

  def activated: Receive = {
    case Bid(bidder, offer) if offer > currentPrice => {
      currentPrice = offer
      currentWinner = bidder
      bidder ! BidSuccessful
    }
    case Bid(bidder, _) => {
      bidder ! BidRejected
    }
    case BidTimerExpired => {
      seller ! ItemSold(currentWinner, currentPrice, name)
      currentWinner ! AuctionWon
      scheduledMessage = system.scheduler.scheduleOnce(durationSeconds seconds, self, DeleteTimerExpired)
      context become sold
    }
  }

  def ignored: Receive = {
    case DeleteTimerExpired => {
      context.stop(context.self)
    }
    case Relist => {
      scheduledMessage.cancel
      system.scheduler.scheduleOnce(durationSeconds seconds, self, BidTimerExpired)
    }
  }

  def sold: Receive = {
    case DeleteTimerExpired => {
      context stop self
    }
  }
}