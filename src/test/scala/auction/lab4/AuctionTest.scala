package auction.lab4

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestFSMRef, TestKit, TestProbe}
import auction.lab4.Auction._
import org.scalatest.{BeforeAndAfter, WordSpecLike}

class AuctionTest extends TestKit(ActorSystem("SearchSpec")) with WordSpecLike with ImplicitSender with BeforeAndAfter {

  var seller: TestProbe = _
  var buyer: TestProbe = _

  "Auction" must {

    before {
      seller = TestProbe("seller")
      buyer = TestProbe("buyer")
    }

    "accept offers if they meet criteria" in {

      val fsm = TestActorRef(new FSMAuction(seller.ref, 2, 3, "descr"))
      fsm ! Auction.Bid(buyer.ref, 1)
      buyer.expectMsgPF() {
        case a => println(a)
          true
      }

      fsm ! Auction.Bid(buyer.ref, 2)
      buyer.expectMsg(BidSuccessful)

      fsm ! Auction.Bid(buyer.ref, 5)
      buyer.expectMsg(BidSuccessful)

      fsm ! Auction.Bid(buyer.ref, 5)
      buyer.expectMsg(BidRejected)
    }

    "finish after specified time without offer and respond to Relist" in {
      val fsm = TestActorRef(new FSMAuction(seller.ref, 2, 1, "descr"))
      TimeUnit.MILLISECONDS.sleep(1100L)

      fsm ! Relist
    }

    "finish after specified time with offer and notify seller and winner" in {
      val fsm = TestActorRef(new FSMAuction(seller.ref, 2, 1, "descr"))
      fsm ! Auction.Bid(buyer.ref, 5)
      buyer.expectMsg(BidSuccessful)

      TimeUnit.MILLISECONDS.sleep(1100L)

      buyer.expectMsg(AuctionWon)
      assert(seller.expectMsgPF() {
        case ItemSold(winner, finalPrice) => winner == buyer.ref && finalPrice == 5
      })
    }

  }

}
