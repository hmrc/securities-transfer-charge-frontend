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
import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.{ParsedCell, ParsedRow, ParsedValue}
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.ParsedRowReader

import java.time.LocalDate

class ParsedRowReaderSpec extends AnyWordSpec with Matchers {

  private val testColumn = 1

  "ParsedRowReader.readString" must {

    "return Missing for absent values" in {
      ParsedRowReader.readString(row(None), testColumn) mustBe ParsedValue.Missing
      ParsedRowReader.readString(row(Some("")), testColumn) mustBe ParsedValue.Missing
      ParsedRowReader.readString(row(Some("   ")), testColumn) mustBe ParsedValue.Missing
    }

    "return trimmed Valid value" in {
      ParsedRowReader.readString(row(Some("  abc  ")), testColumn) mustBe ParsedValue.Valid("abc")
    }
  }

  "ParsedRowReader.readBigDecimal" must {

    "parse plain numbers" in {
      ParsedRowReader.readBigDecimal(row(Some("123.45")), testColumn) mustBe ParsedValue.Valid(BigDecimal("123.45"))
    }

    "parse values with commas, pounds and percent signs" in {
      ParsedRowReader.readBigDecimal(row(Some("1,234.56")), testColumn) mustBe ParsedValue.Valid(BigDecimal("1234.56"))
      ParsedRowReader.readBigDecimal(row(Some("£123.45")), testColumn) mustBe ParsedValue.Valid(BigDecimal("123.45"))
      ParsedRowReader.readBigDecimal(row(Some("0.5%")), testColumn) mustBe ParsedValue.Valid(BigDecimal("0.5"))
    }

    "return Missing for blank values" in {
      ParsedRowReader.readBigDecimal(row(Some(" ")), testColumn) mustBe ParsedValue.Missing
    }

    "return Invalid for non-numeric values" in {
      ParsedRowReader.readBigDecimal(row(Some("abc")), testColumn) mustBe ParsedValue.Invalid("abc", "not a number")
    }
  }

  "ParsedRowReader.readBoolean" must {

    "parse yes variants to true" in {
      ParsedRowReader.readBoolean(row(Some("yes")), testColumn) mustBe ParsedValue.Valid(true)
      ParsedRowReader.readBoolean(row(Some("y")), testColumn) mustBe ParsedValue.Valid(true)
      ParsedRowReader.readBoolean(row(Some("true")), testColumn) mustBe ParsedValue.Valid(true)
    }

    "parse no variants to false" in {
      ParsedRowReader.readBoolean(row(Some("no")), testColumn) mustBe ParsedValue.Valid(false)
      ParsedRowReader.readBoolean(row(Some("n")), testColumn) mustBe ParsedValue.Valid(false)
      ParsedRowReader.readBoolean(row(Some("false")), testColumn) mustBe ParsedValue.Valid(false)
    }

    "return Missing for blank values" in {
      ParsedRowReader.readBoolean(row(Some(" ")), testColumn) mustBe ParsedValue.Missing
    }

    "return Invalid for unrecognised values" in {
      ParsedRowReader.readBoolean(row(Some("maybe")), testColumn) mustBe ParsedValue.Invalid("maybe", "not a recognised boolean")
    }
  }

  "ParsedRowReader.readDate" must {

    "parse yyyy-MM-dd" in {
      ParsedRowReader.readDate(row(Some("2025-11-20")), testColumn) mustBe ParsedValue.Valid(LocalDate.of(2025, 11, 20))
    }

    "parse yyyy/MM/dd" in {
      ParsedRowReader.readDate(row(Some("2025/11/20")), testColumn) mustBe ParsedValue.Valid(LocalDate.of(2025, 11, 20))
    }

    "parse d/M/yyyy and dd/MM/yyyy" in {
      ParsedRowReader.readDate(row(Some("2/3/2025")), testColumn) mustBe ParsedValue.Valid(LocalDate.of(2025, 3, 2))
      ParsedRowReader.readDate(row(Some("02/03/2025")), testColumn) mustBe ParsedValue.Valid(LocalDate.of(2025, 3, 2))
    }

    "parse spaced dates" in {
      ParsedRowReader.readDate(row(Some("2 3 2025")), testColumn) mustBe ParsedValue.Valid(LocalDate.of(2025, 3, 2))
      ParsedRowReader.readDate(row(Some("02 03 2025")), testColumn) mustBe ParsedValue.Valid(LocalDate.of(2025, 3, 2))
    }

    "return Missing for blank values" in {
      ParsedRowReader.readDate(row(Some(" ")), testColumn) mustBe ParsedValue.Missing
    }

    "return Invalid for non-date values" in {
      ParsedRowReader.readDate(row(Some("not a date")), testColumn) mustBe ParsedValue.Invalid("not a date", "not a valid date")
      ParsedRowReader.readDate(row(Some("32/13/2025")), testColumn) mustBe ParsedValue.Invalid("32/13/2025", "not a valid date")
    }
  }

  private def row(value: Option[String]): ParsedRow =
    ParsedRow(
      rowNumber = 3,
      cells = value.toSeq.map(v => ParsedCell(testColumn, v))
    )
}