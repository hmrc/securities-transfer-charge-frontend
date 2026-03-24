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

import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.FileParseError.RowLimitExceeded
import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.{ParsedCell, UploadedFile}
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.CsvFileParser
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class CsvFileParserSpec extends AnyWordSpec with Matchers with EitherValues {

  private val parser = new CsvFileParser(TestFileUploadConfig.config())

  private def uploadedFile(csv: String): UploadedFile =
    UploadedFile(
      fileName = "test.csv",
      mimeType = "text/csv",
      inputStream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))
    )

  "parse" should {

    "parse a simple CSV file into ParsedFile" in {
      val csv =
        """name,amount,date
          |Bill,100,2026-03-23
          |Bob,200,2026-03-24
          |""".stripMargin

      val result = parser.parse(uploadedFile(csv)).value

      result.fileName shouldBe "test.csv"
      result.mimeType shouldBe "text/csv"
      result.rows.size shouldBe 3

      result.rows.head.rowNumber shouldBe 1
      result.rows.head.cells shouldBe Seq(
        ParsedCell(0, "name"),
        ParsedCell(1, "amount"),
        ParsedCell(2, "date")
      )

      result.rows(1).rowNumber shouldBe 2
      result.rows(1).cells shouldBe Seq(
        ParsedCell(0, "Bill"),
        ParsedCell(1, "100"),
        ParsedCell(2, "2026-03-23")
      )
    }

    "handle quoted commas correctly" in {
      val csv =
        """name,address
          |"Bill","1, High Street"
          |""".stripMargin

      val result = parser.parse(uploadedFile(csv)).value

      result.rows(1).cells shouldBe Seq(
        ParsedCell(0, "Bill"),
        ParsedCell(1, "1, High Street")
      )
    }

    "trim surrounding whitespace from values" in {
      val csv =
        """name,amount
          | Bill , 100
          |""".stripMargin

      val result = parser.parse(uploadedFile(csv)).value

      result.rows(1).cells shouldBe Seq(
        ParsedCell(0, "Bill"),
        ParsedCell(1, "100")
      )
    }

    "return RowLimitExceeded when the row count exceeds the configured maximum" in {
      val parser = new CsvFileParser(TestFileUploadConfig.config(maxRows = 1))

      val csv =
        """name
          |Bill
          |Bob
          |""".stripMargin

      parser.parse(uploadedFile(csv)).left.value shouldBe RowLimitExceeded(3, 1)
    }
  }
}