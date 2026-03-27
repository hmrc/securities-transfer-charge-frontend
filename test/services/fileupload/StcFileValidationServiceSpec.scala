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
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.*
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.*

import java.time.LocalDate

class StcFileValidationServiceSpec extends AnyWordSpec with Matchers with MockitoSugar {

  private val stcRowValidationService = mock[StcRowValidationService]
  private val service = new StcFileValidationService(stcRowValidationService)

  private val parsedRow1 = ParsedStcRow(
    rowNumber = 3,
    addressLine1 = ParsedValue.Valid("10 Downing Street"),
    addressLine2 = ParsedValue.Missing,
    addressLine3 = ParsedValue.Missing,
    addressLine4 = ParsedValue.Missing,
    postcode = ParsedValue.Valid("SW1A 2AA"),
    country = ParsedValue.Valid("United Kingdom"),
    sellerName = ParsedValue.Valid("Bob Seller"),
    sellerAddressInUk = ParsedValue.Valid(true),
    sellerAddressLine1 = ParsedValue.Valid("1 Seller Street"),
    sellerAddressLine2 = ParsedValue.Missing,
    sellerAddressLine3 = ParsedValue.Missing,
    sellerAddressLine4 = ParsedValue.Missing,
    sellerPostcode = ParsedValue.Valid("LS1 1AA"),
    sellerCountry = ParsedValue.Valid("United Kingdom"),
    connectedPersons = ParsedValue.Valid(false),
    applyingForRelief = ParsedValue.Valid(false),
    whatReliefAreYouApplyingFor = ParsedValue.Missing,
    securitiesTarget = ParsedValue.Missing,
    companyRegistrationNumber = ParsedValue.Missing,
    chargingPoint = ParsedValue.Valid(LocalDate.of(2026, 3, 23)),
    taxRate = ParsedValue.Valid(BigDecimal("0.5")),
    whatTypeOfSecurities = ParsedValue.Valid("Stock"),
    otherSecuritiesType = ParsedValue.Missing,
    securitiesQuantity = ParsedValue.Valid(BigDecimal("100")),
    amountPaidForSecurities = ParsedValue.Valid(BigDecimal("500")),
    totalMarketValue = ParsedValue.Valid(BigDecimal("600"))
  )

  private val parsedRow2 = parsedRow1.copy(rowNumber = 4)

  "validate" should {

    "validate each row and wrap the results in a StcFileValidationResponse" in {
      val validatedRow1 = ValidatedStcRow(parsedRow1, Seq.empty)
      val validatedRow2 = ValidatedStcRow(
        parsedRow2,
        Seq(StcRowValidationError(4, "sellerName", "sellerName is required", blocking = true))
      )

      when(stcRowValidationService.validate(parsedRow1)).thenReturn(validatedRow1)
      when(stcRowValidationService.validate(parsedRow2)).thenReturn(validatedRow2)

      val result = service.validate(Seq(parsedRow1, parsedRow2))

      result.rows shouldBe Seq(validatedRow1, validatedRow2)
      result.hasBlockingErrors shouldBe true
      result.hasErrors shouldBe true
      result.validRows shouldBe Seq(parsedRow1)
    }
  }
}