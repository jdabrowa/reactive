package auction.lab5

import java.time.LocalTime

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object AuctionPublisherMain extends App{

  log("Initializing actor system...")
  private val config: Config = ConfigFactory.load
  val system = ActorSystem("AuctionPublisherSystem", config.getConfig("publisher").withFallback(config))
  system.actorOf(Props[AuctionPublisher], "publisher")
  log("System initialized")

  Await.result(system.whenTerminated, Duration.Inf)

  def log(msg: String): Unit = {
    println (" [" + LocalTime.now().toString + "] Main > " + msg)
  }
}
