import akka.actor.{Actor, ActorLogging, Props}

object User {
  def props(id: String): Props = Props(new User(id))

  final case class GenerateKey(requestId: Long)
  final case class PrivateKey(requestId: Long, value: Option[Long])
}

/**
  * A User actor should be created on-demand when a key needs to be generated.
  * The actor will stop itself once the key is generated.
  *
  * @param id of the user.
  */
class User(id: String) extends Actor with ActorLogging {
  import User._

  override def receive: Receive = {
    case GenerateKey(requestId) =>
      sender() ! PrivateKey(requestId, None)
      context.stop(self)
  }
}
