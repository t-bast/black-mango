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

  def this() = this(ActorSystem("DeviceSpec"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  "A User Actor" should {
    "generates private key on demand" in {
      val probe = TestProbe()
      val user = system.actorOf(User.props("t-bast"))

      user.tell(User.GenerateKey(42), probe.ref)
      val response = probe.expectMsgType[User.PrivateKey]
      response.requestId should ===(42L)
      response.value should ===(None)
    }

    "stops after generating key" in {
      val probe = TestProbe()
      val user = system.actorOf(User.props("t-bast"))
      probe.watch(user)

      user.tell(User.GenerateKey(42), probe.ref)
      probe.expectMsgType[User.PrivateKey]
      probe.expectTerminated(user, 250.milliseconds)
    }

    "ignores unknown messages" in {
      val probe = TestProbe()
      val user = system.actorOf(User.props("t-bast"))

      user.tell("master key please?", probe.ref)
      probe.expectNoMessage(250.milliseconds)
    }
  }
}
