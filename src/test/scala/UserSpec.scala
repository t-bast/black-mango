import org.scalatest.{BeforeAndAfterAll, WordSpecLike, Matchers}
import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import scala.language.postfixOps
import scala.concurrent.duration._

class UserSpec(_system: ActorSystem)
  extends TestKit(_system)
    with Matchers
    with WordSpecLike
    with BeforeAndAfterAll {

  def this() = this(ActorSystem("UserSpec"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  "A User Actor" should {
    "generate private keys on demand" in {
      val probe = TestProbe()
      val user = system.actorOf(User.props("t-bast"))

      user.tell(User.GenerateKey(42, "2018"), probe.ref)
      val response1 = probe.expectMsgType[User.PrivateKey]
      response1.requestId should ===(42L)
      response1.value should ===(None)

      user.tell(User.GenerateKey(43, "2019"), probe.ref)
      val response2 = probe.expectMsgType[User.PrivateKey]
      response2.requestId should ===(43L)
      response2.value should ===(None)
    }

    "ignore unknown messages" in {
      val probe = TestProbe()
      val user = system.actorOf(User.props("t-bast"))

      user.tell("master key please?", probe.ref)
      probe.expectNoMessage(250.milliseconds)
    }
  }
}
