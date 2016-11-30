package auction.lab5

import akka.actor.{Actor, ActorRef}
import auction.lab5.AuctionSearch.{Query, RegisterAuction, SearchResult}

import scala.collection.mutable

object AuctionSearch {
  case class RegisterAuction(name: String)
  case class Query(keyword: String)
  case class SearchResult(auctions: List[ActorRef])
}

class AuctionSearch() extends Actor {

  val auctions: mutable.Map[String, ActorRef] = mutable.Map.empty[String, ActorRef]

  override def receive: Receive = {
    case RegisterAuction(auctionName) => {
      auctions += (auctionName -> sender)
    }
    case Query(keyword: String) => {
      def auctionMatch(description: String, keyword: String): Boolean = { description.toLowerCase().contains(keyword.toLowerCase()) }
      val result: List[ActorRef] = auctions.keys.filter { auctionMatch(_, keyword) }.map{ auctions }.toList
      sender ! SearchResult(result)
    }
  }
}