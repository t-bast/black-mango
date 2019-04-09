import it.unisa.dia.gas.plaf.jpbc.pairing.a.TypeACurveGenerator
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory
import it.unisa.dia.gas.jpbc.Element
import java.math.BigInteger
import java.security.{MessageDigest, SecureRandom}

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
    * Master public key.
    * TODO: this is currently insecure: the base point should be of order q.
    */
  val P: Element = curve.newRandomElement.mul(new BigInteger(masterKey)).getImmutable

  /**
    * Map an arbitrary string to a curve point.
    * Note that we are *not* using the paper's recommended algorithm so this is
    * most likely very insecure.
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
}
