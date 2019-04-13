import org.scalatest.{Matchers, WordSpec}

class IBESpec extends WordSpec with Matchers {
  "Identity-Based Encryption" should {
    val ibe = new IBE

    "initialize non-degenerate system parameters" in {
      ibe.P.isOne should ===(false)
      ibe.P.isZero should ===(false)
      ibe.Pub.isOne should ===(false)
      ibe.Pub.isZero should ===(false)
    }

    "extract non-degenerate user keys" in {
      val sk = ibe.extract("elliot@ecorp.com")
      sk.isOne should ===(false)
      sk.isZero should ===(false)
    }

    "extract deterministic user keys" in {
      val sk1 = ibe.extract("elliot@ecorp.com")
      val sk2 = ibe.extract("elliot@ecorp.com")
      sk1 should ===(sk2)
    }

    "extract private keys from user ids" in {
      val sk1 = ibe.extract("elliot@ecorp.com")
      val sk2 = ibe.extract("elliot@ecorp.com|2019")
      sk1 should !==(sk2)
    }

    "encrypt and decrypt" in {
      val sk = ibe.extract("elliot@ecorp.com")
      val message = Array.fill(32)(42.byteValue)
      val (c1, c2) = ibe.encrypt("elliot@ecorp.com", message)
      val decrypted = ibe.decrypt(sk, c1, c2)
      decrypted should ===(message)
    }
  }
}
