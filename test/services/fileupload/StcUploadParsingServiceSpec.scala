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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.EitherValues
import org.scalatest.matchers.must.Matchers.mustBe
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.*
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.{FileParsingService, StcUploadParsingService}

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class StcUploadParsingServiceSpec extends AnyWordSpec with Matchers with EitherValues with MockitoSugar {

  private def hashRow(cells: Seq[String]): String = {
    val normalisedString = cells.map(_.trim).mkString("|")
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(normalisedString.getBytes(StandardCharsets.UTF_8))
    hashBytes.map("%02x".format(_)).mkString
  }

  private val validRow1Cells = Seq("Header1", "Header2")
  private val validRow2Cells = Seq("Question 1", "Question 2")
  private val validRow3Cells = Seq("Hint 1", "Hint 2")

  private val validRow2 = ParsedRow(2, validRow2Cells.zipWithIndex.map { case (v, i) => ParsedCell(i, v) })
  private val validRow3 = ParsedRow(3, validRow3Cells.zipWithIndex.map { case (v, i) => ParsedCell(i, v) })

  private val fileUploadConfig = TestFileUploadConfig.config(
    row1Hash = hashRow(validRow1Cells),
    row2Hash = hashRow(validRow2Cells),
    row3Hash = hashRow(validRow3Cells)
  )

  private val fileParsingService = mock[FileParsingService]

  private val service = new StcUploadParsingService(
    fileUploadConfig = fileUploadConfig,
    fileParsingService = fileParsingService
  )

  private val uploadedFile = UploadedFile(
    fileName = "test.xlsx",
    mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    inputStream = new ByteArrayInputStream("irrelevant".getBytes(StandardCharsets.UTF_8))
  )

  "parse" should {

    "skip template rows before firstDataRow and drop completely empty rows" in {
      val dataRow = ParsedRow(
        4,
        Seq(
          ParsedCell(1, "10 Downing Street"),
          ParsedCell(7, "Bob Seller")
        )
      )

      val emptyDataRow = ParsedRow(
        5,
        Seq(
          ParsedCell(1, ""),
          ParsedCell(7, " ")
        )
      )

      val parsedFile = ParsedFile(
        fileName = "test.xlsx",
        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        headers = validRow1Cells,
        rows = Seq(validRow2, validRow3, dataRow, emptyDataRow)
      )

      when(fileParsingService.parse(any[UploadedFile])).thenReturn(Right(parsedFile))

      val result = service.parse(uploadedFile)

      result.value.rows mustBe Seq(dataRow)
      verify(fileParsingService).parse(uploadedFile)
    }

    "keep multiple non-empty data rows from firstDataRow onwards" in {
      val dataRow1 = ParsedRow(4, Seq(ParsedCell(7, "Seller 1")))
      val dataRow2 = ParsedRow(5, Seq(ParsedCell(7, "Seller 2")))

      val parsedFile = ParsedFile(
        "test.xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        headers = validRow1Cells,
        rows = Seq(
          validRow2,
          validRow3,
          dataRow1,
          dataRow2
        )
      )

      when(fileParsingService.parse(any[UploadedFile])).thenReturn(Right(parsedFile))

      val result = service.parse(uploadedFile)

      result.value.rows mustBe Seq(dataRow1, dataRow2)
    }

    "return EmptyFile when there are no data rows after filtering" in {
      val parsedFile = ParsedFile(
        "test.xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        headers = validRow1Cells,
        rows = Seq(
          validRow2,
          validRow3,
          ParsedRow(4, Seq(ParsedCell(1, ""))),
          ParsedRow(5, Seq(ParsedCell(1, " ")))
        )
      )

      when(fileParsingService.parse(any[UploadedFile])).thenReturn(Right(parsedFile))

      service.parse(uploadedFile) mustBe Left(FileParseError.EmptyFile)
    }

    "return EmptyFile when the parsed file contains only template rows" in {
      val parsedFile = ParsedFile(
        "test.xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        headers = validRow1Cells,
        rows = Seq(
          validRow2,
          validRow3
        )
      )

      when(fileParsingService.parse(any[UploadedFile])).thenReturn(Right(parsedFile))

      service.parse(uploadedFile) mustBe Left(FileParseError.EmptyFile)
    }

    "return InvalidTemplate when row 1 (headers) hash does not match" in {
      val invalidHeaders = Seq("InvalidHeader1", "Header2")

      val parsedFile = ParsedFile(
        "test.xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        headers = invalidHeaders,
        rows = Seq(validRow2, validRow3, ParsedRow(4, Seq(ParsedCell(1, "Data"))))
      )

      when(fileParsingService.parse(any[UploadedFile])).thenReturn(Right(parsedFile))

      service.parse(uploadedFile) mustBe Left(FileParseError.InvalidTemplate)
    }

    "return InvalidTemplate when row 2 hash does not match" in {
      val invalidRow2 = ParsedRow(2, Seq(ParsedCell(0, "Invalid Question"), ParsedCell(1, "Question 2")))

      val parsedFile = ParsedFile(
        "test.xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        headers = validRow1Cells,
        rows = Seq(invalidRow2, validRow3, ParsedRow(4, Seq(ParsedCell(1, "Data"))))
      )

      when(fileParsingService.parse(any[UploadedFile])).thenReturn(Right(parsedFile))

      service.parse(uploadedFile) mustBe Left(FileParseError.InvalidTemplate)
    }

    "return InvalidTemplate when row 3 hash does not match" in {
      val invalidRow3 = ParsedRow(3, Seq(ParsedCell(0, "Invalid Hint"), ParsedCell(1, "Hint 2")))

      val parsedFile = ParsedFile(
        "test.xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        headers = validRow1Cells,
        rows = Seq(validRow2, invalidRow3, ParsedRow(4, Seq(ParsedCell(1, "Data"))))
      )

      when(fileParsingService.parse(any[UploadedFile])).thenReturn(Right(parsedFile))

      service.parse(uploadedFile) mustBe Left(FileParseError.InvalidTemplate)
    }

    "propagate file parsing errors" in {
      when(fileParsingService.parse(any[UploadedFile]))
        .thenReturn(Left(FileParseError.InvalidXlsx("broken workbook")))

      service.parse(uploadedFile) mustBe Left(FileParseError.InvalidXlsx("broken workbook"))
    }
  }
}