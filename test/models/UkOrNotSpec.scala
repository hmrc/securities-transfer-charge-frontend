package models

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}

class UkOrNotSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "UkOrNot" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(UkOrNot.values.toSeq)

      forAll(gen) {
        ukOrNot =>

          JsString(ukOrNot.toString).validate[UkOrNot].asOpt.value mustEqual ukOrNot
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!UkOrNot.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[UkOrNot] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(UkOrNot.values.toSeq)

      forAll(gen) {
        ukOrNot =>

          Json.toJson(ukOrNot) mustEqual JsString(ukOrNot.toString)
      }
    }
  }
}
