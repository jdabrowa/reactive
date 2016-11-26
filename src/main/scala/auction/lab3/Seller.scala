package auction.lab3

import akka.actor.{Actor, ActorRef, Props}
import auction.lab2.Auction.ItemSold

import scala.collection.mutable
import scala.util.Random

object Seller {

}

class Seller(names: List[(String, String)]) extends Actor {

  val system = context.system
  var auctions = mutable.Map.empty[ActorRef, String]

  for(auctionName <- names) {
    val actor: ActorRef = system.actorOf(Props(new FSMAuction(self, 1 + Random.nextInt(3), 5 + Random.nextInt(5), auctionName._1)), auctionName._2)
    auctions += (actor -> auctionName._1)
  }

  override def receive: Receive = {
    case ItemSold => {
      println(s"Item ${auctions(sender)} sold")
    }
  }
}