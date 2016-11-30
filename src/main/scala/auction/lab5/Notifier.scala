package auction.lab5

import java.time.LocalTime
import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import auction.lab5.Notifier.{HealthCheck, HealthCheckResponse, Notify}

import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration

object Notifier {
  sealed case class Notify(auctionTitle: String, buyerName: String, currentPrice: Integer)
  case object HealthCheck
  case object HealthCheckResponse
}

class Notifier extends Actor {

  implicit val timeout = Timeout(5, TimeUnit.SECONDS)
  val publisherFuture = context.actorSelection("akka.tcp://AuctionPublisherSystem@127.0.0.1:2552/user/publisher").resolveOne(FiniteDuration(5, "seconds"))
  val publisher = Await.result(publisherFuture, timeout.duration)

  var future = publisher ! HealthCheck

  override def receive: Receive = {
    case Notify(title, buyer, price) => {
      publisher ! Notify(title, buyer, price)
    }
    case HealthCheckResponse => {
      log("Reply from remote node")
    }
  }

  def log(msg: String): Unit = {
    println(" [" + LocalTime.now() + "] " + self.path.name + " > " + msg)
  }
}