import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import akka.actor.{ActorSystem, PoisonPill}
import akka.testkit.{TestKit, TestProbe}

import scala.language.postfixOps

class KeyGenCenterSpec(_system: ActorSystem)
  extends TestKit(_system)
    with Matchers
    with WordSpecLike
    with BeforeAndAfterAll {

  def this() = this(ActorSystem("KeyGenCenterSpec"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  "A Key Generation Center Actor" should {
    "forward key generation request to a user actor" in {
      val probe = TestProbe()
      val keyGenCenter = system.actorOf(KeyGenCenter.props("ECorp"))

      keyGenCenter.tell(KeyGenCenter.GenerateKey(42, "t-bast", "2019"), probe.ref)
      val response = probe.expectMsgType[User.PrivateKey]
      response.requestId should ===(42L)
      response.value should !==(None)

      val user = probe.lastSender
      user.path.name should ===("user-t-bast")
    }

    "handle killed user actors" in {
      val probe = TestProbe()
      val keyGenCenter = system.actorOf(KeyGenCenter.props("ECorp"))

      keyGenCenter.tell(KeyGenCenter.GenerateKey(42, "t-bast", "laptop"), probe.ref)
      probe.expectMsgType[User.PrivateKey]

      val user = probe.lastSender
      probe.watch(user)
      user ! PoisonPill

      probe.expectTerminated(user)

      keyGenCenter.tell(KeyGenCenter.GenerateKey(42, "t-bast", "laptop"), probe.ref)
      probe.expectMsgType[User.PrivateKey]

      probe.lastSender should !==(user)
    }
  }
}
