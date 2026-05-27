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
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.*
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.{StcFileValidationService, StcUploadParsingService, StcUploadProcessingService}

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

  private val headers = Seq("nameOfSeller")

  private val parsedRow = ParsedRow(
    rowNumber = 4,
    cells = Seq(
      ParsedCell(1, "Seller Ltd")
    )
  )

  private val parsedFile = ParsedFile(
    fileName = "test.csv",
    mimeType = "text/csv",
    headers = headers,
    rows = Seq(parsedRow)
  )

  private val validationResponse = StcFileValidationResponse(
    rows = Seq(
      ValidatedStcRow(
        parsedRow = ParsedStcRow(
          rowNumber = 4,
          sellerName = Some("Seller 1"),
          sellerAddressInUK = None,
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
          chargingPoint = None,
          taxRate = None,
          whatTypeOfSecurities = None,
          typeOfShares = None,
          securitiesQuantity = None,
          amountPaidForSecurities = None,
          totalMarketValue = None,
          minSharePrice = None,
          maxSharePrice = None,
          sharePurchaseReason = None,
          purchaseForCancellation = None
        ),
        validationErrors = Seq.empty
      )
    )
  )

  "StcUploadProcessingService.process" must {

    "parse then validate the uploaded file" in {
      when(stcUploadParsingService.parse(uploadedFile))
        .thenReturn(Right(parsedFile))

      when(stcFileValidationService.validate(parsedFile.rows, parsedFile.headers))
        .thenReturn(validationResponse)

      service.process(uploadedFile) mustBe Right(validationResponse)
    }

    "return parse errors without validating" in {
      when(stcUploadParsingService.parse(uploadedFile))
        .thenReturn(Left(FileParseError.EmptyFile))

      service.process(uploadedFile) mustBe Left(FileParseError.EmptyFile)
    }
  }
}