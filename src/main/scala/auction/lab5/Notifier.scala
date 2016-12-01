package auction.lab5

import java.time.LocalTime

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorRef, OneForOneStrategy, Props}
import auction.lab5.Notifier.{ChildRestarted, Execute, Notify}

object Notifier {
  sealed trait Notify
  sealed case class BidNotify(auctionTitle: String, buyerName: String, currentPrice: Integer) extends Notify
  sealed case class AuctionWonNotify(auctionTitle: String, buyerName: String, currentPrice: Integer) extends Notify
  case object NotifyReceived
  case object Execute
  sealed case class ChildRestarted(child: ActorRef)
}

class Notifier extends Actor {

  override def receive: Receive = {
    case notification: Notify => {
      val childRequest = context.actorOf(Props(new NotifierRequest(notification)))
      childRequest ! Execute
    }
    case ChildRestarted(child) => {
      child ! Execute
    }
  }

  override val supervisorStrategy = {
    OneForOneStrategy() {
      case e: Exception => {
        log(s"Got ${e.getClass.getCanonicalName}: restarting")
        Restart
      }
    }
  }

  def log(msg: String): Unit = {
    println(" [" + LocalTime.now() + "] " + self.path.name + " > " + msg)
  }
}