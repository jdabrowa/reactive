package auction.lab5

import java.time.LocalTime

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object AuctionPublisherMain extends App{

  log("Initializing actor system...")
  val system = ActorSystem("AuctionPublisherSystem", ConfigFactory.load().getConfig("publisher"))
  system.actorOf(Props[AuctionPublisher])
  log("System initialized")

  Await.result(system.whenTerminated, Duration.Inf)

  def log(msg: String): Unit = {
    println (" [" + LocalTime.now().toString + "] Main > " + msg)
  }
}
