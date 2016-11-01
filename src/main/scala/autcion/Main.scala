import akka.actor.{Actor, ActorSystem, Props}
import autcion.{Auction, Buyer}

import scala.util.Random

object Main extends App {

  var system = ActorSystem("system")

  var seller = system.actorOf(Props[A], "seller")

  var numAuctions = 4
  var numBidders = 2

  var auctions = (
    for(i <- 1 to numAuctions) yield {
      system.actorOf(Props(new Auction(seller, 1 + Random.nextInt(3), 5 + Random.nextInt(5))), "Auction" + i)
    }).toList

  for(i <- 1 to numBidders){
    system.actorOf(Props(new Buyer(auctions)), "Buyer" + i)
  }

  class A extends Actor {
    override def receive: Receive = {
      case(_) => {

      }
    }
  }


}