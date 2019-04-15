import com.google.crypto.tink.subtle.ChaCha20Poly1305
import it.unisa.dia.gas.plaf.jpbc.pairing.a.TypeACurveGenerator
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory
import it.unisa.dia.gas.jpbc.{Element, PairingParameters}
import java.math.BigInteger
import java.security.{MessageDigest, SecureRandom}

object IBE {

  trait KeyGen {
    /**
      * Generate a private key for the given ID.
      *
      * @param id of the user.
      * @return this user's private key (a curve point).
      */
    def genUserKey(id: String): Element
  }

  trait Encrypter {
    /**
      * Encrypt a message for the given user.
      *
      * @param id      of the recipient.
      * @param message to encrypt.
      * @return message encryption.
      */
    def encrypt(id: String, message: Array[Byte]): EncryptedPayload
  }

  trait Decrypter {
    /**
      * Decrypt a message for the given user.
      *
      * @param sk        private key of the recipient.
      * @param encrypted message.
      * @return the original message.
      */
    def decrypt(sk: Element, encrypted: EncryptedPayload): Array[Byte]
  }

  /**
    * Static parameters for an IBE instance.
    *
    * @param pairingParams   parameters for the pairing operation (curve and type of pairing).
    * @param generator       random curve generator.
    * @param masterPublicKey master key public curve point.
    * @param masterKey       master secret key.
    */
  case class Params(
                     pairingParams: PairingParameters,
                     generator: Element,
                     masterPublicKey: Element,
                     masterKey: Array[Byte])

  /**
    * A message encrypted with IBE.
    *
    * @param randomCommitment is a commitment to the randomness used.
    * @param ciphertext       is the encrypted ciphertext bytes.
    */
  case class EncryptedPayload(randomCommitment: Element, ciphertext: Array[Byte])

  /**
    * Generate parameters for an IBE instance.
    * Obviously the same set of parameters needs to be used for encryption and decryption.
    *
    * @return the IBE parameters.
    */
  def generateParams(): Params = {
    val rng = new SecureRandom()
    val masterKey = Array.fill(64)(0.byteValue)
    rng.nextBytes(masterKey)

    // Note: this is probably very insecure because IBE seems to require a type B pairing
    // but jPBC doesn't offer it...
    // We also should use a specific, well-chosen curve instead of generating a random one.
    val pairingParams = new TypeACurveGenerator(160, 512).generate()
    val pairing = PairingFactory.getPairing(pairingParams)

    // G1 and G2 both represent the elliptic curve group in a Type A pairing (symmetric), so either is fine.
    val curve = pairing.getG1
    val P = curve.newRandomElement.getImmutable
    // Note: in the Boneh-Franklin paper the base point should be of order q, which is not the case here.
    // But with a type A pairing I'm not sure if that's necessary: a thorough security analysis is needed.
    val Pub = P.mul(new BigInteger(masterKey)).getImmutable

    Params(pairingParams, P, Pub, masterKey)
  }

  /**
    * Map an arbitrary string to a curve point.
    * Note that we are *not* using the paper's recommended algorithm so this is most likely insecure.
    * A security analysis of the jPBC hash to group operation is needed.
    *
    * @param label  to map to a curve point.
    * @param params parameters of the IBE instance.
    * @return a curve point.
    */
  def mapToPoint(label: String, params: Params): Element = {
    val sha = MessageDigest.getInstance("SHA-256")
    val h = sha.digest(label.getBytes())
    PairingFactory.getPairing(params.pairingParams).
      getG1.
      newElementFromHash(h, 0, h.length).
      getImmutable
  }
}

/**
  * Encapsulate the IBE primitives.
  * This could be split if it becomes necessary to restrict actors' capabilities.
  *
  * @param params parameters of the IBE instance.
  */
case class IBE(params: IBE.Params) extends IBE.KeyGen with IBE.Encrypter with IBE.Decrypter {
  import IBE._

  private val rng = new SecureRandom()
  private val pairing = PairingFactory.getPairing(params.pairingParams)

  override def genUserKey(id: String): Element = {
    val q = mapToPoint(id, params)
    q.mul(new BigInteger(params.masterKey)).getImmutable
  }

  override def encrypt(id: String, message: Array[Byte]): EncryptedPayload = {
    val q = mapToPoint(id, params)
    val rb = Array.fill(64)(0.byteValue)
    rng.nextBytes(rb)

    val r = new BigInteger(rb)
    val gid = pairing.pairing(q, params.masterPublicKey)

    val rCommit = params.generator.mul(r).getImmutable

    val h = MessageDigest.getInstance("SHA-256").digest(gid.pow(r).toBytes)
    val cipher = new ChaCha20Poly1305(h)
    val ciphertext = cipher.encrypt(message, Array.empty)

    EncryptedPayload(rCommit, ciphertext)
  }

  override def decrypt(sk: Element, encrypted: EncryptedPayload): Array[Byte] = {
    val EncryptedPayload(rCommit, ciphertext) = encrypted
    val gidr = pairing.pairing(sk, rCommit)
    val h = MessageDigest.getInstance("SHA-256").digest(gidr.toBytes)
    val cipher = new ChaCha20Poly1305(h)
    val message = cipher.decrypt(ciphertext, Array.empty)

    message
  }
}
