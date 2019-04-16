import java.nio.charset.StandardCharsets
import akka.actor.{Actor, ActorLogging, Props}
import it.unisa.dia.gas.jpbc.Element

object User {
  def props(id: String, params: IBE.Params): Props = Props(new User(id, params))

  final case class GenerateKey(requestId: Long, label: String)
  final case class PrivateKey(requestId: Long, value: Option[Element])

  final case class Encrypt(requestId: Long, label: String, message: String)
  final case class EncryptedMessage(requestId: Long, ciphertext: IBE.EncryptedPayload)

  final case class Decrypt(requestId: Long, label: String, ciphertext: IBE.EncryptedPayload)
  final case class DecryptedMessage(requestId: Long, message: String)
}

/**
  * A User actor should be created on-demand when a user logs-in.
  * It can then generate keys for custom labels associated to the user.
  *
  * @param id of the user (should have been authenticated upstream).
  */
class User(id: String, params: IBE.Params) extends Actor with ActorLogging {
  import User._

  val ibe = new IBE(params)
  val userKeys = Map.empty[String, Element]

  override def receive: Receive = {
    case GenerateKey(requestId, label) =>
      userKeys.get(label) match {
        case Some(key) => sender() ! PrivateKey(requestId, Some(key))
        case None =>
          log.info("Generating key for {}|{}", id, label)
          val key = ibe.genUserKey(s"$id|$label")
          sender() ! PrivateKey(requestId, Some(key))
      }
    case Encrypt(requestId, label, message) =>
      log.info("Encrypting message for {}|{}", id, label)
      val encrypted = ibe.encrypt(s"$id|$label", message.getBytes(StandardCharsets.UTF_8))
      sender() ! EncryptedMessage(requestId, encrypted)
    case Decrypt(requestId, label, ciphertext) =>
      log.info("Decrypting message for {}|{}", id, label)
      userKeys.get(label) match {
        case Some(key) =>
          val decrypted = ibe.decrypt(key, ciphertext)
          sender() ! DecryptedMessage(requestId, new String(decrypted, StandardCharsets.UTF_8))
        case None =>
          log.info("Generating key for {}|{}", id, label)
          val key = ibe.genUserKey(s"$id|$label")
          val decrypted = ibe.decrypt(key, ciphertext)
          sender() ! DecryptedMessage(requestId, new String(decrypted, StandardCharsets.UTF_8))
      }
  }
}
