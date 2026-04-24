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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{ParsedCell, ParsedRow, ParsedValue}
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.ParsedRowReader

import java.time.LocalDate

class ParsedRowReaderSpec extends AnyWordSpec with Matchers {

  private def row(values: String*): ParsedRow =
    ParsedRow(
      rowNumber = 3,
      cells = values.zipWithIndex.map { case (value, idx) =>
        ParsedCell(idx, value)
      }
    )

  "readString" should {

    "return Valid(trimmed value) when present" in {
      ParsedRowReader.readString(row("  hello  "), 0) shouldBe ParsedValue.Valid("hello")
    }

    "return Missing for blank strings" in {
      ParsedRowReader.readString(row("   "), 0) shouldBe ParsedValue.Missing
    }

    "return Missing when the column is missing" in {
      ParsedRowReader.readString(row("a"), 10) shouldBe ParsedValue.Missing
    }
  }

  "readBigDecimal" should {

    "parse a plain decimal" in {
      ParsedRowReader.readBigDecimal(row("123.45"), 0) shouldBe ParsedValue.Valid(BigDecimal("123.45"))
    }

    "parse a value containing commas" in {
      ParsedRowReader.readBigDecimal(row("1,234.56"), 0) shouldBe ParsedValue.Valid(BigDecimal("1234.56"))
    }

    "parse a value containing a pound sign" in {
      ParsedRowReader.readBigDecimal(row("£600.00"), 0) shouldBe ParsedValue.Valid(BigDecimal("600.00"))
    }

    "parse a value containing a percent sign" in {
      ParsedRowReader.readBigDecimal(row("0.5%"), 0) shouldBe ParsedValue.Valid(BigDecimal("0.5"))
    }

    "return Invalid for an invalid number" in {
      ParsedRowReader.readBigDecimal(row("abc"), 0) shouldBe ParsedValue.Invalid("abc", "not a number")
    }

    "return Missing for a blank value" in {
      ParsedRowReader.readBigDecimal(row("   "), 0) shouldBe ParsedValue.Missing
    }

    "return Missing when the column is missing" in {
      ParsedRowReader.readBigDecimal(row("123.45"), 10) shouldBe ParsedValue.Missing
    }
  }

  "readBoolean" should {

    "parse positive answers as true" in {
      ParsedRowReader.readBoolean(row("yes"), 0) shouldBe ParsedValue.Valid(true)
      ParsedRowReader.readBoolean(row("Y"), 0) shouldBe ParsedValue.Valid(true)
      ParsedRowReader.readBoolean(row("true"), 0) shouldBe ParsedValue.Valid(true)
    }

    "parse negative answers as false" in {
      ParsedRowReader.readBoolean(row("no"), 0) shouldBe ParsedValue.Valid(false)
      ParsedRowReader.readBoolean(row("N"), 0) shouldBe ParsedValue.Valid(false)
      ParsedRowReader.readBoolean(row("false"), 0) shouldBe ParsedValue.Valid(false)
    }

    "return Invalid for unrecognised values" in {
      ParsedRowReader.readBoolean(row("maybe"), 0) shouldBe ParsedValue.Invalid("maybe", "not a recognised boolean")
    }

    "return Missing for a blank value" in {
      ParsedRowReader.readBoolean(row("   "), 0) shouldBe ParsedValue.Missing
    }

    "return Missing when the column is missing" in {
      ParsedRowReader.readBoolean(row("yes"), 10) shouldBe ParsedValue.Missing
    }
  }

  "readDate" should {

    "parse supported date formats" in {
      Seq(
        "2026-03-23" -> LocalDate.of(2026, 3, 23),
        "23/3/2026"  -> LocalDate.of(2026, 3, 23),
        "23/03/2026" -> LocalDate.of(2026, 3, 23),
        "23 03 2026" -> LocalDate.of(2026, 3, 23)
      ).foreach { case (input, expected) =>
        ParsedRowReader.readDate(row(input), 0) shouldBe ParsedValue.Valid(expected)
      }
    }

    "return Invalid for invalid dates" in {
      ParsedRowReader.readDate(row("not-a-date"), 0) shouldBe ParsedValue.Invalid("not-a-date", "not a valid date")
    }

    "return Missing when the value is blank" in {
      ParsedRowReader.readDate(row("   "), 0) shouldBe ParsedValue.Missing
    }

    "return Missing when the column is missing" in {
      ParsedRowReader.readDate(row("2026-03-23"), 10) shouldBe ParsedValue.Missing
    }
  }
}