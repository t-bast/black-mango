import java.nio.charset.StandardCharsets
import javax.crypto.AEADBadTagException
import org.scalatest.{Matchers, WordSpec}
import IBE._

class IBESpec extends WordSpec with Matchers {
  "Identity-Based Encryption" should {
    val params = IBE.generateParams()
    val ibe = new IBE(params)

    "initialize non-degenerate system parameters" in {
      params.generator.isOne should ===(false)
      params.generator.isZero should ===(false)
      params.masterPublicKey.isOne should ===(false)
      params.masterPublicKey.isZero should ===(false)
    }

    "extract non-degenerate user keys" in {
      val sk = ibe.genUserKey("elliot@ecorp.com")
      sk.isOne should ===(false)
      sk.isZero should ===(false)
    }

    "extract deterministic user keys" in {
      val sk1 = ibe.genUserKey("elliot@ecorp.com")
      val sk2 = ibe.genUserKey("elliot@ecorp.com")
      sk1 should ===(sk2)
    }

    "extract private keys from user ids" in {
      val sk1 = ibe.genUserKey("elliot@ecorp.com")
      val sk2 = ibe.genUserKey("elliot@ecorp.com|2019")
      sk1 should !==(sk2)
    }

    "encrypt and decrypt" in {
      val sk = ibe.genUserKey("elliot@ecorp.com")
      val message = "are you seeing what I'm seeing?"
      val encrypted = ibe.encrypt("elliot@ecorp.com", message.getBytes(StandardCharsets.UTF_8))
      val decrypted = new String(ibe.decrypt(sk, encrypted), StandardCharsets.UTF_8)
      decrypted should ===(message)
    }

    "decrypt invalid ciphertext bytes" in {
      val sk = ibe.genUserKey("elliot@ecorp.com")
      val message = "Car je ne puis trouver parmi ces pâles roses"
      val EncryptedPayload(rCommit, ciphertext) = ibe.encrypt("elliot@ecorp.com", message.getBytes(StandardCharsets.UTF_8))
      val invalidCiphertext = ciphertext map (b => (b + 1).toByte)

      assertThrows[AEADBadTagException] {
        ibe.decrypt(sk, EncryptedPayload(rCommit, invalidCiphertext))
      }
    }

    "decrypt invalid ciphertext curve point" in {
      val sk = ibe.genUserKey("elliot@ecorp.com")
      val message = "Une fleur qui ressemble à mon rouge idéal."
      val EncryptedPayload(rCommit, ciphertext) = ibe.encrypt("elliot@ecorp.com", message.getBytes(StandardCharsets.UTF_8))
      val invalidPoint = rCommit.square().getImmutable

      assertThrows[AEADBadTagException] {
        ibe.decrypt(sk, EncryptedPayload(invalidPoint, ciphertext))
      }
    }
  }
}
