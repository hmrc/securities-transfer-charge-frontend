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
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import uk.gov.hmrc.securitiestransferchargefrontend.config.FileUploadConfig
import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.{ParsedCell, ParsedFile, ParsedRow, ParsedStcRow, UploadedFile}
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.FileParsingService
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcRowMapper
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcUploadParsingService

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.time.LocalDate

class StcUploadParsingServiceSpec extends AnyWordSpec with Matchers with EitherValues with MockitoSugar {

  private val fileUploadConfig = new FileUploadConfig(
    Configuration(
      "file-upload.max-rows" -> 1000,
      "file-upload.xlsx.expected-worksheet" -> "Sheet1",
      "file-upload.first-data-row" -> 3
    )
  )

  private val fileParsingService = mock[FileParsingService]
  private val stcRowMapper       = mock[StcRowMapper]

  private val service = new StcUploadParsingService(
    fileUploadConfig,
    fileParsingService,
    stcRowMapper
  )

  private val uploadedFile = UploadedFile(
    fileName = "test.xlsx",
    mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    inputStream = new ByteArrayInputStream("irrelevant".getBytes(StandardCharsets.UTF_8))
  )

  "parse" should {

    "skip rows before firstDataRow, drop completely empty rows, and map the remaining rows" in {
      val headerRow = ParsedRow(
        rowNumber = 1,
        cells = Seq(ParsedCell(1, "Your address - line 1"))
      )

      val guidanceRow = ParsedRow(
        rowNumber = 2,
        cells = Seq(ParsedCell(1, "Enter the first line of your address"))
      )

      val dataRow = ParsedRow(
        rowNumber = 3,
        cells = Seq(
          ParsedCell(1, "10 Downing Street"),
          ParsedCell(7, "Bob Seller"),
          ParsedCell(20, "2026-03-23"),
          ParsedCell(21, "0.5%"),
          ParsedCell(24, "100"),
          ParsedCell(25, "£500"),
          ParsedCell(26, "600")
        )
      )

      val emptyDataRow = ParsedRow(
        rowNumber = 4,
        cells = Seq(
          ParsedCell(1, ""),
          ParsedCell(7, " "),
          ParsedCell(20, "")
        )
      )

      val mappedRow = ParsedStcRow(
        rowNumber = 3,
        addressLine1 = Some("10 Downing Street"),
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        postcode = None,
        country = None,
        sellerName = Some("Bob Seller"),
        sellerAddressInUk = None,
        sellerAddressLine1 = None,
        sellerAddressLine2 = None,
        sellerAddressLine3 = None,
        sellerAddressLine4 = None,
        sellerPostcode = None,
        sellerCountry = None,
        connectedPersons = None,
        applyingForRelief = None,
        whatReliefAreYouApplyingFor = None,
        securitiesTarget = None,
        companyRegistrationNumber = None,
        chargingPoint = Some(LocalDate.of(2026, 3, 23)),
        taxRate = Some(BigDecimal("0.5")),
        whatTypeOfSecurities = None,
        otherSecuritiesType = None,
        securitiesQuantity = Some(BigDecimal("100")),
        amountPaidForSecurities = Some(BigDecimal("500")),
        totalMarketValue = Some(BigDecimal("600"))
      )

      val parsedFile = ParsedFile(
        fileName = "test.xlsx",
        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        rows = Seq(headerRow, guidanceRow, dataRow, emptyDataRow)
      )

      when(fileParsingService.parse(any[UploadedFile])).thenReturn(Right(parsedFile))
      when(stcRowMapper.map(dataRow)).thenReturn(mappedRow)

      val result = service.parse(uploadedFile).value

      result shouldBe Seq(mappedRow)

      verify(fileParsingService).parse(uploadedFile)
      verify(stcRowMapper).map(dataRow)
    }

    "return an empty sequence when there are no data rows after filtering" in {
      val parsedFile = ParsedFile(
        fileName = "test.xlsx",
        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        rows = Seq(
          ParsedRow(1, Seq(ParsedCell(1, "Header"))),
          ParsedRow(2, Seq(ParsedCell(1, "Guidance"))),
          ParsedRow(3, Seq(ParsedCell(1, "")))
        )
      )

      when(fileParsingService.parse(any[UploadedFile])).thenReturn(Right(parsedFile))

      service.parse(uploadedFile).value shouldBe Seq.empty
    }
  }
}