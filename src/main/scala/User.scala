import akka.actor.{Actor, ActorLogging, Props}

object User {
  def props(id: String): Props = Props(new User(id))

  final case class GenerateKey(requestId: Long)
  final case class PrivateKey(requestId: Long, value: Option[Long])
}

class User(id: String) extends Actor with ActorLogging {
  import User._

  override def receive: Receive = {
    case GenerateKey(requestId) => sender() ! PrivateKey(requestId, None)
  }
}
