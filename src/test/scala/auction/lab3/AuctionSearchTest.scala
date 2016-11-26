package auction.lab3

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import auction.lab3.AuctionSearch.{Query, RegisterAuction, SearchResult}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class AuctionSearchTest extends TestKit(ActorSystem("SearchSpec"))
  with WordSpecLike with BeforeAndAfterAll with ImplicitSender {

  "AuctionSearch must" must {

    "initially be empty" in {
      val actorRef = TestActorRef[AuctionSearch]
      assert(actorRef.underlyingActor.auctions isEmpty)
    }

    "store auctions" in {
      val actorRef = TestActorRef[AuctionSearch]
      actorRef.underlyingActor.receive(RegisterAuction("some auction description"))
      actorRef.underlyingActor.receive(RegisterAuction("other auction description"))
      assert(actorRef.underlyingActor.auctions.size == 2)
    }

    "return correct auctions" in {
      val actorRef = TestActorRef[AuctionSearch]
      actorRef ! RegisterAuction("some auction description")
      actorRef ! RegisterAuction("other auction description")
      actorRef ! Query("auction")
      expectMsgPF() {
        case SearchResult(auctions) => auctions.size == 2
      }
    }

  }

}
