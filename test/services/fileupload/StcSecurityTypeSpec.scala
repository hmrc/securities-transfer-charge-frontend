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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcSecurityType

class StcSecurityTypeSpec extends AnyWordSpec with Matchers {

  "StcSecurityType.isShares" must {

    "return true for shares ignoring case and whitespace" in {
      StcSecurityType.isShares("shares") mustBe true
      StcSecurityType.isShares("Shares") mustBe true
      StcSecurityType.isShares(" SHARES ") mustBe true
    }

    "return false for other security descriptions" in {
      StcSecurityType.isShares("Loan notes or other debt securities") mustBe false
      StcSecurityType.isShares("Interests in underlying securities") mustBe false
      StcSecurityType.isShares("Another type of security") mustBe false
      StcSecurityType.isShares("Ordinary Shares") mustBe false
    }
  }
}