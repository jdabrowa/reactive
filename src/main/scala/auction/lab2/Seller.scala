package auction.lab2

import akka.actor.Actor
import auction.lab2.Auction.ItemSold

object Seller {

}

class Seller() extends Actor {

  val system = context.system

  override def receive: Receive = {
    case ItemSold(winner, finalPrice, name) =>
      println(s"Item $name sold to $winner for $finalPrice")
  }
}