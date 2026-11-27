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

package services

import base.stubs.StubReliefsService
import base.{Fixtures, SpecBase}
import clients.FakeEtmpSubmissionClient
import org.mockito.Mockito.*
import org.scalacheck.Gen
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Relief, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.{ReasonForPurchase, RoleAtPurchasingCompany}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.{EtmpSubmissionClient, ReliefsUnavailableException}
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{SubmissionId, SubscriptionId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.bulk.CompanyDetails as BulkCompanyDetails
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.RoleAtPurchasingCompanyPage
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.bulk.{BulkCompanyDetailsPage, BulkRoleAtPurchasingCompanyPage}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.AgentReferencePage
import uk.gov.hmrc.securitiestransferchargefrontend.services.*

import java.time.LocalDate
import scala.concurrent.Future

class EtmpSubmissionServiceSpec extends AnyFreeSpec with Matchers with SpecBase with ScalaFutures with MockitoSugar {

  private val mockUserAnswers = mock[UserAnswers]

  private val stfTransaction = StfTransaction(
    agentReference = None,
    confirmedAddress = Some(ConfirmableAddress(List("123 Main Street", "London"), "SW1A 1AA", Some(Country("United Kingdom", "GB")))),
    nameOfBuyer = Some("John Smith"),
    buyerAddress = Some(AlfConfirmedAddress("audit-ref-123", Some("id-123"), AlfAddress(List("123 Main Street", "London"), "SW1A 1AA", Country("United Kingdom", "GB")))),
    nameOfSeller = "Helen Jones",
    sellerAddress = AlfConfirmedAddress("audit-ref-456", Some("id-456"), AlfAddress(List("456 High Street", "Manchester"), "M1 1AA", Country("United Kingdom", "GB"))),
    connectedPersons = true,
    applyingForRelief = false,
    whatReliefAreYouApplyingFor = None,
    securitiesTarget = SecuritiesTarget("Company ABC Ltd", Some("12345678")),
    chargingPoint = LocalDate.of(2026, 6, 15),
    taxRate = TaxRate.HalfPercent,
    purchasingShares = true,
    detailsOfThisTransfer = Some(DetailsOfThisTransfer(
      numberOfShares = 100,
      typeOfShares = "ordinary shares",
      amountPaid = BigDecimal("8000.00"),
      marketValue = Some(BigDecimal("10000.50"))
    )),
    otherSecuritiesType = None,
    amountPaidForSecurities = None,
    totalMarketValue = None
  )

  private val sh03Transaction  = Sh03Transaction(
    agentReference = None,
    companyDetails = Fixtures.sh03CompanyDetails,
    reasonForPurchase = ReasonForPurchase.ForCancellation,
    treasuryShares = None,
    connectedPersons = false,
    applyingForRelief = false,
    whatReliefAreYouApplyingFor = None,
    detailsOfThisSharePurchase = Fixtures.sh03DetailsOfThisSharePurchase,
    maximumAmountPaid = Some(BigDecimal(1500)),
    minimumAmountPaid = Some(BigDecimal(1000)),
    chargingPoint = LocalDate.now(),
    roleAtPurchasingCompany = RoleAtPurchasingCompany("Administrator",None)
  )

  when(mockUserAnswers.get(StfTransaction)).thenReturn(Some(stfTransaction))
  when(mockUserAnswers.get(Sh03Transaction)).thenReturn(Some(sh03Transaction))
  when(mockUserAnswers.get(RoleAtPurchasingCompanyPage)).thenReturn(None)
  when(mockUserAnswers.get(AgentReferencePage)).thenReturn(None)
  when(mockUserAnswers.submissionId).thenReturn(submissionId)

  val affinityData: AffinityData = Individual(
    name = "John Smith",
    address = Address(
      addressLine1 = "10 High Street",
      addressLine2 = Some("Bolton"),
      addressLine3 = None,
      postcode = "BL1 2TG",
      countryCode = "UK"
    ),
    email = "jsmith@foo.com",
    nino = "NX787356B" // Can come from GG
  )

  val successfulChargeGen: Gen[StcCharge] = for {
    recordId              <- Gen.chooseNum(1, 1000)
    utrn                  <- Gen.chooseNum(10000000, 99999999).map(_.toString)
    (chargeTypeDescription, chargeType) <- Gen.oneOf(
      ("Securities transfer tax", "STT"),
      ("Late submission", "LSP"),
      ("Late Payment", "LPP"),
      ("Late Payment Interest", "LPI")
    )
    chargeReference       <- Gen.chooseNum(1, 1000).map(n => s"STT$n")
    chargeAmount          <- Gen.chooseNum(100d, 1000d)
    chargeDueDate         = "2026-09-09"
  } yield StcChargeSuccess(recordId, utrn, chargeTypeDescription, chargeReference, chargeType, chargeAmount, chargeDueDate)

  val failedChargeGen: Gen[StcCharge] = for {
    recordId  <- Gen.chooseNum(1, 1000)
    errorCode <- Gen.oneOf("404", "401", "400", "500")
    errorText = "Big error"
  } yield StcChargeFailure(recordId, errorCode, errorText)

  def successfulChargesGen: Gen[List[StcCharge]] = Gen.nonEmptyListOf(successfulChargeGen)
  def failureChargesGen: Gen[List[StcCharge]] = Gen.nonEmptyListOf(failedChargeGen)
  def mixedChargesGen: Gen[List[StcCharge]] = for {
    succ   <- successfulChargeGen
    fail   <- failedChargeGen
    count  <- Gen.chooseNum(0, 10)
    others <- Gen.listOfN(count, Gen.oneOf(successfulChargeGen, failedChargeGen))
  } yield succ :: fail :: others

  def successfulChargesResponseGen: Gen[StcTransactionCreateResponse] = for {
    charges <- successfulChargesGen
  } yield StcTransactionCreateProcessed(
    success = StcTransactionCreateProcessedBody(
      processingDate = "2026-09-01",
      charges = charges
    ))

  def mixedChargesResponseGen: Gen[StcTransactionCreateResponse] = for {
    charges <- mixedChargesGen
  } yield StcTransactionCreateProcessed(
    success = StcTransactionCreateProcessedBody(
      processingDate = "2026-09-01",
      charges = charges
    ))

  def failedChargesResponseGen: Gen[StcTransactionCreateResponse] = for {
    charges <- failureChargesGen
  } yield StcTransactionCreateProcessed(
    success = StcTransactionCreateProcessedBody(
      processingDate = "2026-09-01",
      charges = charges
    ))

  "The service" - {
    "return success if ETMP returns only successful responses" in {
      val resp = successfulChargesResponseGen.sample.get
      val fakeClient = FakeEtmpSubmissionClient(resp)
      val service = new EtmpSubmissionServiceImpl(fakeClient, new StubReliefsService())
      val result = service.submitSingleStf(subscriptionId, mockUserAnswers, affinityData)
        whenReady(result) { r =>
          r mustBe a[SubmissionCreateResponseSuccess]
        }
      }

    "return partial success if ETMP returns a mix of successful and unsuccessful responses" in {
      val resp = mixedChargesResponseGen.sample.get
      val fakeClient = FakeEtmpSubmissionClient(resp)
      val service = new EtmpSubmissionServiceImpl(fakeClient, new StubReliefsService())
      val result = service.submitSingleStf(subscriptionId, mockUserAnswers, affinityData)
      whenReady(result) { r =>
        r mustBe a[SubmissionCreateResponsePartialFailure]
      }
    }
    "return failure if ETMP returns only unsuccessful responses" in {
      val resp = failedChargesResponseGen.sample.get
      val fakeClient = FakeEtmpSubmissionClient(resp)
      val service = new EtmpSubmissionServiceImpl(fakeClient, new StubReliefsService())
      val result = service.submitSingleStf(subscriptionId, mockUserAnswers, affinityData)
      whenReady(result) { r =>
        r mustBe SubmissionCreateResponseFailure
      }
    }

    "return success if ETMP returns only successful responses (SH03)" in {
      val resp = successfulChargesResponseGen.sample.get
      val fakeClient = FakeEtmpSubmissionClient(resp)
      val service = new EtmpSubmissionServiceImpl(fakeClient, new StubReliefsService())
      val result = service.submitSingleSh03(subscriptionId, mockUserAnswers, affinityData)
      whenReady(result) { r =>
        r mustBe a[SubmissionCreateResponseSuccess]
      }
    }

    "return partial success if ETMP returns a mix of successful and unsuccessful responses (SH03)" in {
      val resp = mixedChargesResponseGen.sample.get
      val fakeClient = FakeEtmpSubmissionClient(resp)
      val service = new EtmpSubmissionServiceImpl(fakeClient, new StubReliefsService())
      val result = service.submitSingleSh03(subscriptionId, mockUserAnswers, affinityData)
      whenReady(result) { r =>
        r mustBe a[SubmissionCreateResponsePartialFailure]
      }
    }

    "return failure if ETMP returns only unsuccessful responses (SH03)" in {
      val resp = failedChargesResponseGen.sample.get
      val fakeClient = FakeEtmpSubmissionClient(resp)
      val service = new EtmpSubmissionServiceImpl(fakeClient, new StubReliefsService())
      val result = service.submitSingleSh03(subscriptionId, mockUserAnswers, affinityData)
      whenReady(result) { r =>
        r mustBe SubmissionCreateResponseFailure
      }
    }

    "getSubmissionDataStf should return Some with correct data when StfTransaction exists" in {
      val fakeClient = FakeEtmpSubmissionClient(successfulChargesResponseGen.sample.get)
      val service = new EtmpSubmissionServiceImpl(fakeClient, new StubReliefsService())
      val result = service.getSubmissionDataStf(mockUserAnswers, affinityData, StubReliefsService.reliefs)
      
      result mustBe defined
      result.foreach { case (request, transaction) =>
        request mustBe a[SingleTransferRequest]
        transaction mustBe stfTransaction
      }
    }

    "getSubmissionDataStf should return None when StfTransaction does not exist" in {
      val mockEmptyUserAnswers = mock[UserAnswers]
      when(mockEmptyUserAnswers.get(StfTransaction)).thenReturn(None)
      when(mockEmptyUserAnswers.get(RoleAtPurchasingCompanyPage)).thenReturn(None)
      when(mockEmptyUserAnswers.get(AgentReferencePage)).thenReturn(None)
      when(mockEmptyUserAnswers.submissionId).thenReturn(submissionId)
      
      val fakeClient = FakeEtmpSubmissionClient(successfulChargesResponseGen.sample.get)
      val service = new EtmpSubmissionServiceImpl(fakeClient, new StubReliefsService())
      val result = service.getSubmissionDataStf(mockEmptyUserAnswers, affinityData, StubReliefsService.reliefs)
      
      result mustBe None
    }

    "getSubmissionDataSh03 should return Some with correct data when Sh03Transaction exists" in {
      val fakeClient = FakeEtmpSubmissionClient(successfulChargesResponseGen.sample.get)
      val service = new EtmpSubmissionServiceImpl(fakeClient, new StubReliefsService())
      val result = service.getSubmissionDataSh03(mockUserAnswers, affinityData, StubReliefsService.reliefs)
      
      result mustBe defined
      result.foreach { case (declaration, request) =>
        declaration mustBe a[SingleTransferDeclaration]
        request mustBe a[SingleTransferRequest]
      }
    }

    "getSubmissionDataSh03 should return None when Sh03Transaction does not exist" in {
      val mockEmptyUserAnswers = mock[UserAnswers]
      when(mockEmptyUserAnswers.get(StfTransaction)).thenReturn(None)
      when(mockEmptyUserAnswers.get(Sh03Transaction)).thenReturn(None)
      when(mockEmptyUserAnswers.get(RoleAtPurchasingCompanyPage)).thenReturn(None)
      when(mockEmptyUserAnswers.get(AgentReferencePage)).thenReturn(None)
      when(mockEmptyUserAnswers.submissionId).thenReturn(submissionId)
      
      val fakeClient = FakeEtmpSubmissionClient(successfulChargesResponseGen.sample.get)
      val service = new EtmpSubmissionServiceImpl(fakeClient, new StubReliefsService())
      val result = service.getSubmissionDataSh03(mockEmptyUserAnswers, affinityData, StubReliefsService.reliefs)
      
      result mustBe None
    }
  }

  "submitBulkSH03" - {

    class CapturingEtmpSubmissionClient(toReturn: StcTransactionCreateResponse) extends EtmpSubmissionClient {
      var payload: Option[SubmissionBatchPayload] = None

      def submitBatchTransfer(subscriptionId: SubscriptionId, submissionId: SubmissionId, payload: SubmissionBatchPayload)
                             (implicit hc: HeaderCarrier): Future[StcTransactionCreateResponse] = {
        this.payload = Some(payload)
        Future.successful(toReturn)
      }

      def getTransaction(subscriptionId: SubscriptionId, utrn: String)(implicit hc: HeaderCarrier): Future[GetTransactionResult] =
        Future.failed(new UnsupportedOperationException("getTransaction not stubbed"))
    }

    def declarationSentFor(userAnswers: UserAnswers): SingleTransferDeclaration = {
      val client = new CapturingEtmpSubmissionClient(successfulChargesResponseGen.sample.get)
      // The fixture row claims "Group relief", so the list needs a relief with exactly that name.
      val service = new EtmpSubmissionServiceImpl(client, new StubReliefsService(Seq(Relief("Group relief", 100))))
      service.submitBulkSH03(subscriptionId, Seq(Fixtures.testValidatedStcRow), userAnswers, affinityData).futureValue
      client.payload.value.declaration
    }

    val bulkAnswers =
      emptyUserAnswers
        .set(BulkCompanyDetailsPage, BulkCompanyDetails("Bulk Co Ltd", "87654321")).success.value
        .set(BulkRoleAtPurchasingCompanyPage, RoleAtPurchasingCompany("ukSocietas", Some("Board"))).success.value

    "send the role and UKS organ from the bulk role page in the declaration" in {
      val declaration = declarationSentFor(bulkAnswers)
      declaration.role1 mustBe Some(DeclarationRole.UkSocietas)
      declaration.role2 mustBe Some("Board")
    }

    "not use a role saved on the single SH03 role page" in {
      val answers = bulkAnswers
        .remove(BulkRoleAtPurchasingCompanyPage).success.value
        .set(RoleAtPurchasingCompanyPage, RoleAtPurchasingCompany("director", None)).success.value

      declarationSentFor(answers).role1 mustBe None
    }
  }

  "getTransaction" - {

    val subscriptionId = uk.gov.hmrc.securitiestransferchargefrontend.domain.SubscriptionId("XASTC0000123456")

    "return what the client returns" in {
      val fakeClient = new FakeEtmpSubmissionClient(
        StcTransactionCreateProcessed(StcTransactionCreateProcessedBody("2026-03-31", Nil)),
        scala.concurrent.Future.successful(GetTransactionResult.StcIdNotFound)
      )
      val service = new EtmpSubmissionServiceImpl(fakeClient, new StubReliefsService())

      service.getTransaction(subscriptionId, "900459020010").futureValue mustBe GetTransactionResult.StcIdNotFound
    }

    "propagate client failures" in {
      val fakeClient = new FakeEtmpSubmissionClient(
        StcTransactionCreateProcessed(StcTransactionCreateProcessedBody("2026-03-31", Nil)),
        scala.concurrent.Future.failed(new RuntimeException("boom"))
      )
      val service = new EtmpSubmissionServiceImpl(fakeClient, new StubReliefsService())

      service.getTransaction(subscriptionId, "900459020010").failed.futureValue.getMessage mustBe "boom"
    }
  }

  "relief percentage on submission" - {

    val subscriptionId = SubscriptionId("XASTC0000123456")

    class CapturingClient extends EtmpSubmissionClient {
      var payload: Option[SubmissionBatchPayload] = None
      def submitBatchTransfer(subscriptionId: SubscriptionId, submissionId: SubmissionId, payload: SubmissionBatchPayload)
                             (implicit hc: HeaderCarrier): Future[StcTransactionCreateResponse] = {
        this.payload = Some(payload)
        Future.successful(successfulChargesResponseGen.sample.get)
      }
      def getTransaction(subscriptionId: SubscriptionId, utrn: String)(implicit hc: HeaderCarrier): Future[GetTransactionResult] =
        Future.failed(new UnsupportedOperationException("getTransaction not stubbed"))
    }

    val claimingStf = stfTransaction.copy(applyingForRelief = true, whatReliefAreYouApplyingFor = Some("Charities Relief"))

    def answersWith(transaction: StfTransaction): UserAnswers = {
      val answers = mock[UserAnswers]
      when(answers.get(StfTransaction)).thenReturn(Some(transaction))
      when(answers.get(AgentReferencePage)).thenReturn(None)
      when(answers.submissionId).thenReturn(submissionId)
      answers
    }

    "submit the percentage from the list of reliefs" in {
      val client  = new CapturingClient
      val service = new EtmpSubmissionServiceImpl(client, new StubReliefsService(Seq(Relief("Charities Relief", 50))))

      service.submitSingleStf(subscriptionId, answersWith(claimingStf), affinityData).futureValue

      client.payload.value.transfers.head.transactionDetails.reliefPercentage mustBe Some(50)
    }

    "not submit, and fail, when the claimed relief is not in the list of reliefs" in {
      val client  = new CapturingClient
      val service = new EtmpSubmissionServiceImpl(client, new StubReliefsService(Seq(Relief("Another Relief", 50))))

      service.submitSingleStf(subscriptionId, answersWith(claimingStf), affinityData).futureValue mustBe SubmissionCreateResponseFailure

      client.payload mustBe None
    }

    "not submit, and fail, when the list of reliefs is unavailable" in {
      val client = new CapturingClient
      val unavailable = new ReliefsService {
        override def getReliefs()(implicit hc: HeaderCarrier): Future[Seq[Relief]] =
          Future.failed(new ReliefsUnavailableException("down"))
      }
      val service = new EtmpSubmissionServiceImpl(client, unavailable)

      service.submitSingleStf(subscriptionId, answersWith(claimingStf), affinityData).futureValue mustBe SubmissionCreateResponseFailure

      client.payload mustBe None
    }

    "submit a claimed relief's percentage for a bulk STF submission" in {
      val client  = new CapturingClient
      val service = new EtmpSubmissionServiceImpl(client, new StubReliefsService(Seq(Relief("Charities Relief", 50))))
      val parsed  = Fixtures.testValidatedStcRow.parsedRow
      val row     = Fixtures.testValidatedStcRow.copy(parsedRow = parsed.copy(
        applyingForRelief = Some(true), whatReliefAreYouApplyingFor = Some("Charities Relief")))

      service.submitBulkStf(subscriptionId, Seq(row), emptyUserAnswers, affinityData).futureValue

      client.payload.value.transfers.head.transactionDetails.reliefPercentage mustBe Some(50)
    }
  }

}
