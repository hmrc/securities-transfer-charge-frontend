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

package models.stf.fileupload

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{ParsedCell, ParsedRow}

class ParsedRowSpec extends AnyWordSpec with Matchers {

  "valueAt" should {

    "return the value for an existing column index" in {
      val row = ParsedRow(
        rowNumber = 3,
        cells = Seq(
          ParsedCell(0, "foo"),
          ParsedCell(1, "bar")
        )
      )

      row.valueAt(0) shouldBe Some("foo")
      row.valueAt(1) shouldBe Some("bar")
    }

    "return None for a missing column index" in {
      val row = ParsedRow(
        rowNumber = 3,
        cells = Seq(
          ParsedCell(0, "foo")
        )
      )

      row.valueAt(99) shouldBe None
    }
  }

  "isCompletelyEmpty" should {

    "return true when all cells are blank" in {
      val row = ParsedRow(
        rowNumber = 3,
        cells = Seq(
          ParsedCell(0, ""),
          ParsedCell(1, "   ")
        )
      )

      row.isCompletelyEmpty shouldBe true
    }

    "return false when at least one cell contains data" in {
      val row = ParsedRow(
        rowNumber = 3,
        cells = Seq(
          ParsedCell(0, ""),
          ParsedCell(1, "value")
        )
      )

      row.isCompletelyEmpty shouldBe false
    }
  }
}