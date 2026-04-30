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
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcTaxRateParser
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcTaxRateParser.ParsedTaxRate

class StcTaxRateParserSpec extends AnyWordSpec with Matchers {

  "StcTaxRateParser.parse" must {

    "parse 0.5" in {
      StcTaxRateParser.parse(0.5) mustBe Some(ParsedTaxRate.HalfPercent)
    }
    
    "parse 1.5" in {
      StcTaxRateParser.parse(1.5) mustBe Some(ParsedTaxRate.OneAndHalfPercent)
    }

    "return None for invalid values" in {
      StcTaxRateParser.parse(0) mustBe None
      StcTaxRateParser.parse(2) mustBe None
    }
  }
}