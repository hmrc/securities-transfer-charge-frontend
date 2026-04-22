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

package models.submission

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.{ParsedStcRow, ParsedValue, ValidatedStcRow}
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf
import java.time.LocalDate

class RowTransformsSpec extends AnyWordSpec with Matchers {

  private val individualData = Individual(
    name = "John Doe",
    address = stf.Address(
      addressLine1 = "10 Downing Street",
      addressLine2 = Some("Westminster"),
      addressLine3 = Some("London"),
      postcode = "SW1A 2AA",
      countryCode = "GBR"
    ),
    phone = "01234567890",
    email = "foo@bar.com",
    nino = "NY054388A"
  )

  private val validatedRow = ValidatedStcRow(
    parsedRow = ParsedStcRow(
      rowNumber = 3,
      addressLine1 = ParsedValue.Valid("10 Downing Street"),
      addressLine2 = ParsedValue.Valid("Westminster"),
      addressLine3 = ParsedValue.Valid("London"),
      addressLine4 = ParsedValue.Missing,
      postcode = ParsedValue.Valid("SW1A 2AA"),
      country = ParsedValue.Valid("United Kingdom"),
      sellerName = ParsedValue.Valid("Bob Seller"),
      sellerAddressInUk = ParsedValue.Valid(true),
      sellerAddressLine1 = ParsedValue.Valid("1 Seller Street"),
      sellerAddressLine2 = ParsedValue.Valid("Seller District"),
      sellerAddressLine3 = ParsedValue.Valid("Seller City"),
      sellerAddressLine4 = ParsedValue.Missing,
      sellerPostcode = ParsedValue.Valid("LS1 1AA"),
      sellerCountry = ParsedValue.Valid("United Kingdom"),
      connectedPersons = ParsedValue.Valid(true),
      applyingForRelief = ParsedValue.Valid(true),
      whatReliefAreYouApplyingFor = ParsedValue.Valid("Group relief"),
      securitiesTarget = ParsedValue.Valid("Example Holdings Ltd"),
      companyRegistrationNumber = ParsedValue.Valid("12345678"),
      chargingPoint = ParsedValue.Valid(LocalDate.of(2026, 3, 23)),
      taxRate = ParsedValue.Valid(BigDecimal("1.5")),
      whatTypeOfSecurities = ParsedValue.Valid("Shares"),
      otherSecuritiesType = ParsedValue.Missing,
      securitiesQuantity = ParsedValue.Valid(BigDecimal("1000")),
      amountPaidForSecurities = ParsedValue.Valid(BigDecimal("5000.25")),
      totalMarketValue = ParsedValue.Valid(BigDecimal("6000"))
    ),
    validationErrors = Seq.empty
  )

  "fromValidatedStcRow" should {

    "transform a validated row into a SingleTransferRequest" in {
      RowTransforms.fromValidatedStcRowToStfRequest(validatedRow, individualData) shouldBe SingleTransferRequest(
        recordId = 3,
        transactionDetails = SingleTransferTransactionDetails(
          transactionType = TransferType.STF,
          reasonForPurchase = None,
          descriptionOfSecurity = "Shares",
          numberOfShares = 1000,
          nominalValue = None,
          marketValue = Some(BigDecimal("6000")),
          qualifyAsTreasuryShares = None,
          maxPricePaid = None,
          minPricePaid = None,
          originalChargingPoint = LocalDate.of(2026, 3, 23),
          considerationActual = BigDecimal("5000.25"),
          isConnectedPartiesTransactions = true,
          companyName = "Example Holdings Ltd",
          companyRegistrationNumber = Some("12345678"),
          reliefClaimedName = Some("Group relief"),
          reliefPercentage = Some(100)
        ),
        contingentDetails = None,
        mainSellerDetails = SingleTransferSellerDetails(
          sellerName = "Bob Seller",
          addr1 = "1 Seller Street",
          addr2 = Some("Seller District"),
          addr3 = Some("Seller City"),
          addr4 = None,
          postcode = "LS1 1AA",
          country = "United Kingdom"
        ),
        otherSellers = None,
        mainBuyerDetails = SingleTransferBuyerDetails(
          buyerName = "John Doe",
          addr1 = "10 Downing Street",
          addr2 = Some("Westminster"),
          addr3 = Some("London"),
          addr4 = None,
          postcode = "SW1A 2AA",
          country = "GBR",
          email = "foo@bar.com",
          uniqueId = Some("NY054388A"),
          taxRate = BuyerTaxRate.OneAndHalfPercent,
          isPLC = None
        ),
        otherBuyers = None,
        agentDetails = None
      )
    }

    "throw when seller postcode is missing" in {
      val rowMissingBuyerPostcode = validatedRow.copy(parsedRow = validatedRow.parsedRow.copy(sellerPostcode = ParsedValue.Missing))

      an[IllegalArgumentException] shouldBe thrownBy {
        RowTransforms.fromValidatedStcRowToStfRequest(rowMissingBuyerPostcode, individualData)
      }
    }

    "throw when seller country is missing" in {
      val rowMissingSellerCountry = validatedRow.copy(parsedRow = validatedRow.parsedRow.copy(sellerCountry = ParsedValue.Missing))

      an[IllegalArgumentException] shouldBe thrownBy {
        RowTransforms.fromValidatedStcRowToStfRequest(rowMissingSellerCountry, individualData)
      }
    }

    "throw when market value is missing for connected parties transactions" in {
      val rowMissingMarketValue = validatedRow.copy(parsedRow = validatedRow.parsedRow.copy(totalMarketValue = ParsedValue.Missing))

      an[IllegalArgumentException] shouldBe thrownBy {
        RowTransforms.fromValidatedStcRowToStfRequest(rowMissingMarketValue, individualData)
      }
    }
  }
}
