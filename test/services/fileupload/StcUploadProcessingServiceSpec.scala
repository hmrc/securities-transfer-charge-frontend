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

import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.*
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.{StcFileValidationService, StcUploadColumn, StcUploadParsingService, StcUploadProcessingService}

import java.io.ByteArrayInputStream

class StcUploadProcessingServiceSpec extends AnyWordSpec with Matchers with MockitoSugar {

  private val stcUploadParsingService = mock[StcUploadParsingService]
  private val stcFileValidationService = mock[StcFileValidationService]

  private val service =
    new StcUploadProcessingService(
      stcUploadParsingService = stcUploadParsingService,
      stcFileValidationService = stcFileValidationService
    )

  private val uploadedFile = UploadedFile(
    fileName = "test.csv",
    mimeType = "text/csv",
    inputStream = new ByteArrayInputStream("data".getBytes)
  )

  private val parsedRow = ParsedRow(
    rowNumber = 3,
    cells = Seq(
      ParsedCell(StcUploadColumn.sellerName, "Seller Ltd")
    )
  )

  private val validationResponse = StcFileValidationResponse(
    rows = Seq(
      ValidatedStcRow(
        parsedRow = ParsedStcRow(
          rowNumber = 3,
          sellerName = ParsedValue.Valid("Seller Ltd"),
          sellerAddressInUk = ParsedValue.Missing,
          sellerAddressLine1 = ParsedValue.Missing,
          sellerAddressLine2 = ParsedValue.Missing,
          sellerAddressLine3 = ParsedValue.Missing,
          sellerAddressLine4 = ParsedValue.Missing,
          sellerPostcode = ParsedValue.Missing,
          sellerCountry = ParsedValue.Missing,
          connectedPersons = ParsedValue.Missing,
          applyingForRelief = ParsedValue.Missing,
          whatReliefAreYouApplyingFor = ParsedValue.Missing,
          securitiesTarget = ParsedValue.Missing,
          companyRegistrationNumber = ParsedValue.Missing,
          chargingPoint = ParsedValue.Missing,
          taxRate = ParsedValue.Missing,
          whatTypeOfSecurities = ParsedValue.Missing,
          typeOfShares = ParsedValue.Missing,
          securitiesQuantity = ParsedValue.Missing,
          amountPaidForSecurities = ParsedValue.Missing,
          totalMarketValue = ParsedValue.Missing
        ),
        validationErrors = Seq.empty
      )
    )
  )

  "StcUploadProcessingService.process" must {

    "parse then validate the uploaded file" in {
      when(stcUploadParsingService.parse(uploadedFile)).thenReturn(Right(Seq(parsedRow)))
      when(stcFileValidationService.validate(Seq(parsedRow))).thenReturn(validationResponse)

      service.process(uploadedFile) mustBe Right(validationResponse)
    }

    "return parse errors without validating" in {
      when(stcUploadParsingService.parse(uploadedFile)).thenReturn(Left(FileParseError.EmptyFile))

      service.process(uploadedFile) mustBe Left(FileParseError.EmptyFile)
    }
  }
}