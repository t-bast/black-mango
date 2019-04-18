import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn
import spray.json.DefaultJsonProtocol

trait JsonSupport extends SprayJsonSupport {
  import DefaultJsonProtocol._

  case class PrivateKey(key: Array[Byte])
  case class Plaintext(message: String)
  case class Ciphertext(commit: Array[Byte], ciphertext: Array[Byte])

  implicit val keyJsonFormat = jsonFormat1(PrivateKey)
  implicit val plaintextJsonFormat = jsonFormat1(Plaintext)
  implicit val ciphertextJsonFormat = jsonFormat2(Ciphertext)
}

trait IbeRoutes extends JsonSupport {
  // These should be provided by the app.
  implicit def system: ActorSystem

  def keyGenCenter: ActorRef

  implicit lazy val timeout: Timeout = Timeout(5.seconds)

  lazy val ibeRoutes: Route =
    path("healthz") {
      get {
        complete("ok")
      }
    } ~ path("keys" / Segment / Segment) { (user: String, label: String) =>
      get {
        val key = (keyGenCenter ? KeyGenCenter.GenerateKey(0L, user, label)).mapTo[User.PrivateKey]
        onSuccess(key) { key =>
          val response = PrivateKey(key.value.toBytes)
          complete(response)
        }
      }
    } ~ pathPrefix("users" / Segment / Segment) { (user: String, label: String) =>
      path("encrypt") {
        post {
          entity(as[Plaintext]) { req =>
            val encrypted = (keyGenCenter ? KeyGenCenter.Encrypt(0L, user, label, req.message)).
              mapTo[User.EncryptedMessage]
            onSuccess(encrypted) { encrypted =>
              complete(Ciphertext(
                encrypted.ciphertext.randomCommitment.toBytes,
                encrypted.ciphertext.ciphertext
              ))
            }
          }
        }
      } ~ path("decrypt") {
        post {
          entity(as[Ciphertext]) { req =>
            val decrypted = (keyGenCenter ? KeyGenCenter.Decrypt(0L, user, label, req.commit, req.ciphertext)).
              mapTo[User.DecryptedMessage]
            onSuccess(decrypted) { decrypted =>
              complete(Plaintext(decrypted.message))
            }
          }
        }
      }
    }
}

/**
  * HTTP server routing to the relevant akka actors.
  */
object HttpServer extends App with IbeRoutes {
  implicit val system: ActorSystem = ActorSystem("IBE")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val keyGenCenter: ActorRef = system.actorOf(KeyGenCenter.props("t-bast-center9"), "KeyGenCenter")

  val bindingFuture = Http().bindAndHandle(ibeRoutes, "localhost", 8080)

  println("Server available at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine()
  println("Exit signal received, stopping server...")

  // Clean up server binding resources.
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => {
      system.terminate()
      println("Server stopped.")
    })
}
