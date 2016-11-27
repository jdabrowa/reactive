package auction.lab4

import java.time.LocalTime

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import auction.lab4.Auction.ItemSold
import auction.lab4.Seller.AuctionParams

import scala.collection.mutable
import scala.util.Random

object Seller {
  sealed case class AuctionParams(parent: ActorRef, startingPrice: Integer, duration: Integer, name: String, description: String)
  def defaultChildMaker(actorSystem: ActorSystem): AuctionParams => ActorRef = {
    auctionParams => actorSystem.actorOf(Props(new FSMAuction(auctionParams.parent, auctionParams.startingPrice, auctionParams.duration, auctionParams.description)), auctionParams.name)
  }
}

class Seller(names: List[(String, String)], childMaker: AuctionParams => ActorRef) extends Actor {

  val system = context.system
  var auctions = mutable.Map.empty[ActorRef, String]

  for(auctionName <- names) {
    val startingPrice: Int = 1 + Random.nextInt(3)
    val auction: ActorRef = childMaker(AuctionParams(self, startingPrice, 5 + Random.nextInt(5), auctionName._2, auctionName._1))
    auctions += (auction -> auctionName._1)
    log(s"Created auction: ${auctionName._2} with price $startingPrice")
  }

  override def receive: Receive = {
    case ItemSold => {
      log(s"Item ${auctions(sender)} sold to ${sender.path.name}")
    }
  }
  def log(msg: String): Unit = {
    println (" [" + LocalTime.now().toString + "] " + self.path.name + " > " + msg)
  }
}