/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models.submission

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsNumber, JsValue, Json}
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.BuyerTaxRate
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.BuyerTaxRate.{HalfPercent, OneAndHalfPercent}

class BuyerTaxRateSpec extends AnyWordSpec with Matchers:

  val cases: Seq[(BuyerTaxRate, JsValue)] = Seq(
    HalfPercent       -> JsNumber(1),
    OneAndHalfPercent -> JsNumber(2)
  )

  "BuyerTaxRate" should {
    "serialise to the correct value for a given tax rate percentage" in {
      cases.foreach { case (tax, expectedJson) =>
        Json.toJson(tax) shouldBe expectedJson
      }
    }

    "deserialise from the correct value for a given tax rate percentage" in {
      cases.foreach { case (expectedTax, json) =>
        Json.fromJson[BuyerTaxRate](json).get shouldBe expectedTax
      }
    }

    "round trip correctly" in {
      cases.foreach { case (tax, _) =>
        Json.fromJson[BuyerTaxRate](Json.toJson(tax)).get shouldBe tax
      }
    }

    "Fail to deserialise from invalid values" in {
      val invalidJsonValues = Seq(JsNumber(0), JsNumber(3), JsNumber(-1), JsNumber(1.5), JsNumber(2.5))
      invalidJsonValues.foreach { json =>
        Json.fromJson[BuyerTaxRate](json).isError shouldBe true
      }
    }
}
