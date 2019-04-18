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
      response.value.isZero should ===(false)

      val user = probe.lastSender
      user.path.name should ===("user-t-bast")
    }

    "forward encryption and decryption requests to a user actor" in {
      val probe = TestProbe()
      val keyGenCenter = system.actorOf(KeyGenCenter.props("ECorp"))

      keyGenCenter.tell(KeyGenCenter.Encrypt(42, "t-bast", "poems", "l'idéal"), probe.ref)
      val response = probe.expectMsgType[User.EncryptedMessage]
      response.requestId should ===(42L)
      response.ciphertext.randomCommitment.isZero should ===(false)
      response.ciphertext.ciphertext.length should !==(0)

      val user = probe.lastSender
      user.path.name should ===("user-t-bast")

      keyGenCenter.tell(KeyGenCenter.Decrypt(
        43,
        "t-bast",
        "poems",
        response.ciphertext.randomCommitment.toBytes,
        response.ciphertext.ciphertext,
      ), probe.ref)
      val decrypted = probe.expectMsgType[User.DecryptedMessage]
      decrypted.requestId should ===(43L)
      decrypted.message should ===("l'idéal")
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
