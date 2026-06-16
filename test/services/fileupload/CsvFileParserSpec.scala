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
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{FileParseError, ParsedCell, ParsedRow, UploadedFile}
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.CsvFileParser

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class CsvFileParserSpec extends AnyWordSpec with Matchers with EitherValues {

  private val maxColumns = 27

  private val parser = new CsvFileParser()

  private def uploadedFile(csv: String): UploadedFile =
    UploadedFile(
      fileName = "test.csv",
      mimeType = "text/csv",
      inputStream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))
    )

  private def blankCells(fromIndex: Int): Seq[ParsedCell] =
    (fromIndex until maxColumns).map(index => ParsedCell(index, ""))

  private def parseFully(csv: String): Either[FileParseError, (Seq[String], Seq[ParsedRow])] =
    parser.withParsedStream(uploadedFile(csv), maxColumns) { (headers, rowStream) =>
      Right((headers, rowStream.toList))
    }

  "withParsedStream" should {

    "stream the headers and rows of a simple CSV file" in {
      val csv =
        """name,amount,date
          |Bill,100,2026-03-23
          |Bob,200,2026-03-24
          |""".stripMargin

      val (headers, rows) = parseFully(csv).value

      headers shouldBe Seq("name", "amount", "date") ++
        Seq.fill(maxColumns - 3)("")

      rows.size shouldBe 2

      rows.head.rowNumber shouldBe 2
      rows.head.cells shouldBe Seq(
        ParsedCell(0, "Bill"),
        ParsedCell(1, "100"),
        ParsedCell(2, "2026-03-23")
      ) ++ blankCells(fromIndex = 3)

      rows(1).rowNumber shouldBe 3
      rows(1).cells shouldBe Seq(
        ParsedCell(0, "Bob"),
        ParsedCell(1, "200"),
        ParsedCell(2, "2026-03-24")
      ) ++ blankCells(fromIndex = 3)
    }

    "handle quoted commas correctly" in {
      val csv =
        """name,address
          |"Bill","1, High Street"
          |""".stripMargin

      val (_, rows) = parseFully(csv).value

      rows.head.cells shouldBe Seq(
        ParsedCell(0, "Bill"),
        ParsedCell(1, "1, High Street")
      ) ++ blankCells(fromIndex = 2)
    }

    "trim surrounding whitespace from values" in {
      val csv =
        """name,amount
          | Bill , 100
          |""".stripMargin

      val (_, rows) = parseFully(csv).value

      rows.head.cells shouldBe Seq(
        ParsedCell(0, "Bill"),
        ParsedCell(1, "100")
      ) ++ blankCells(fromIndex = 2)
    }

    "pad missing trailing columns with blank cells up to the configured maximum" in {
      val csv =
        """name
          |Bill
          |""".stripMargin

      val (headers, rows) = parseFully(csv).value

      headers shouldBe Seq("name") ++
        Seq.fill(maxColumns - 1)("")

      rows.head.cells shouldBe Seq(
        ParsedCell(0, "Bill")
      ) ++ blankCells(fromIndex = 1)
    }

    "ignore columns beyond the first 27" in {
      val header = (1 to 30).map(i => s"col$i").mkString(",")
      val row    = (1 to 30).map(i => s"value$i").mkString(",")

      val csv =
        s"""$header
           |$row
           |""".stripMargin

      val (headers, rows) = parseFully(csv).value

      headers should have size maxColumns
      rows.head.cells should have size maxColumns

      headers shouldBe (1 to maxColumns).map(i => s"col$i")
      rows.head.cells shouldBe (0 until maxColumns).map { index =>
        ParsedCell(index, s"value${index + 1}")
      }
    }
  }
}