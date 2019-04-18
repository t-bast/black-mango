import akka.actor.ActorRef
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestProbe
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory
import it.unisa.dia.gas.plaf.jpbc.pairing.a.TypeACurveGenerator
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

class HttpServerSpec
  extends WordSpec
    with Matchers
    with ScalaFutures
    with ScalatestRouteTest
    with IbeRoutes {

  // We generate a sample pairing to create test curve points.
  // I haven't found a better way to easily instantiate the Element interface in tests.
  private val pairingParams = new TypeACurveGenerator(160, 512).generate()
  private val pairing = PairingFactory.getPairing(pairingParams)
  private val testPoint = pairing.getG1.newRandomElement().getImmutable

  private val probe = TestProbe()
  override val keyGenCenter: ActorRef = probe.ref
  lazy private val routes = ibeRoutes

  "IbeRoutes" should {
    "handle health keep-alive" in {
      val request = HttpRequest(uri = "/healthz")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[String] should ===("ok")
      }
    }

    "generate user private keys" in {
      val request = HttpRequest(uri = "/keys/t-bast/2019")
      val response = request ~> routes ~> runRoute

      val message = probe.expectMsgType[KeyGenCenter.GenerateKey]
      message.user should ===("t-bast")
      message.label should ===("2019")

      probe.lastSender ! User.PrivateKey(message.requestId, testPoint)

      check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[PrivateKey].key.length should !==(0)
      }(response)
    }

    "encrypt messages" in {
      val plaintext = Plaintext("Là, tout n'est qu'ordre et beauté,")
      val plaintextEntity = Marshal(plaintext).to[MessageEntity].futureValue

      val request = Post("/users/t-bast/poems/encrypt").withEntity(plaintextEntity)
      val response = request ~> routes ~> runRoute

      val message = probe.expectMsgType[KeyGenCenter.Encrypt]
      message.user should ===("t-bast")
      message.label should ===("poems")
      message.message should ===(plaintext.message)

      probe.lastSender ! User.EncryptedMessage(
        message.requestId,
        IBE.EncryptedPayload(testPoint, Array.fill(32)(42.byteValue)),
      )

      check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[Ciphertext].ciphertext should ===(Array.fill(32)(42.byteValue))
      }(response)
    }

    "decrypt messages" in {
      val ciphertext = Ciphertext(Array.fill(16)(24.byteValue), Array.fill(32)(42.byteValue))
      val ciphertextEntity = Marshal(ciphertext).to[MessageEntity].futureValue

      val request = Post("/users/t-bast/poems/decrypt").withEntity(ciphertextEntity)
      val response = request ~> routes ~> runRoute

      val message = probe.expectMsgType[KeyGenCenter.Decrypt]
      message.user should ===("t-bast")
      message.label should ===("poems")
      message.commit should ===(ciphertext.commit)
      message.ciphertext should ===(ciphertext.ciphertext)

      probe.lastSender ! User.DecryptedMessage(
        message.requestId,
        "Luxe, calme et volupté.",
      )

      check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[Plaintext].message should ===("Luxe, calme et volupté.")
      }(response)
    }
  }
}
