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

package models.audit

import base.SpecBase
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.requests.{IndividualUniqueIdentifier, StcAuthorisedRequest, StcDataRequest}
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{CredentialId, GroupIdentifier, SubmissionId, SubscriptionId, TransferType, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType.STF
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.models.audit.STFPostSubmissionAuditModelBuilder
import uk.gov.hmrc.securitiestransferchargefrontend.models.nrs.IdentityData
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission._
import uk.gov.hmrc.securitiestransferchargefrontend.services.SubmissionCreateResponseSuccess

import java.time.LocalDate

class STFPostSubmissionAuditModelBuilderSpec extends SpecBase {

  override val testUserId = UserId("user123")
  val testInternalId = testUserId.value
  val testGroupId = GroupIdentifier("group123")
  val testSubmissionId = SubmissionId("STC-123456789")
  val testSubscriptionId = SubscriptionId("SUB123")
  val testCredentialId = CredentialId("CRED123")

  val testIdentityData = IdentityData(
    internalId = Some("user123"),
    externalId = Some("ext123"),
    agentCode = None,
    credentials = None,
    confidenceLevel = 200,
    nino = Some("AB123456C"),
    saUtr = None,
    name = None,
    dateOfBirth = None,
    email = None,
    agentInformation = None,
    groupIdentifier = Some("group123"),
    credentialRole = None,
    mdtpInformation = None,
    itmpName = None,
    itmpDateOfBirth = None,
    itmpAddress = None,
    affinityGroup = Some("Individual"),
    credentialStrength = None,
    loginTimes = None
  )

  val testDeclaration = SingleTransferDeclaration(
    role1 = None,
    role2 = None,
    name = "Jane Smith",
    addr1 = "10 Main Road",
    addr2 = Some("Area"),
    addr3 = Some("Region"),
    addr4 = Some("London"),
    postcode = "SW1A 1AA",
    country = "GB",
    selfDeclarationAgent = None,
    isCorrectInfo = true
  )

  val testAgentDetails = Some(SingleTransferAgentDetails(
    name = "Agent Name",
    addr1 = Some("1 Agent Street"),
    addr2 = None,
    addr3 = None,
    addr4 = None,
    postcode = "AB1 2CD",
    country = "GB",
    phone = "01234567890",
    email = "agent@example.com",
    clientReference = Some("REF123")
  ))

  val testTransactionDetails = SingleTransferTransactionDetails(
    transactionType = TransferType.STF,
    reasonForPurchase = None,
    typeOfSecurity = "Ordinary",
    numberOfShares = 1000,
    nominalValue = None,
    marketValue = Some(BigDecimal(5000)),
    qualifyAsTreasuryShares = None,
    maxPricePaid = None,
    minPricePaid = None,
    originalChargingPoint = LocalDate.of(2024, 1, 1),
    considerationActual = BigDecimal(7500),
    isConnectedPartiesTransactions = false,
    companyName = "Test Company Ltd",
    companyRegistrationNumber = Some("12345678"),
    reliefClaimedName = Some("Group relief"),
    reliefPercentage = Some(50)
  )

  val testBuyerDetails = SingleTransferBuyerDetails(
    buyerName = "John Doe",
    addr1 = "1 High Street",
    addr2 = Some("District"),
    addr3 = Some("Town"),
    addr4 = Some("City"),
    postcode = "AB1 2CD",
    country = "GB",
    email = "john@example.com",
    uniqueId = Some("AB123456C"),
    taxRate = BuyerTaxRate.HalfPercent,
    isPLC = Some(false)
  )

  val testSellerDetails = Some(SingleTransferSellerDetails(
    sellerName = "Seller Co",
    addr1 = "5 Seller Street",
    addr2 = Some("District"),
    addr3 = Some("Town"),
    addr4 = Some("City"),
    postcode = "SE1 1SE",
    country = "GB"
  ))

  val testSTFRequest = SingleTransferRequest(
    recordId = 1,
    transactionDetails = testTransactionDetails,
    contingentDetails = None,
    mainSellerDetails = testSellerDetails,
    otherSellers = None,
    mainBuyerDetails = testBuyerDetails,
    otherBuyers = None,
    agentDetails = testAgentDetails
  )

  val testStfTransaction = StfTransaction(
    agentReference = None,
    confirmedAddress = None,
    nameOfBuyer = Some("John Doe"),
    buyerAddress = None,
    nameOfSeller = "Seller Co",
    sellerAddress = null,
    connectedPersons = false,
    applyingForRelief = true,
    whatReliefAreYouApplyingFor = Some("Group relief"),
    securitiesTarget = null,
    chargingPoint = LocalDate.of(2024, 1, 1),
    taxRate = null,
    purchasingShares = true,
    detailsOfThisTransfer = None,
    otherSecuritiesType = None,
    amountPaidForSecurities = Some(BigDecimal(7500)),
    totalMarketValue = Some(BigDecimal(5000))
  )

  val testEtmpResponse = SubmissionCreateResponseSuccess(
    submissionId = testSubmissionId,
    utrn = "UTRN123",
    chargeReferences = Seq("CHG123"),
    taxDue = BigDecimal(1000.50),
    paymentDueBy = LocalDate.of(2024, 12, 31),
    agentReference = None
  )

  val testAuthorisedRequest = StcAuthorisedRequest(
    request = FakeRequest(),
    internalId = testInternalId,
    groupIdentifier = testGroupId.value,
    affinityGroup = AffinityGroup.Individual,
    subscriptionId = testSubscriptionId,
    credentialId = testCredentialId,
    identityData = testIdentityData,
    uniqueIdentifier = IndividualUniqueIdentifier("AB123456C")
  )

  val testUserAnswers: UserAnswers = UserAnswers.empty(testUserId)(testGroupId)(testSubmissionId)(STF)

  val testDataRequest = StcDataRequest(
    request = testAuthorisedRequest,
    userAnswers = testUserAnswers
  )

  "STFPostSubmissionAuditModelBuilder" - {

    "must build audit model for single transfer" in {
      val result = STFPostSubmissionAuditModelBuilder.buildSubmissionAuditModel(
        declaration = testDeclaration,
        agentDetails = testAgentDetails,
        stfRequests = Seq(testSTFRequest),
        etmpResponses = Seq(testEtmpResponse),
        stfTransaction = testStfTransaction,
        request = testDataRequest,
        totalBatches = 1,
        totalTransfers = 1,
        currentBatch = 1,
        batchStartIndex = 0,
        versionNumber = 1,
        chargeType = "STC",
        chargeTypeDescription = "tax"
      )

      result.auditDetail.declarantName mustBe "Jane Smith"
      result.auditDetail.subscriptionId mustBe testSubscriptionId.value
      result.auditDetail.submissionId mustBe testSubmissionId.value
      result.auditDetail.affinityGroup mustBe "Individual"
      result.auditDetail.totalBatches mustBe 1
      result.auditDetail.totalTransfers mustBe 1
      result.auditDetail.currentBatch mustBe 1
      result.auditDetail.transfers.length mustBe 1
    }

    "must build audit model for multiple transfers" in {
      val result = STFPostSubmissionAuditModelBuilder.buildSubmissionAuditModel(
        declaration = testDeclaration,
        agentDetails = testAgentDetails,
        stfRequests = Seq(testSTFRequest, testSTFRequest.copy(recordId = 2)),
        etmpResponses = Seq(testEtmpResponse, testEtmpResponse.copy(utrn = "UTRN124")),
        stfTransaction = testStfTransaction,
        request = testDataRequest,
        totalBatches = 1,
        totalTransfers = 2,
        currentBatch = 1,
        batchStartIndex = 0,
        versionNumber = 1
      )

      result.auditDetail.totalTransfers mustBe 2
      result.auditDetail.transfers.length mustBe 2
      result.auditDetail.transfers.head.transferNumber mustBe 1
      result.auditDetail.transfers(1).transferNumber mustBe 2
    }

    "must build audit model without agent details" in {
      val result = STFPostSubmissionAuditModelBuilder.buildSubmissionAuditModel(
        declaration = testDeclaration,
        agentDetails = None,
        stfRequests = Seq(testSTFRequest),
        etmpResponses = Seq(testEtmpResponse),
        stfTransaction = testStfTransaction,
        request = testDataRequest,
        totalBatches = 1,
        totalTransfers = 1,
        currentBatch = 1,
        batchStartIndex = 0,
        versionNumber = 1
      )

      result.auditDetail.reference mustBe ""
    }

    "must correctly map tax rate from HalfPercent to BigDecimal" in {
      val result = STFPostSubmissionAuditModelBuilder.buildSubmissionAuditModel(
        declaration = testDeclaration,
        agentDetails = None,
        stfRequests = Seq(testSTFRequest),
        etmpResponses = Seq(testEtmpResponse),
        stfTransaction = testStfTransaction,
        request = testDataRequest,
        totalBatches = 1,
        totalTransfers = 1,
        currentBatch = 1,
        batchStartIndex = 0,
        versionNumber = 1
      )

      result.auditDetail.transfers.head.transferDetails.taxRate mustBe BigDecimal(0.5)
    }

    "must correctly map tax rate from OneAndHalfPercent to BigDecimal" in {
      val buyerWithHigherRate = testBuyerDetails.copy(taxRate = BuyerTaxRate.OneAndHalfPercent)
      val requestWithHigherRate = testSTFRequest.copy(mainBuyerDetails = buyerWithHigherRate)

      val result = STFPostSubmissionAuditModelBuilder.buildSubmissionAuditModel(
        declaration = testDeclaration,
        agentDetails = None,
        stfRequests = Seq(requestWithHigherRate),
        etmpResponses = Seq(testEtmpResponse),
        stfTransaction = testStfTransaction,
        request = testDataRequest,
        totalBatches = 1,
        totalTransfers = 1,
        currentBatch = 1,
        batchStartIndex = 0,
        versionNumber = 1
      )

      result.auditDetail.transfers.head.transferDetails.taxRate mustBe BigDecimal(1.5)
    }

    "must handle seller in UK correctly" in {
      val result = STFPostSubmissionAuditModelBuilder.buildSubmissionAuditModel(
        declaration = testDeclaration,
        agentDetails = None,
        stfRequests = Seq(testSTFRequest),
        etmpResponses = Seq(testEtmpResponse),
        stfTransaction = testStfTransaction,
        request = testDataRequest,
        totalBatches = 1,
        totalTransfers = 1,
        currentBatch = 1,
        batchStartIndex = 0,
        versionNumber = 1
      )

      result.auditDetail.transfers.head.mainSellerDetails.sellerInUk mustBe true
      result.auditDetail.transfers.head.mainSellerDetails.sellerCountry mustBe "GB"
    }

    "must handle seller not in UK correctly" in {
      val nonUkSeller = testSellerDetails.map(_.copy(country = "FR"))
      val requestWithNonUkSeller = testSTFRequest.copy(mainSellerDetails = nonUkSeller)

      val result = STFPostSubmissionAuditModelBuilder.buildSubmissionAuditModel(
        declaration = testDeclaration,
        agentDetails = None,
        stfRequests = Seq(requestWithNonUkSeller),
        etmpResponses = Seq(testEtmpResponse),
        stfTransaction = testStfTransaction,
        request = testDataRequest,
        totalBatches = 1,
        totalTransfers = 1,
        currentBatch = 1,
        batchStartIndex = 0,
        versionNumber = 1
      )

      result.auditDetail.transfers.head.mainSellerDetails.sellerInUk mustBe false
      result.auditDetail.transfers.head.mainSellerDetails.sellerCountry mustBe "FR"
    }

    "must handle batch processing with correct transfer numbers" in {
      val result = STFPostSubmissionAuditModelBuilder.buildSubmissionAuditModel(
        declaration = testDeclaration,
        agentDetails = None,
        stfRequests = Seq(testSTFRequest, testSTFRequest.copy(recordId = 2)),
        etmpResponses = Seq(testEtmpResponse, testEtmpResponse.copy(utrn = "UTRN124")),
        stfTransaction = testStfTransaction,
        request = testDataRequest,
        totalBatches = 3,
        totalTransfers = 250,
        currentBatch = 2,
        batchStartIndex = 100,
        versionNumber = 1
      )

      result.auditDetail.totalBatches mustBe 3
      result.auditDetail.totalTransfers mustBe 250
      result.auditDetail.currentBatch mustBe 2
      result.auditDetail.transfers.head.transferNumber mustBe 101
      result.auditDetail.transfers(1).transferNumber mustBe 102
    }

    "must have correct auditType" in {
      val result = STFPostSubmissionAuditModelBuilder.buildSubmissionAuditModel(
        declaration = testDeclaration,
        agentDetails = None,
        stfRequests = Seq(testSTFRequest),
        etmpResponses = Seq(testEtmpResponse),
        stfTransaction = testStfTransaction,
        request = testDataRequest,
        totalBatches = 1,
        totalTransfers = 1,
        currentBatch = 1,
        batchStartIndex = 0,
        versionNumber = 1
      )

      result.auditType mustBe "StockTransferFormSubmission"
    }
  }
}
