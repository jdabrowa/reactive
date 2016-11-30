package auction.lab4

import java.time.LocalTime
import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, Cancellable}
import akka.persistence.fsm.PersistentFSM
import akka.persistence.fsm.PersistentFSM.FSMState
import auction.lab4.Auction._
import auction.lab4.AuctionSearch.RegisterAuction

import scala.concurrent.duration._
import scala.reflect.ClassTag

object Auction {

  // FSM States
  sealed trait State extends FSMState

  case object Created extends State {
    override def identifier: String = "created"
  }

  case object Activated extends State {
    override def identifier: String = "activated"
  }

  case object Sold extends State {
    override def identifier: String = "sold"
  }

  case object Ignored extends State {
    override def identifier: String = "ignored"
  }

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

  case class BidRejected(currentPrice: Integer)

  case object AuctionWon

  // Self-to-Self events
  sealed trait SelfNotfiy

  case object BidTimerExpired

  case object DeleteTimerExpired

  case class NotifyTTL(remainingMillis: Long)

  // FSM Persistence
  class AuctionEvent

  case class SuccessfulBidEvent(bidder: ActorRef, newPrice: Integer) extends AuctionEvent

  case class IgnoredEvent() extends AuctionEvent

  case class SoldEvent() extends AuctionEvent

  case class AuctionRelistedEvent() extends AuctionEvent

  case class TTLNotifyEvent(remainingMillis: Long) extends AuctionEvent

}

class Auction {
  private var _bidTimerMessage: Cancellable = _

  def bidTimerMessage = _bidTimerMessage

  def bidTimerMessage_=(m: Cancellable): Unit = _bidTimerMessage = m

  private var _deleteTimerMessage: Cancellable = _

  def deleteTimerMessage = _deleteTimerMessage

  def deleteTimerMessage_=(m: Cancellable): Unit = _deleteTimerMessage = m

}

class FSMAuction(seller: ActorRef, startingPrice: Int, durationSeconds: Int, description: String)(implicit val domainEventClassTag: ClassTag[AuctionEvent])
  extends Auction with PersistentFSM[State, AuctionDetails, AuctionEvent] {

  startWith(Created, InitialState(startingPrice))

  override def persistenceId = s"persistent-auction-fsm-${self.path.name}"

  val system = context.system

  import system.dispatcher

  context.actorSelection("/user/auctionSearch") ! RegisterAuction(description)
  self ! NotifyTTL(TimeUnit.SECONDS.toMillis(durationSeconds))
  private val endTime = System.currentTimeMillis() + durationSeconds * 1000

  when(Created) {
    case (Event(BidTimerExpired, _)) => {
      //      log("a")
      goto(Ignored) applying IgnoredEvent()
    }
    case (Event(Bid(bidder, offeredPrice), InitialState(initialPrice))) if offeredPrice >= initialPrice => {
      log(s"Accepting offer for $offeredPrice from ${bidder.path.name}")
      bidder ! BidSuccessful
      goto(Activated) applying SuccessfulBidEvent(bidder, offeredPrice)
    }
    case (Event(Bid(bidder, offeredPrice), InitialState(initialPrice))) => {
      //      log("c")
      bidder ! BidRejected(initialPrice)
      stay
    }
  }

  when(Activated) {
    case (Event(Bid(newBidder, offeredPrice), Bidded(previousBidder, currentPrice))) if offeredPrice > currentPrice => {
      //      log("d")
      previousBidder ! BidRejected(offeredPrice)
      stay applying SuccessfulBidEvent(newBidder, offeredPrice) replying BidSuccessful
    }
    case (Event(Bid(bidder, offeredPrice), Bidded(_, currentPrice))) if offeredPrice <= currentPrice => {
      bidder ! BidRejected(currentPrice)
      stay
    }
    case (Event(BidTimerExpired, Bidded(winner, finalOffer))) => {
      //      log("e")
      winner ! AuctionWon
      seller ! ItemSold(winner, finalOffer)
      goto(Sold) applying SoldEvent()
    }
  }

  when(Ignored) {
    case (Event(DeleteTimerExpired, _)) => {
      log(self.path.name + " completed: no bid")
      stop
    }
    case (Event(Relist, _)) => {
      log(self.path.name + " relisting")
      goto(Created) applying AuctionRelistedEvent()
    }
  }

  when(Sold) {
    case (Event(DeleteTimerExpired, Bidded(winner, price))) => {
      log(self.path.name + " completed: highest bid is " + price + " from " + winner.path.name)
      stop
    }
    case (Event(Bid(bidder, _), _)) => {
      log("Rejecting offer (item already sold)")
      stay
    }
  }

  whenUnhandled {
    case (Event(Bid(bidder, _), InitialState(d))) => {
      //      log("t")
      bidder ! BidRejected(d)
      stay
    }
    case Event(NotifyTTL(millis), _) => {
      //      log("r")
      stay applying TTLNotifyEvent(millis)
    }
    case _ => {
      log("def!!!")
      stay
    }
  }

  override def applyEvent(domainEvent: AuctionEvent, currentData: AuctionDetails): AuctionDetails = {
    domainEvent match {
      case SuccessfulBidEvent(bidder, offer) => {
        //        log("h")
        Bidded(bidder, offer)
      }
      case IgnoredEvent() => {
        //        log("m")
        deleteTimerMessage = system.scheduler.scheduleOnce(durationSeconds seconds, self, DeleteTimerExpired)
        currentData
      }
      case SoldEvent() => {
        //        log("n")
        deleteTimerMessage = system.scheduler.scheduleOnce(durationSeconds seconds, self, DeleteTimerExpired)
        currentData
      }
      case AuctionRelistedEvent() => {
        //        log("y")
        deleteTimerMessage.cancel()
        InitialState(startingPrice)
      }
      case TTLNotifyEvent(millis) => {
        if (bidTimerMessage != null) bidTimerMessage.cancel()
        log(s"Replaying TTL with $millis")
        bidTimerMessage = system.scheduler.scheduleOnce(millis milliseconds, self, BidTimerExpired)
        currentData
      }
    }
  }

  override def postStop() = {
    stay() applying TTLNotifyEvent(endTime - System.currentTimeMillis())
  }

  def log(msg: String): Unit = {
    println(" [" + LocalTime.now() + "] " + self.path.name + " > " + msg)
  }
}