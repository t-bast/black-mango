import it.unisa.dia.gas.plaf.jpbc.pairing.a.TypeACurveGenerator
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory
import java.math.BigInteger
import java.security.SecureRandom

/**
  * Encapsulate the IBE primitives.
  * New system parameters are generated for each instance.
  */
case class IBE() {
  private val rBits = 160
  private val qBits = 512

  // Note: this is probably very insecure because IBE seems to require a type B pairing
  // but jPBC doesn't offer it...
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
  val P = curve.newRandomElement.mul(new BigInteger(masterKey))

  /**
    * Extract a private key for the given ID.
    *
    * @param id of the user's key.
    * @return TODO
    */
  def extract(id: String) = ???
}
