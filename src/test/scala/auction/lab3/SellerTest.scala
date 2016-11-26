package auction.lab3

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import auction.lab3.Seller.AuctionParams
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.collection.mutable

class SellerTest extends TestKit(ActorSystem("SearchSpec"))
  with WordSpecLike with BeforeAndAfterAll {

  "Seller" must {

    "spawn auctions for each description" in {

      val parents = mutable.MutableList.empty[ActorRef]
      val names = mutable.MutableList.empty[String]
      val descrs = mutable.MutableList.empty[String]

      def testChildMaker: (AuctionParams => ActorRef) =
        auctionParams => {
          parents += auctionParams.parent
          names += auctionParams.name
          descrs += auctionParams.description
          TestProbe("auction").ref
        }

      val descriptions: List[(String, String)] = List(("aa", "a"), ("bb", "b"))
      val seller = TestActorRef(Props(new Seller(descriptions, testChildMaker)), "sut")

      assert(parents.size == 2)
      assert(parents.toSet == Set(seller))
      assert(names.toSet == Set("a", "b"))
      assert(descrs.toSet == Set("aa", "bb"))
    }

  }

}
