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

package services.fileupload

import base.SpecBase
import uk.gov.hmrc.securitiestransferchargefrontend.models.Relief
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.ReliefNames

class ReliefNamesSpec extends SpecBase {

  private val allowed = ReliefNames.from(Seq(Relief("Charities Relief", 100), Relief("PISCES", 100)))

  "ReliefNames.isAllowed" - {

    "must allow a name in the list" in {
      ReliefNames.isAllowed("Charities Relief", allowed) mustBe true
    }

    "must ignore case and surrounding spaces" in {
      ReliefNames.isAllowed("  charities RELIEF ", allowed) mustBe true
      ReliefNames.isAllowed("pisces", allowed) mustBe true
    }

    "must reject a name not in the list" in {
      ReliefNames.isAllowed("Made Up Relief", allowed) mustBe false
    }

    "must reject everything when there are no reliefs" in {
      ReliefNames.isAllowed("Charities Relief", ReliefNames.from(Seq.empty)) mustBe false
    }
  }
}
