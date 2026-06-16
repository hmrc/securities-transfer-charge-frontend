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

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.when
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{FileParseError, ParsedCell, ParsedRow, UploadedFile}
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.ExcelFileParser

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.time.{LocalDate, ZoneId}
import java.util.Date

class ExcelFileParserSpec extends AnyWordSpec with Matchers with EitherValues with MockitoSugar {

  private val maxColumns = 27

  private val mockAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.rowCacheSize).thenReturn(100)
  when(mockAppConfig.bufferSizeBytes).thenReturn(4096)

  private val parser = new ExcelFileParser(TestFileUploadConfig.config(), mockAppConfig)

  private def workbookBytes(build: XSSFWorkbook => Unit): Array[Byte] = {
    val workbook = new XSSFWorkbook()
    try {
      build(workbook)
      val out = new ByteArrayOutputStream()
      workbook.write(out)
      out.toByteArray
    } finally {
      workbook.close()
    }
  }

  private def uploadedFile(bytes: Array[Byte]): UploadedFile =
    UploadedFile(
      fileName = "test.xlsx",
      mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      inputStream = new ByteArrayInputStream(bytes)
    )

  private def blankCells(fromIndex: Int): Seq[ParsedCell] =
    (fromIndex until maxColumns).map(index => ParsedCell(index, ""))

  private def blankHeaders(fromIndex: Int): Seq[String] =
    (fromIndex until maxColumns).map(_ => "")

  private def parseFully(bytes: Array[Byte]): Either[FileParseError, (Seq[String], Seq[ParsedRow])] =
    parser.withParsedStream(uploadedFile(bytes), maxColumns) { (headers, rowStream) =>
      Right((headers, rowStream.toList))
    }

  "withParsedStream" should {

    "parse string, numeric, boolean and date cells" in {
      val bytes = workbookBytes { workbook =>
        val sheet = workbook.createSheet("Sheet1")

        val header = sheet.createRow(0)
        header.createCell(0).setCellValue("name")
        header.createCell(1).setCellValue("amount")
        header.createCell(2).setCellValue("flag")
        header.createCell(3).setCellValue("date")

        val row = sheet.createRow(1)
        row.createCell(0).setCellValue("Bob")
        row.createCell(1).setCellValue(123.45)
        row.createCell(2).setCellValue(true)

        val dateCell = row.createCell(3)
        val localDate = LocalDate.of(2026, 3, 23)
        val instant = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant
        dateCell.setCellValue(Date.from(instant))

        val dateStyle = workbook.createCellStyle()
        val dataFormat = workbook.getCreationHelper.createDataFormat()
        dateStyle.setDataFormat(dataFormat.getFormat("yyyy-mm-dd"))
        dateCell.setCellStyle(dateStyle)
      }

      val (headers, rows) = parseFully(bytes).value

      headers shouldBe Seq(
        "name", "amount", "flag", "date"
      ) ++ blankHeaders(4)

      rows.size shouldBe 1

      rows.head.rowNumber shouldBe 2

      rows.head.cells shouldBe Seq(
        ParsedCell(0, "Bob"),
        ParsedCell(1, "123.45"),
        ParsedCell(2, "true"),
        ParsedCell(3, "2026-03-23")
      ) ++ blankCells(4)
    }

    "pad missing trailing columns with blank cells up to the configured maximum" in {
      val bytes = workbookBytes { workbook =>
        val sheet = workbook.createSheet("Sheet1")

        val header = sheet.createRow(0)
        header.createCell(0).setCellValue("name")

        val row = sheet.createRow(1)
        row.createCell(0).setCellValue("Bob")
      }

      val (headers, rows) = parseFully(bytes).value

      headers shouldBe Seq("name") ++ blankHeaders(1)

      rows.head.cells shouldBe Seq(
        ParsedCell(0, "Bob")
      ) ++ blankCells(1)
    }

    "ignore columns beyond the first 27" in {
      val bytes = workbookBytes { workbook =>
        val sheet = workbook.createSheet("Sheet1")

        val header = sheet.createRow(0)
        val row = sheet.createRow(1)

        (0 until 30).foreach { index =>
          header.createCell(index).setCellValue(s"h${index + 1}")
          row.createCell(index).setCellValue(s"value${index + 1}")
        }
      }

      val (headers, rows) = parseFully(bytes).value

      headers should have size maxColumns
      rows.size shouldBe 1
      rows.head.cells should have size maxColumns

      rows.head.cells shouldBe (0 until maxColumns).map { index =>
        ParsedCell(index, s"value${index + 1}")
      }
    }
  }
}