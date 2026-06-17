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
import play.api.libs.json.{JsBoolean, JsNumber, JsString, JsValue, Json}
import uk.gov.hmrc.securitiestransferchargefrontend.domain.TransferType

class TransferTypeSpec extends AnyWordSpec with Matchers:

  val cases: Seq[(TransferType, JsValue)] = Seq(
    TransferType.STF   -> JsNumber(1),
    TransferType.SH03  -> JsNumber(2),
    TransferType.Other -> JsNumber(3)
  )

  "TransferType" should {
    "serialise to the correct value for a given transfer type" in {
      cases.foreach { case (transferType, expectedJson) =>
        Json.toJson(transferType) shouldBe expectedJson
      }
    }

    "deserialise from the correct value for a given transfer type" in {
      cases.foreach { case (expectedTransferType, json) =>
        Json.fromJson[TransferType](json).get shouldBe expectedTransferType
      }
    }

    "round trip correctly" in {
      cases.foreach { case (transferType, _) =>
        Json.fromJson[TransferType](Json.toJson(transferType)).get shouldBe transferType
      }
    }

    "Fail to deserialise from invalid values" in {
      val invalidJsonValues = Seq(JsNumber(0), JsNumber(4), JsNumber(-1), JsString("STF"), JsBoolean(false))
      invalidJsonValues.foreach { json =>
        Json.fromJson[TransferType](json).isError shouldBe true
      }
    }
}
