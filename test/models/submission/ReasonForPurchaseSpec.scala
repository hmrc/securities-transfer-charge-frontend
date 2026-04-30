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
import play.api.libs.json.{JsBoolean, JsNumber, JsString, JsValue}
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.ReasonForPurchase
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.ReasonForPurchase.*

class ReasonForPurchaseSpec extends AnyWordSpec with Matchers:

  val cases: Seq[(ReasonForPurchase, JsValue)] = Seq(
    PurchasedForCancellation      -> JsNumber(1),
    PurchasedToPlaceIntoTreasury  -> JsNumber(2),
    Both                          -> JsNumber(3)
  )

  "ReasonForPurchase" should {
    "serialise to the correct value for a given reason for purchase" in {
      cases.foreach { case (reason, expectedJson) =>
        play.api.libs.json.Json.toJson(reason) shouldBe expectedJson
      }
    }

    "deserialise from the correct value for a given reason for purchase" in {
      cases.foreach { case (expectedReason, json) =>
        play.api.libs.json.Json.fromJson[ReasonForPurchase](json).get shouldBe expectedReason
      }
    }

    "round trip correctly" in {
      cases.foreach { case (reason, _) =>
        play.api.libs.json.Json.fromJson[ReasonForPurchase](play.api.libs.json.Json.toJson(reason)).get shouldBe reason
      }
    }

    "Fail to deserialise from invalid values" in {
      val invalidJsonValues = Seq(JsNumber(0), JsNumber(4), JsNumber(-1), JsString("one"), JsBoolean(true))
      invalidJsonValues.foreach { json =>
        play.api.libs.json.Json.fromJson[ReasonForPurchase](json).isError shouldBe true
      }
    }
  }
