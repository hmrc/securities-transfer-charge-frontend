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
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.DeclarationRole
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.DeclarationRole.*

class DeclarationRoleSpec extends AnyWordSpec with Matchers:

  val cases: Seq[(DeclarationRole, JsValue)] = Seq(
    Director          -> JsString("1"),
    Secretary         -> JsString("2"),
    PersonAuthorised  -> JsString("3"),
    Administrator     -> JsString("4"),
    Receiver          -> JsString("5"),
    ReceiverManager   -> JsString("6"),
    CicManager        -> JsString("7"),
    UkSocietas        -> JsString("8")
  )
  
  
  "DeclarationRole" should {
    "serialise to the correct value for a given role rate percentage" in {
      cases.foreach { case (role, expectedJson) =>
        Json.toJson(role) shouldBe expectedJson
      }
    }

    "deserialise from the correct value for a given role rate percentage" in {
      cases.foreach { case (expectedrole, json) =>
        Json.fromJson[DeclarationRole](json).get shouldBe expectedrole
      }
    }

    "round trip correctly" in {
      cases.foreach { case (role, _) =>
        Json.fromJson[DeclarationRole](Json.toJson(role)).get shouldBe role
      }
    }

    "Fail to deserialise from invalid values" in {
      val invalidJsonValues = Seq(JsNumber(0), JsString("300"), JsBoolean(false), JsNumber(1.5), JsString("42"))
      invalidJsonValues.foreach { json =>
        Json.fromJson[DeclarationRole](json).isError shouldBe true
      }
    }
}
