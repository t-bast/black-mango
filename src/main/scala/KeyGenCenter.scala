import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}

object KeyGenCenter {
  def props(id: String): Props = Props(new KeyGenCenter(id))

  final case class GenerateKey(requestId: Long, user: String, label: String)
}

/**
  * A Key Generation Center creates user actors on-demand and forwards key generation requests accordingly.
  *
  * @param id of the key generation center (friendly name).
  */
class KeyGenCenter(id: String) extends Actor with ActorLogging {
  import KeyGenCenter._

  var users = Map.empty[String, ActorRef]
  var userIds = Map.empty[ActorRef, String]

  override def receive: Receive = {
    case GenerateKey(requestId, user, label) =>
      users.get(user) match {
        case Some(userActor) =>
          userActor forward User.GenerateKey(requestId, label)
        case None =>
          log.info("Creating user actor for {}", user)
          val userActor = context.actorOf(User.props(user), s"user-$user")
          context.watch(userActor)

          users += user -> userActor
          userIds += userActor -> user
          userActor forward User.GenerateKey(requestId, label)
      }

    case Terminated(userActor) =>
      val userId = userIds(userActor)
      log.info("User actor {} has been terminated", userId)
      users -= userId
      userIds -= userActor
  }
}
