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
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.{StcFileValidationService, StcRowValidationService, StcUploadColumn}

class StcFileValidationServiceSpec extends AnyWordSpec with Matchers with MockitoSugar {

  private val stcRowValidationService = mock[StcRowValidationService]
  private val service = new StcFileValidationService(stcRowValidationService)

  private val parsedRow1 = ParsedRow(
    rowNumber = 3,
    cells = Seq(ParsedCell(StcUploadColumn.sellerName, "Seller 1"))
  )

  private val parsedRow2 = ParsedRow(
    rowNumber = 4,
    cells = Seq(ParsedCell(StcUploadColumn.sellerName, ""))
  )

  private val validatedRow1 = ValidatedStcRow(
    parsedRow = ParsedStcRow(
      rowNumber = 3,
      sellerName = ParsedValue.Valid("Seller 1"),
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

  private val validatedRow2 = ValidatedStcRow(
    parsedRow = ParsedStcRow(
      rowNumber = 4,
      sellerName = ParsedValue.Missing,
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

  "StcFileValidationService.validate" must {

    "validate each parsed row and return a file validation response" in {
      when(stcRowValidationService.validate(parsedRow1)).thenReturn(validatedRow1)
      when(stcRowValidationService.validate(parsedRow2)).thenReturn(validatedRow2)

      val result = service.validate(Seq(parsedRow1, parsedRow2))

      result.rows mustBe Seq(validatedRow1, validatedRow2)
      result.hasErrors mustBe true
      result.hasBlockingErrors mustBe true
      result.validRows mustBe Seq(validatedRow1.parsedRow)
    }
  }
}