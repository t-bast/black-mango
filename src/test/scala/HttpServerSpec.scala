import akka.actor.ActorRef
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestProbe
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

class HttpServerSpec
  extends WordSpec
    with Matchers
    with ScalaFutures
    with ScalatestRouteTest
    with IbeRoutes {

  val probe = TestProbe()
  override val keyGenCenter: ActorRef = probe.ref
  lazy val routes = ibeRoutes

  "IbeRoutes" should {
    "handle health keep-alive" in {
      val request = HttpRequest(uri = "/healthz")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[String] should ===("ok")
      }
    }
  }
}
