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
    addressLine1 = Some("10 Downing Street"),
    addressLine2 = None,
    addressLine3 = None,
    addressLine4 = None,
    postcode = Some("SW1A 2AA"),
    country = Some("United Kingdom"),
    sellerName = Some("Bob Seller"),
    sellerAddressInUk = Some(true),
    sellerAddressLine1 = Some("1 Seller Street"),
    sellerAddressLine2 = None,
    sellerAddressLine3 = None,
    sellerAddressLine4 = None,
    sellerPostcode = Some("LS1 1AA"),
    sellerCountry = Some("United Kingdom"),
    connectedPersons = Some(false),
    applyingForRelief = Some(false),
    whatReliefAreYouApplyingFor = None,
    securitiesTarget = None,
    companyRegistrationNumber = None,
    chargingPoint = Some(LocalDate.of(2026, 3, 23)),
    taxRate = Some(BigDecimal("0.5")),
    whatTypeOfSecurities = Some("Stock"),
    otherSecuritiesType = None,
    securitiesQuantity = Some(BigDecimal("100")),
    amountPaidForSecurities = Some(BigDecimal("500")),
    totalMarketValue = Some(BigDecimal("600"))
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