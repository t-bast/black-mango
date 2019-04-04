import akka.actor.{Actor, ActorLogging, Props}

object User {
  def props(id: String): Props = Props(new User(id))

  final case class GenerateKey(requestId: Long, label: String)
  final case class PrivateKey(requestId: Long, value: Option[Long])
}

/**
  * A User actor should be created on-demand when a user logs-in.
  * It can then generate keys for custom labels associated to the user.
  *
  * @param id of the user (should have been authenticated upstream).
  */
class User(id: String) extends Actor with ActorLogging {
  import User._

  override def receive: Receive = {
    case GenerateKey(requestId, label) =>
      log.info("Generating key for {} / {}", id, label)
      sender() ! PrivateKey(requestId, None)
  }
}
