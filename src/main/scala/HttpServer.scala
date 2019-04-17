import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

trait IbeRoutes {
  // These should be provided by the app.
  implicit def system: ActorSystem
  def keyGenCenter: ActorRef

  implicit lazy val timeout: Timeout = Timeout(5.seconds)

  lazy val ibeRoutes: Route =
    path("healthz") {
      get {
        complete(HttpEntity("ok"))
      }
    } ~
      path("keys" / Segment / Segment) { (user: String, label: String) =>
        get {
          complete(s"key generated for $user with label $label")
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
