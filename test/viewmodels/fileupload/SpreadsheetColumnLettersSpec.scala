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

package viewmodels.fileupload

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.fileupload.SpreadsheetColumnLetters

class SpreadsheetColumnLettersSpec extends AnyWordSpec with Matchers {

  "fromZeroBasedIndex" should {

    "convert single-letter columns correctly" in {
      SpreadsheetColumnLetters.fromZeroBasedIndex(0) shouldBe "A"
      SpreadsheetColumnLetters.fromZeroBasedIndex(1) shouldBe "B"
      SpreadsheetColumnLetters.fromZeroBasedIndex(7) shouldBe "H"
      SpreadsheetColumnLetters.fromZeroBasedIndex(25) shouldBe "Z"
    }

    "convert multi-letter columns correctly" in {
      SpreadsheetColumnLetters.fromZeroBasedIndex(26) shouldBe "AA"
      SpreadsheetColumnLetters.fromZeroBasedIndex(27) shouldBe "AB"
      SpreadsheetColumnLetters.fromZeroBasedIndex(51) shouldBe "AZ"
      SpreadsheetColumnLetters.fromZeroBasedIndex(52) shouldBe "BA"
    }

    "throw for negative indexes" in {
      val exception = the[IllegalArgumentException] thrownBy {
        SpreadsheetColumnLetters.fromZeroBasedIndex(-1)
      }

      exception.getMessage should include("Column index must be >= 0")
    }
  }
}