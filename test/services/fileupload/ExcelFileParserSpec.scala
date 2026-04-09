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
import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.FileParseError.RowLimitExceeded
import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.{ParsedCell, UploadedFile}
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.ExcelFileParser

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.time.{LocalDate, ZoneId}
import java.util.Date

class ExcelFileParserSpec extends AnyWordSpec with Matchers with EitherValues {

  private val maxColumns = 27
  private val parser = new ExcelFileParser(TestFileUploadConfig.config())

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

  "parse" should {

    "parse string, numeric, boolean and date cells" in {
      val bytes = workbookBytes { workbook =>
        val sheet = workbook.createSheet("Sheet1")
        val row = sheet.createRow(0)

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

      val result = parser.parse(uploadedFile(bytes)).value

      result.rows.size shouldBe 1
      result.rows.head.cells shouldBe Seq(
        ParsedCell(0, "Bob"),
        ParsedCell(1, "123.45"),
        ParsedCell(2, "true"),
        ParsedCell(3, "2026-03-23")
      ) ++ blankCells(fromIndex = 4)
    }

    "pad missing trailing columns with blank cells up to the configured maximum" in {
      val bytes = workbookBytes { workbook =>
        val sheet = workbook.createSheet("Sheet1")
        val row = sheet.createRow(0)

        row.createCell(0).setCellValue("Bob")
      }

      val result = parser.parse(uploadedFile(bytes)).value

      result.rows.head.cells shouldBe Seq(
        ParsedCell(0, "Bob")
      ) ++ blankCells(fromIndex = 1)
    }

    "ignore columns beyond the first 27" in {
      val bytes = workbookBytes { workbook =>
        val sheet = workbook.createSheet("Sheet1")
        val row = sheet.createRow(0)

        (0 until 30).foreach { index =>
          row.createCell(index).setCellValue(s"value${index + 1}")
        }
      }

      val result = parser.parse(uploadedFile(bytes)).value

      result.rows.size shouldBe 1
      result.rows.head.cells should have size maxColumns
      result.rows.head.cells shouldBe (0 until maxColumns).map { index =>
        ParsedCell(index, s"value${index + 1}")
      }
    }

    "return RowLimitExceeded when the row count exceeds the configured maximum" in {
      val bytes = workbookBytes { workbook =>
        val sheet = workbook.createSheet("Sheet1")
        sheet.createRow(0).createCell(0).setCellValue("header")
        sheet.createRow(1).createCell(0).setCellValue("row1")
      }

      val parser = new ExcelFileParser(TestFileUploadConfig.config(maxRows = 1))

      parser.parse(uploadedFile(bytes)).left.value shouldBe RowLimitExceeded(2, 1)
    }
  }
}