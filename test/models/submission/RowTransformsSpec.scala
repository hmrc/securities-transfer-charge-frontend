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
import uk.gov.hmrc.securitiestransferchargefrontend.domain.TransferType
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.bulk.CompanyDetails
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{ParsedStcRow, ParsedValue}
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.*

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

  private val parsedStcRow = ParsedStcRow(
      rowNumber = 3,
      buyerName = Some("Bob buyer"),
      buyerAddressInUK= Some(true),
      buyerAddressLine1= Some("1 Seller Street"),
      buyerAddressLine2= Some("Seller District"),
      buyerAddressLine3= Some("Seller City"),
      buyerAddressLine4= None,
      buyerPostcode= Some("AA1 1AA"),
      buyerCountry= Some("United Kingdom"),
      sellerName = Some("Bob Seller"),
      sellerAddressInUK = Some(true),
      sellerAddressLine1 = Some("1 Seller Street"),
      sellerAddressLine2 = Some("Seller District"),
      sellerAddressLine3 = Some("Seller City"),
      sellerAddressLine4 = None,
      sellerPostcode = Some("LS1 1AA"),
      sellerCountry = Some("United Kingdom"),
      connectedPersons = Some(true),
      applyingForRelief = Some(true),
      whatReliefAreYouApplyingFor = Some("Group relief"),
      securitiesTarget = Some("Example Holdings Ltd"),
      companyRegistrationNumber = Some("12345678"),
      chargingPoint = ParsedValue.Valid(LocalDate.of(2026, 3, 23)),
      taxRate = Some(BigDecimal("1.5")),
      whatTypeOfSecurities = Some("Shares"),
      typeOfShares = Some("Ordinary Shares"),
      securitiesQuantity = Some("1000"),
      amountPaidForSecurities = Some("5000.25"),
      totalMarketValue = Some("6000"),
      minSharePrice = Some("100"), 
      maxSharePrice = Some("1000"), 
      sharePurchaseReason = Some("cancellation"), 
      purchaseForCancellation = Some(true)
    )

  "fromValidatedStcRow" should {

    "transform a validated row into a SingleTransferRequest (STF)" in {
      RowTransforms.fromValidatedStcRowToStfRequest(parsedStcRow, individualData,None) shouldBe SingleTransferRequest(
        recordId = 3,
        transactionDetails = SingleTransferTransactionDetails(
          transactionType = TransferType.STF,
          reasonForPurchase = None,
          descriptionOfSecurity = "Ordinary Shares",
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
        mainSellerDetails = Some(SingleTransferSellerDetails(
          sellerName = "Bob Seller",
          addr1 = "1 Seller Street",
          addr2 = Some("Seller District"),
          addr3 = Some("Seller City"),
          addr4 = None,
          postcode = "LS1 1AA",
          country = "United Kingdom"
        )),
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

    "transform a validated row into a SingleTransferRequest (SH03)" in {
      val row = parsedStcRow.copy(whatTypeOfSecurities = Some("Ordinary"))
      val companyDetails = CompanyDetails(companyName = "Company", companyRegistrationNumber = "12345678")
      val agentReference = "REF-12345"
      RowTransforms.fromValidatedStcRowToSh03Request(row, individualData, companyDetails,Some(agentReference)) shouldBe SingleTransferRequest(
        recordId = 3,
        transactionDetails = SingleTransferTransactionDetails(
          transactionType = TransferType.SH03,
          reasonForPurchase = Some(ReasonForPurchase.Both),
          descriptionOfSecurity = "Ordinary",
          numberOfShares = 1000,
          nominalValue = None,
          marketValue = Some(BigDecimal("6000")),
          qualifyAsTreasuryShares = Some(true),
          maxPricePaid = Some(BigDecimal("1000")),
          minPricePaid = Some(BigDecimal("100")),
          originalChargingPoint = LocalDate.of(2026, 3, 23),
          considerationActual = BigDecimal("5000.25"),
          isConnectedPartiesTransactions = true,
          companyName = "Company",
          companyRegistrationNumber = Some("12345678"),
          reliefClaimedName = Some("Group relief"),
          reliefPercentage = Some(100)
        ),
        contingentDetails = None,
        mainSellerDetails = Some(SingleTransferSellerDetails(
          sellerName = "Bob Seller",
          addr1 = "1 Seller Street",
          addr2 = Some("Seller District"),
          addr3 = Some("Seller City"),
          addr4 = None,
          postcode = "LS1 1AA",
          country = "United Kingdom"
        )),
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
          taxRate = BuyerTaxRate.HalfPercent,
          isPLC = None
        ),
        otherBuyers = None,
        agentDetails = None
      )
    }

    "throw when seller postcode is missing" in {
      val rowMissingBuyerPostcode = parsedStcRow.copy(sellerPostcode = None)

      an[IllegalArgumentException] shouldBe thrownBy {
        RowTransforms.fromValidatedStcRowToStfRequest(rowMissingBuyerPostcode, individualData,None)
      }
    }

    "throw when seller country is missing" in {
      val rowMissingSellerCountry = parsedStcRow.copy(sellerCountry = None, sellerAddressInUK = Some(false))

      an[IllegalArgumentException] shouldBe thrownBy {
        RowTransforms.fromValidatedStcRowToStfRequest(rowMissingSellerCountry, individualData,None)
      }
    }

    "throw when market value is missing for connected parties transactions" in {
      val rowMissingMarketValue = parsedStcRow.copy(totalMarketValue = None)

      an[IllegalArgumentException] shouldBe thrownBy {
        RowTransforms.fromValidatedStcRowToStfRequest(rowMissingMarketValue, individualData,None)
      }
    }
  }
}
