package auction.lab5

import java.time.LocalTime

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import auction.lab5.Notifier.{ChildRestarted, Execute, Notify, NotifyReceived}

import scala.concurrent.Await
import scala.concurrent.duration._

class NotifierRequest(notify: Notify) extends Actor {

  implicit val timeout = Timeout(5 seconds)

  override def receive: Receive = {
    case Execute => execute()
    case r => log(s"Got message: $r")
  }

  def execute() = {
    val publisherFuture = context.actorSelection("akka.tcp://AuctionPublisherSystem@127.0.0.1:2552/user/publisher").resolveOne(timeout.duration)
    val publisher: ActorRef = Await.result(publisherFuture, timeout.duration)

    val reply = publisher ? notify

    val result = Await.result(reply, timeout.duration)
    result match {
      case NotifyReceived => ""
    }
  }


  @scala.throws[Exception](classOf[Exception])
  override def postRestart(reason: Throwable): Unit = {
    super.postRestart(reason)
    context.actorSelection("..") ! ChildRestarted(self)
  }

  def log(msg: String): Unit = {
    println(" [" + LocalTime.now() + "] " + self.path.name + " > " + msg)
  }
}
