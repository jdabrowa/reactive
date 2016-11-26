package auction.lab3

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestFSMRef, TestKit, TestProbe}
import auction.lab3.Auction._
import org.scalatest.{BeforeAndAfter, WordSpecLike}

class AuctionTest extends TestKit(ActorSystem("SearchSpec")) with WordSpecLike with ImplicitSender with BeforeAndAfter {

  var seller: TestProbe = _
  var buyer: TestProbe = _

  "Auction" must {

    before {
      seller = TestProbe("seller")
      buyer = TestProbe("buyer")
    }

    "initially be in 'created' state" in {
      val fsm = TestFSMRef(new FSMAuction(buyer.ref, 2, 3, "descr"))
      assert(fsm.stateName == Auction.Created)
      assert(fsm.stateData == Auction.InitialState(2))
    }

    "accept offers if they meet criteria" in {

      val fsm = TestFSMRef(new FSMAuction(buyer.ref, 2, 3, "descr"))
      fsm ! Auction.Bid(buyer.ref, 1)
      buyer.expectMsg(BidRejected)
      assert(fsm.stateName == Auction.Created)
      assert(fsm.stateData == Auction.InitialState(2))

      fsm ! Auction.Bid(buyer.ref, 2)
      buyer.expectMsg(BidSuccessful)
      assert(fsm.stateName == Auction.Activated)
      assert(fsm.stateData == Auction.Bidded(buyer.ref, 2))

      fsm ! Auction.Bid(buyer.ref, 5)
      buyer.expectMsg(BidSuccessful)
      assert(fsm.stateName == Auction.Activated)
      assert(fsm.stateData == Auction.Bidded(buyer.ref, 5))

      fsm ! Auction.Bid(buyer.ref, 5)
      buyer.expectMsg(BidRejected)
    }

    "finish after specified time without offer and respond to Relist" in {
      val fsm = TestFSMRef(new FSMAuction(seller.ref, 2, 1, "descr"))
      TimeUnit.MILLISECONDS.sleep(1100L)
      assert(fsm.stateName == Auction.Ignored)

      fsm ! Relist
      assert(fsm.stateName == Auction.Created)
    }

    "finish after specified time with offer and notify seller and winner" in {
      val fsm = TestFSMRef(new FSMAuction(seller.ref, 2, 1, "descr"))
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
