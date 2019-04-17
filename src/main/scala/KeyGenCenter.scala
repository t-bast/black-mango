import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}

object KeyGenCenter {
  def props(id: String): Props = Props(new KeyGenCenter(id))

  final case class GenerateKey(requestId: Long, user: String, label: String)
  final case class Encrypt(requestId: Long, user: String, label: String, message: String)
  final case class Decrypt(requestId: Long, user: String, label: String, ciphertext: IBE.EncryptedPayload)
}

/**
  * A Key Generation Center creates user actors on-demand and forwards key generation requests accordingly.
  *
  * @param id of the key generation center (friendly name).
  */
class KeyGenCenter(id: String) extends Actor with ActorLogging {
  import KeyGenCenter._

  private val params = IBE.generateParams()

  var users = Map.empty[String, ActorRef]
  var userIds = Map.empty[ActorRef, String]

  def createActor(user: String): ActorRef = {
    log.info("Creating user actor for {}", user)
    val userActor = context.actorOf(User.props(user, params), s"user-$user")
    context.watch(userActor)

    users += user -> userActor
    userIds += userActor -> user

    userActor
  }

  // TODO: there is a lot of duplication here, should be refactored.
  override def receive: Receive = {
    case GenerateKey(requestId, user, label) =>
      users.get(user) match {
        case Some(userActor) =>
          userActor forward User.GenerateKey(requestId, label)
        case None =>
          val userActor = createActor(user)
          userActor forward User.GenerateKey(requestId, label)
      }

    case Encrypt(requestId, user, label, message) =>
      users.get(user) match {
        case Some(userActor) =>
          userActor forward User.Encrypt(requestId, label, message)
        case None =>
          val userActor = createActor(user)
          userActor forward User.Encrypt(requestId, label, message)
      }

    case Decrypt(requestId, user, label, ciphertext) =>
      users.get(user) match {
        case Some(userActor) =>
          userActor forward User.Decrypt(requestId, label, ciphertext)
        case None =>
          val userActor = createActor(user)
          userActor forward User.Decrypt(requestId, label, ciphertext)
      }

    case Terminated(userActor) =>
      val userId = userIds(userActor)
      log.info("User actor {} has been terminated", userId)
      users -= userId
      userIds -= userActor
  }
}
