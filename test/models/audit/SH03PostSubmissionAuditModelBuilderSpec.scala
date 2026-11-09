/*
 * Copyright 2024 HM Revenue & Customs
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
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.requests.{StcAuthorisedRequest, StcDataRequest}
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.requests.{OrganisationUniqueIdentifier, UniqueIdentifier}
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{CredentialId, GroupIdentifier, SubmissionId, SubscriptionId, TransferType, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType.SH03
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.models.audit.SH03PostSubmissionAuditModelBuilder
import uk.gov.hmrc.securitiestransferchargefrontend.models.nrs.IdentityData
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.HowToNotifyAboutShareBuyback
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission._
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.HowToNotifyAboutShareBuybackPage
import uk.gov.hmrc.securitiestransferchargefrontend.services.SubmissionCreateResponseSuccess

import java.time.LocalDate

class SH03PostSubmissionAuditModelBuilderSpec extends SpecBase {

  override val testUserId = UserId("internal123")
  val testInternalId = "internal123"
  val testGroupId = GroupIdentifier("group123")
  val testSubmissionId = SubmissionId("STC-123456789")
  val testSubscriptionId = SubscriptionId("SUB123")
  val testCredentialId = CredentialId("CRED123")
  val testIdentityData = IdentityData(
    internalId = Some("internal123"),
    externalId = Some("ext123"),
    agentCode = None,
    credentials = None,
    confidenceLevel = 200,
    nino = None,
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
    affinityGroup = Some("Organisation"),
    credentialStrength = None,
    loginTimes = None
  )
  val testUniqueIdentifier: UniqueIdentifier = OrganisationUniqueIdentifier(Some("1234567890"))

  val testDeclaration = SingleTransferDeclaration(
    role1 = Some(DeclarationRole.Director),
    role2 = Some("Secretary"),
    name = "Jane Smith",
    addr1 = "10 Main Road",
    addr2 = Some("Area"),
    addr3 = Some("Region"),
    addr4 = Some("London"),
    postcode = "SW1A 1AA",
    country = "GB",
    selfDeclarationAgent = Some(false),
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
    transactionType = TransferType.SH03,
    reasonForPurchase = Some(ReasonForPurchase.PurchasedForCancellation),
    typeOfSecurity = "Shares",
    numberOfShares = 1000,
    nominalValue = None,
    marketValue = Some(BigDecimal(5000)),
    qualifyAsTreasuryShares = Some(true),
    maxPricePaid = Some(BigDecimal(10)),
    minPricePaid = Some(BigDecimal(5)),
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
    isPLC = Some(true)
  )

  val testSH03Request = SingleTransferRequest(
    recordId = 1,
    transactionDetails = testTransactionDetails,
    contingentDetails = None,
    mainSellerDetails = None,
    otherSellers = None,
    mainBuyerDetails = testBuyerDetails,
    otherBuyers = None,
    agentDetails = testAgentDetails
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
    affinityGroup = AffinityGroup.Organisation,
    subscriptionId = testSubscriptionId,
    credentialId = testCredentialId,
    identityData = testIdentityData,
    uniqueIdentifier = testUniqueIdentifier
  )

  val testUserAnswers: UserAnswers = UserAnswers.empty(testUserId)(testGroupId)(testSubmissionId)(SH03)

  val testUserAnswersWithOneAtATime: UserAnswers = testUserAnswers.set(HowToNotifyAboutShareBuybackPage, HowToNotifyAboutShareBuyback.OneAtATime).success.value
  
  val testUserAnswersWithMoreThanOneAtATime: UserAnswers = testUserAnswers.set(HowToNotifyAboutShareBuybackPage, HowToNotifyAboutShareBuyback.MoreThanOneAtATime).success.value

  val testDataRequest = StcDataRequest(
    request = testAuthorisedRequest,
    userAnswers = testUserAnswersWithOneAtATime
  )
  
  val testDataRequestBulk = StcDataRequest(
    request = testAuthorisedRequest,
    userAnswers = testUserAnswersWithMoreThanOneAtATime
  )

  "SH03PostSubmissionAuditModelBuilder" - {

    "must build audit model for single transfer" in {
      val result = SH03PostSubmissionAuditModelBuilder.buildSubmissionAuditModel(
        declaration = testDeclaration,
        agentDetails = testAgentDetails,
        sh03Requests = Seq(testSH03Request),
        etmpResponses = Seq(testEtmpResponse),
        request = testDataRequest,
        totalBatches = 1,
        totalTransfers = 1,
        currentBatch = 1,
        batchStartIndex = 0,
        versionNumber = 1
      )

      result.auditDetail.declarantName mustBe "Jane Smith"
      result.auditDetail.subscriptionId mustBe testSubscriptionId.value
      result.auditDetail.submissionId mustBe testSubmissionId.value
      result.auditDetail.affinityGroup mustBe "Organisation"
      result.auditDetail.transferMethod mustBe "oneAtATime"
      result.auditDetail.totalBatches mustBe 1
      result.auditDetail.totalTransfers mustBe 1
      result.auditDetail.currentBatch mustBe 1
      result.auditDetail.transfers.length mustBe 1
    }

    "must build audit model for multiple transfers" in {
      val result = SH03PostSubmissionAuditModelBuilder.buildSubmissionAuditModel(
        declaration = testDeclaration,
        agentDetails = testAgentDetails,
        sh03Requests = Seq(testSH03Request, testSH03Request.copy(recordId = 2)),
        etmpResponses = Seq(testEtmpResponse, testEtmpResponse.copy(utrn = "UTRN124")),
        request = testDataRequestBulk,
        totalBatches = 1,
        totalTransfers = 2,
        currentBatch = 1,
        batchStartIndex = 0,
        versionNumber = 1
      )

      result.auditDetail.transferMethod mustBe "moreThanOneAtATime"
      result.auditDetail.totalTransfers mustBe 2
      result.auditDetail.transfers.length mustBe 2
      result.auditDetail.transfers.head.transferNumber mustBe 1
      result.auditDetail.transfers(1).transferNumber mustBe 2
    }

    "must build audit model without agent details" in {
      val result = SH03PostSubmissionAuditModelBuilder.buildSubmissionAuditModel(
        declaration = testDeclaration,
        agentDetails = None,
        sh03Requests = Seq(testSH03Request),
        etmpResponses = Seq(testEtmpResponse),
        request = testDataRequest,
        totalBatches = 1,
        totalTransfers = 1,
        currentBatch = 1,
        batchStartIndex = 0,
        versionNumber = 1
      )

      result.auditDetail.reference mustBe ""
    }

    "must build audit model for Individual affinity group" in {
      val individualRequest = testDataRequest.copy(
        request = testAuthorisedRequest.copy(affinityGroup = AffinityGroup.Individual),
        userAnswers = testUserAnswers
      )

      val result = SH03PostSubmissionAuditModelBuilder.buildSubmissionAuditModel(
        declaration = testDeclaration,
        agentDetails = None,
        sh03Requests = Seq(testSH03Request),
        etmpResponses = Seq(testEtmpResponse),
        request = individualRequest,
        totalBatches = 1,
        totalTransfers = 1,
        currentBatch = 1,
        batchStartIndex = 0,
        versionNumber = 1
      )

      result.auditDetail.affinityGroup mustBe "Individual"
    }

    "must build audit model for Agent affinity group" in {
      val agentRequest = testDataRequest.copy(
        request = testAuthorisedRequest.copy(affinityGroup = AffinityGroup.Agent),
        userAnswers = testUserAnswers
      )

      val result = SH03PostSubmissionAuditModelBuilder.buildSubmissionAuditModel(
        declaration = testDeclaration,
        agentDetails = testAgentDetails,
        sh03Requests = Seq(testSH03Request),
        etmpResponses = Seq(testEtmpResponse),
        request = agentRequest,
        totalBatches = 1,
        totalTransfers = 1,
        currentBatch = 1,
        batchStartIndex = 0,
        versionNumber = 1
      )

      result.auditDetail.affinityGroup mustBe "Agent"
      result.auditDetail.reference mustBe "REF123"
    }

    "must correctly map tax rate from HalfPercent" in {
      val result = SH03PostSubmissionAuditModelBuilder.buildSubmissionAuditModel(
        declaration = testDeclaration,
        agentDetails = None,
        sh03Requests = Seq(testSH03Request),
        etmpResponses = Seq(testEtmpResponse),
        request = testDataRequest,
        totalBatches = 1,
        totalTransfers = 1,
        currentBatch = 1,
        batchStartIndex = 0,
        versionNumber = 1
      )

      result.auditDetail.transfers.head.mainBuyerDetails.taxRate mustBe 1
    }

    "must correctly map tax rate from OneAndHalfPercent" in {
      val buyerWithHigherRate = testBuyerDetails.copy(taxRate = BuyerTaxRate.OneAndHalfPercent)
      val requestWithHigherRate = testSH03Request.copy(mainBuyerDetails = buyerWithHigherRate)

      val result = SH03PostSubmissionAuditModelBuilder.buildSubmissionAuditModel(
        declaration = testDeclaration,
        agentDetails = None,
        sh03Requests = Seq(requestWithHigherRate),
        etmpResponses = Seq(testEtmpResponse),
        request = testDataRequest,
        totalBatches = 1,
        totalTransfers = 1,
        currentBatch = 1,
        batchStartIndex = 0,
        versionNumber = 1
      )

      result.auditDetail.transfers.head.mainBuyerDetails.taxRate mustBe 2
    }

    "must handle batch processing with correct transfer numbers" in {
      val result = SH03PostSubmissionAuditModelBuilder.buildSubmissionAuditModel(
        declaration = testDeclaration,
        agentDetails = None,
        sh03Requests = Seq(testSH03Request, testSH03Request.copy(recordId = 2)),
        etmpResponses = Seq(testEtmpResponse, testEtmpResponse.copy(utrn = "UTRN124")),
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
      val result = SH03PostSubmissionAuditModelBuilder.buildSubmissionAuditModel(
        declaration = testDeclaration,
        agentDetails = None,
        sh03Requests = Seq(testSH03Request),
        etmpResponses = Seq(testEtmpResponse),
        request = testDataRequest,
        totalBatches = 1,
        totalTransfers = 1,
        currentBatch = 1,
        batchStartIndex = 0,
        versionNumber = 1
      )

      result.auditType mustBe "PurchaseOfOwnSharesSubmission"
    }
  }
}
