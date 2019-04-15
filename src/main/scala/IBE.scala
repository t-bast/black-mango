import com.google.crypto.tink.subtle.ChaCha20Poly1305
import it.unisa.dia.gas.plaf.jpbc.pairing.a.TypeACurveGenerator
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory
import it.unisa.dia.gas.jpbc.Element
import java.math.BigInteger
import java.security.{MessageDigest, SecureRandom}

/**
  * A message encrypted with IBE.
  *
  * @param randomCommitment is the commitment to the randomness used.
  * @param ciphertext is the encrypted ciphertext bytes.
  */
case class EncryptedPayload(randomCommitment: Element, ciphertext: Array[Byte])

/**
  * Encapsulate the IBE primitives.
  * New system parameters are generated for each instance.
  */
case class IBE() {
  private val rBits = 160
  private val qBits = 512

  // Note: this is probably very insecure because IBE seems to require a type B pairing
  // but jPBC doesn't offer it...
  // We also should use a specific, well-chosen curve instead of generating a random one.
  private val pairing = PairingFactory.getPairing(new TypeACurveGenerator(rBits, qBits).generate())

  // G1 and G2 both represent the elliptic curve group in a Type A pairing (symmetric).
  private val curve = pairing.getG1

  private val rng = new SecureRandom()
  private val masterKey = Array.fill(qBits / 8)(0.byteValue)
  rng.nextBytes(masterKey)

  /**
    * Random base point used for this instance of the scheme.
    */
  val P: Element = curve.newRandomElement.getImmutable

  /**
    * Master public key.
    * TODO: this is currently insecure: the base point should be of order q.
    * But with a type A pairing I'm unsure if that's necessary: a security
    * analysis is needed.
    */
  val Pub: Element = P.mul(new BigInteger(masterKey)).getImmutable

  /**
    * Map an arbitrary string to a curve point.
    * Note that we are *not* using the paper's recommended algorithm so this is
    * most likely very insecure: a security analysis of the hash to group operation
    * is needed.
    *
    * @param id that should be mapped to a curve point.
    * @return curve point.
    */
  private def mapToPoint(id: String): Element = {
    val sha = MessageDigest.getInstance("SHA-256")
    val h = sha.digest(id.getBytes())
    curve.newElementFromHash(h, 0, h.length).getImmutable
  }

  /**
    * Extract a private key for the given ID.
    *
    * @param id of the user.
    * @return this user's private key (curve point).
    */
  def extract(id: String): Element = {
    val q = mapToPoint(id)
    q.mul(new BigInteger(masterKey)).getImmutable
  }

  /**
    * Encrypt a message for the given user.
    *
    * @param id of the recipient.
    * @param message to encrypt.
    * @return message encryption.
    */
  def encrypt(id: String, message: Array[Byte]): EncryptedPayload = {
    val q = mapToPoint(id)
    val rb = Array.fill(qBits / 8)(0.byteValue)
    rng.nextBytes(rb)
    val r = new BigInteger(rb)
    val gid = pairing.pairing(q, Pub)

    val rCommit = P.mul(r).getImmutable

    val h = MessageDigest.getInstance("SHA-256").digest(gid.pow(r).toBytes)
    val cipher = new ChaCha20Poly1305(h)
    val ciphertext = cipher.encrypt(message, Array.empty)

    EncryptedPayload(rCommit, ciphertext)
  }

  /**
    * Decrypt a message for the given user.
    *
    * @param sk private key of the recipient.
    * @param encrypted message.
    * @return the original message.
    */
  def decrypt(sk: Element, encrypted: EncryptedPayload): Array[Byte] = {
    val EncryptedPayload(rCommit, ciphertext) = encrypted
    val gidr = pairing.pairing(sk, rCommit)
    val h = MessageDigest.getInstance("SHA-256").digest(gidr.toBytes)
    val cipher = new ChaCha20Poly1305(h)
    val message = cipher.decrypt(ciphertext, Array.empty)

    message
  }
}
