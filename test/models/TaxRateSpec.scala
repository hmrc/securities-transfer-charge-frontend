package models

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}
import uk.gov.hmrc.securitiestransferchargefrontend.models.TaxRate

class TaxRateSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "TaxRate" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(TaxRate.values.toSeq)

      forAll(gen) {
        taxRate =>

          JsString(taxRate.toString).validate[TaxRate].asOpt.value mustEqual taxRate
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!TaxRate.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[TaxRate] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(TaxRate.values.toSeq)

      forAll(gen) {
        taxRate =>

          Json.toJson(taxRate) mustEqual JsString(taxRate.toString)
      }
    }
  }
}
