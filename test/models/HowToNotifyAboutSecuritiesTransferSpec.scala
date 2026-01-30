package models

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}

class HowToNotifyAboutSecuritiesTransferSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "HowToNotifyAboutSecuritiesTransfer" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(HowToNotifyAboutSecuritiesTransfer.values.toSeq)

      forAll(gen) {
        howToNotifyAboutSecuritiesTransfer =>

          JsString(howToNotifyAboutSecuritiesTransfer.toString).validate[HowToNotifyAboutSecuritiesTransfer].asOpt.value mustEqual howToNotifyAboutSecuritiesTransfer
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!HowToNotifyAboutSecuritiesTransfer.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[HowToNotifyAboutSecuritiesTransfer] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(HowToNotifyAboutSecuritiesTransfer.values.toSeq)

      forAll(gen) {
        howToNotifyAboutSecuritiesTransfer =>

          Json.toJson(howToNotifyAboutSecuritiesTransfer) mustEqual JsString(howToNotifyAboutSecuritiesTransfer.toString)
      }
    }
  }
}
