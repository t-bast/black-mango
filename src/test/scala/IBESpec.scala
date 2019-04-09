import org.scalatest.{Matchers, WordSpec}

class IBESpec extends WordSpec with Matchers {
  "Identity-Based Encryption" should {
    val ibe = new IBE

    "initialize non-degenerate system parameters" in {
      ibe.P.isOne should ===(false)
      ibe.P.isZero should ===(false)
    }

    "extract non-degenerate user keys" in {
      val sk = ibe.extract("eliot@ecorp.com")
      sk.isOne should ===(false)
      sk.isZero should ===(false)
    }

    "extract deterministic user keys" in {
      val sk1 = ibe.extract("eliot@ecorp.com")
      val sk2 = ibe.extract("eliot@ecorp.com")
      sk1 should ===(sk2)
    }

    "extract private keys from user ids" in {
      val sk1 = ibe.extract("eliot@ecorp.com")
      val sk2 = ibe.extract("eliot@ecorp.com|2019")
      sk1 should !==(sk2)
    }
  }
}
