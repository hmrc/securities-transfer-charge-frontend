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

import base.SpecBase
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.securitiestransferchargefrontend.config.FileUploadConfig
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.*
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.*

class StcFileValidationServiceSpec extends SpecBase {

  private val stcRowValidationService = mock[StcRowValidationService]
  private val mockConfig = mock[FileUploadConfig]

  private val service = new StcFileValidationService(mockConfig, stcRowValidationService)

  private val headers: Seq[String] =
    Seq("sellerName")

  private val parsedRow1 =
    ParsedRow(
      rowNumber = 3,
      cells = Seq(ParsedCell(StcUploadColumn.sellerName, "Seller 1"))
    )

  private val parsedRow2 =
    ParsedRow(
      rowNumber = 4,
      cells = Seq(ParsedCell(StcUploadColumn.sellerName, ""))
    )

  private val validatedRow1 =
    ValidatedStcRow(
      parsedRow = ParsedStcRow(
        rowNumber = 3,
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

  private val validatedRow2 =
    ValidatedStcRow(
      parsedRow = ParsedStcRow(
        rowNumber = 4,
        sellerName = None,
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
      validationErrors = Seq(
        StcRowValidationError(
          rowNumber = 4,
          fieldName = "sellerName",
          columnIndex = StcUploadColumn.sellerName,
          message = "sellerName is required",
          blocking = true
        )
      )
    )

  "StcFileValidationService.validate" - {

    "validate each parsed row and return a file validation response" in {

      when(mockConfig.maxErrorsAllowed).thenReturn(25)

      when(
        stcRowValidationService.validateAll(
          Seq(parsedRow1, parsedRow2),
          headers, affinityGroupKeyInd
        )
      ).thenReturn(Seq(validatedRow1, validatedRow2))

      val result = service.validate(Seq(parsedRow1, parsedRow2), headers, affinityGroupKeyInd)

      result.rows mustBe Seq(validatedRow1, validatedRow2)
      result.maxErrorsAllowed mustBe 25
      result.hasErrors mustBe true
      result.hasBlockingErrors mustBe true
      result.validRows mustBe Seq(validatedRow1.parsedRow)
    }
  }
}