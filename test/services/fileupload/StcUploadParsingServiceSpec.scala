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

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.EitherValues
import org.scalatest.matchers.must.Matchers.mustBe
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.securitiestransferchargefrontend.config.{FileUploadConfig, TemplateDefinition}
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType.{SH03, STF}
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

  private val fileUploadConfig = mock[FileUploadConfig]
  when(fileUploadConfig.firstDataRow).thenReturn(4)
  when(fileUploadConfig.template(any[String], any[JourneyType])).thenReturn(
    Some(TemplateDefinition(27, hashRow(validRow1Cells ++ validRow2Cells ++ validRow3Cells)))
  )

  private val uploadedFile = UploadedFile(
    fileName = "test.xlsx",
    mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    inputStream = new ByteArrayInputStream("irrelevant".getBytes(StandardCharsets.UTF_8))
  )

  private val testAffinityGroup = "individual"

  trait Setup {
    val fileParsingService: FileParsingService = mock[FileParsingService]

    val service = new StcUploadParsingService(
      fileUploadConfig = fileUploadConfig,
      fileParsingService = fileParsingService
    )

    def mockStream(headers: Seq[String], rows: Seq[ParsedRow]): Unit = {
      when(fileParsingService.withParsedStream[Seq[ParsedRow]](any[UploadedFile], any[Int])(any()))
        .thenAnswer { invocation =>
          val block = invocation.getArgument(2).asInstanceOf[(Seq[String], Iterator[ParsedRow]) => Either[FileParseError, Seq[ParsedRow]]]
          block(headers, rows.iterator)
        }
    }
  }

  "withVerifiedTemplateStream" should {

    "skip template rows before firstDataRow and drop completely empty rows" in new Setup {
      val dataRow = ParsedRow(4, Seq(ParsedCell(1, "10 Downing Street"), ParsedCell(7, "Bob Seller")))
      val emptyDataRow = ParsedRow(5, Seq(ParsedCell(1, ""), ParsedCell(7, " ")))

      mockStream(validRow1Cells, Seq(validRow2, validRow3, dataRow, emptyDataRow))

      val result: Either[FileParseError, List[ParsedRow]] = service.withVerifiedTemplateStream(uploadedFile, testAffinityGroup, STF) { (_, stream) =>
        Right(stream.toList)
      }

      result.value mustBe Seq(dataRow)
      verify(fileParsingService).withParsedStream[Seq[ParsedRow]](any[UploadedFile], eqTo(27))(any())
    }

    "keep multiple non-empty data rows from firstDataRow onwards" in new Setup {
      val dataRow1 = ParsedRow(4, Seq(ParsedCell(7, "Seller 1")))
      val dataRow2 = ParsedRow(5, Seq(ParsedCell(7, "Seller 2")))

      mockStream(validRow1Cells, Seq(validRow2, validRow3, dataRow1, dataRow2))

      val result: Either[FileParseError, List[ParsedRow]] = service.withVerifiedTemplateStream(uploadedFile, testAffinityGroup, STF) { (_, stream) =>
        Right(stream.toList)
      }

      result.value mustBe Seq(dataRow1, dataRow2)
    }

    "return EmptyFile when there are no data rows after filtering" in new Setup {
      mockStream(validRow1Cells, Seq(validRow2, validRow3, ParsedRow(4, Seq(ParsedCell(1, ""))), ParsedRow(5, Seq(ParsedCell(1, " ")))))

      val result: Either[FileParseError, List[ParsedRow]] = service.withVerifiedTemplateStream(uploadedFile, testAffinityGroup, STF) { (_, stream) => Right(stream.toList) }

      result mustBe Left(FileParseError.EmptyFile)
    }

    "return EmptyFile when the parsed file contains only template rows" in new Setup {
      mockStream(validRow1Cells, Seq(validRow2, validRow3))

      val result: Either[FileParseError, List[ParsedRow]] = service.withVerifiedTemplateStream(uploadedFile, testAffinityGroup, STF) { (_, stream) => Right(stream.toList) }

      result mustBe Left(FileParseError.EmptyFile)
    }

    "return InvalidTemplate when row 1 (headers) hash does not match" in new Setup {
      val invalidHeaders: Seq[String] = Seq("InvalidHeader1", "Header2")

      mockStream(invalidHeaders, Seq(validRow2, validRow3, ParsedRow(4, Seq(ParsedCell(1, "Data")))))

      val result: Either[FileParseError, List[ParsedRow]] = service.withVerifiedTemplateStream(uploadedFile, testAffinityGroup, STF) { (_, stream) => Right(stream.toList) }

      result mustBe Left(FileParseError.InvalidTemplate)
    }

    "return InvalidTemplate when row 2 hash does not match" in new Setup {
      val invalidRow2 = ParsedRow(2, Seq(ParsedCell(0, "Invalid Question"), ParsedCell(1, "Question 2")))

      mockStream(validRow1Cells, Seq(invalidRow2, validRow3, ParsedRow(4, Seq(ParsedCell(1, "Data")))))

      val result: Either[FileParseError, List[ParsedRow]] = service.withVerifiedTemplateStream(uploadedFile, testAffinityGroup, STF) { (_, stream) => Right(stream.toList) }

      result mustBe Left(FileParseError.InvalidTemplate)
    }

    "return InvalidTemplate when row 3 hash does not match" in new Setup {
      val invalidRow3 = ParsedRow(3, Seq(ParsedCell(0, "Invalid Hint"), ParsedCell(1, "Hint 2")))

      mockStream(validRow1Cells, Seq(validRow2, invalidRow3, ParsedRow(4, Seq(ParsedCell(1, "Data")))))

      val result: Either[FileParseError, List[ParsedRow]] = service.withVerifiedTemplateStream(uploadedFile, testAffinityGroup, STF) { (_, stream) => Right(stream.toList) }

      result mustBe Left(FileParseError.InvalidTemplate)
    }

    "return InvalidTemplate when no template configuration is found for the given journey keys" in new Setup {
      when(fileUploadConfig.template(eqTo(testAffinityGroup), eqTo(STF))).thenReturn(None)

      mockStream(validRow1Cells, Seq(validRow2, validRow3, ParsedRow(4, Seq(ParsedCell(1, "Data")))))

      val result: Either[FileParseError, List[ParsedRow]] = service.withVerifiedTemplateStream(uploadedFile, testAffinityGroup, STF) { (_, stream) => Right(stream.toList) }

      result mustBe Left(FileParseError.InvalidTemplate)
    }

    "propagate file parsing errors" in new Setup {
      
      when(fileUploadConfig.template(eqTo(testAffinityGroup), eqTo(STF))).thenReturn(
        Some(TemplateDefinition(27, hashRow(validRow1Cells ++ validRow2Cells ++ validRow3Cells)))
      )
      
      when(fileParsingService.withParsedStream[Seq[ParsedRow]](any[UploadedFile], any[Int])(any()))
        .thenReturn(Left(FileParseError.InvalidXlsx("broken workbook")))

      val result: Either[FileParseError, List[ParsedRow]] = service.withVerifiedTemplateStream(uploadedFile, testAffinityGroup, STF) { (_, stream) => Right(stream.toList) }

      result mustBe Left(FileParseError.InvalidXlsx("broken workbook"))
    }

    "lookup config properties and pass expected columns dynamically to the parser selector channel" in new Setup {
      val mockConfig: FileUploadConfig = mock[FileUploadConfig]
      val mockParser: FileParsingService = mock[FileParsingService]
      val currentService = new StcUploadParsingService(mockConfig, mockParser)

      when(mockConfig.firstDataRow).thenReturn(4)

      when(mockConfig.template(eqTo("individual"), eqTo(STF))).thenReturn(
        Some(TemplateDefinition(expectedColumns = 27, signature = "ind-hash"))
      )
      when(mockConfig.template(eqTo("org"), eqTo(STF))).thenReturn(
        Some(TemplateDefinition(expectedColumns = 21, signature = "org-hash"))
      )

      when(mockConfig.template(eqTo("agent"), eqTo(STF))).thenReturn(
        Some(TemplateDefinition(expectedColumns = 29, signature = "agent-hash"))
      )

      when(mockConfig.template(eqTo("agent"), eqTo(SH03))).thenReturn(
        Some(TemplateDefinition(expectedColumns = 33, signature = "sh03-agent-hash"))
      )

      when(mockParser.withParsedStream[Unit](any(), any())(any())).thenReturn(Right(()))

      currentService.withVerifiedTemplateStream(uploadedFile, "individual", STF) { (_, _) => Right(()) }
      verify(mockParser).withParsedStream[Unit](eqTo(uploadedFile), eqTo(27))(any())

      currentService.withVerifiedTemplateStream(uploadedFile, "org", STF) { (_, _) => Right(()) }
      verify(mockParser).withParsedStream[Unit](eqTo(uploadedFile), eqTo(21))(any())

      currentService.withVerifiedTemplateStream(uploadedFile, "agent", STF) { (_, _) => Right(()) }
      verify(mockParser).withParsedStream[Unit](eqTo(uploadedFile), eqTo(29))(any())

      currentService.withVerifiedTemplateStream(uploadedFile, "agent", SH03) { (_, _) => Right(()) }
      verify(mockParser).withParsedStream[Unit](eqTo(uploadedFile), eqTo(33))(any())
    }
  }
}