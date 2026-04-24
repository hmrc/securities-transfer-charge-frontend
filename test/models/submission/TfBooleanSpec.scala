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
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.TfBoolean

class TfBooleanSpec extends AnyWordSpec with Matchers:

  val cases: Seq[(TfBoolean, JsValue)] = Seq(
    TfBoolean(true) -> JsString("T"),
    TfBoolean(false) -> JsString("F")
  )
  
  "TfBoolean" should {
    "serialise to the correct value for a given boolean" in {
      cases.foreach { case (tfBoolean, expectedJson) =>
        play.api.libs.json.Json.toJson(tfBoolean) shouldBe expectedJson
      }
    }

    "deserialise from the correct value for a given boolean" in {
      cases.foreach { case (expectedTfBoolean, json) =>
        play.api.libs.json.Json.fromJson[TfBoolean](json).get shouldBe expectedTfBoolean
      }
    }

    "round trip correctly" in {
      cases.foreach { case (tfBoolean, _) =>
        play.api.libs.json.Json.fromJson[TfBoolean](play.api.libs.json.Json.toJson(tfBoolean)).get shouldBe tfBoolean
      }
    }

    "Fail to deserialise from invalid values" in {
      val invalidJsonValues = Seq(JsString("Y"), JsString("N"), JsString("true"), JsString("false"), JsBoolean(true), JsNumber(22))
      invalidJsonValues.foreach { json =>
        play.api.libs.json.Json.fromJson[TfBoolean](json).isError shouldBe true
      }
    }
  }
