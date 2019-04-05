import org.scalatest.{Matchers, WordSpec}

class IBESpec extends WordSpec with Matchers {
  "Identity-Based Encryption" should {
    "initialize non-degenerate system parameters" in {
      val ibe = new IBE
      ibe.P.isOne should ===(false)
      ibe.P.isZero should ===(false)
    }
  }
}
