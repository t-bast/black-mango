import akka.actor.ActorRef
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
  }
}
