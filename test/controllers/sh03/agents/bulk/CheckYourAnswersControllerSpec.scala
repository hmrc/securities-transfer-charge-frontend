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

package controllers.sh03.agents.bulk

import base.{FileUploadFixtures, SpecBase}
import base.stubs.StubPersistentNavigator
import com.google.inject.name.Names
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, anyBoolean, anyString, eq as eqTo}
import org.mockito.Mockito.{never, reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.requests.StcDataRequest
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.bulk.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType.SH03
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.models.shared.AgentReference
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.HowToNotifyAboutSecuritiesTransfer
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{ParsedStcRow, ParsedStcRowsDocument, ParsedValue, ValidatedStcRow}
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.BulkConfirmationDetails
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.{Navigator, PersistentNavigator}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.BulkConfirmationDetailsPage
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.bulk.{BulkAgentReferencePage, BulkCheckYourAnswersPage}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.HowToNotifyAboutSecuritiesTransferPage
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.{ParsedStcRowsRepository, SessionRepository}
import uk.gov.hmrc.securitiestransferchargefrontend.services.TransactionSubmissionService
import uk.gov.hmrc.securitiestransferchargefrontend.services.sh03.agents.bulk.CheckYourAnswersService

import java.time.{Instant, LocalDate}
import scala.concurrent.Future


class CheckYourAnswersControllerSpec extends SpecBase with FileUploadFixtures with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRepository, mockCyaService, mockSessionRepository, mockTransactionSubmissionService, mockNavigator)
  }

  private def answersWithoutAgentRef: UserAnswers =
    UserAnswers.empty(testUserId)(testGroupIdentifier)(submissionId)(SH03)
      .set(HowToNotifyAboutSecuritiesTransferPage, HowToNotifyAboutSecuritiesTransfer.MoreThanOneAtATime).success.value
      .setFileUploadReference(reference)

  private def completeUserAnswers: UserAnswers =
    answersWithoutAgentRef
      .set(BulkAgentReferencePage, AgentReference(Some("Reference1"))).success.value


  private val validSh03Row: ParsedStcRow =
    ParsedStcRow(
      rowNumber = 3,
      buyerName = None,
      buyerAddressInUK = None,
      buyerAddressLine1 = None,
      buyerAddressLine2 = None,
      buyerAddressLine3 = None,
      buyerAddressLine4 = None,
      buyerPostcode = None,
      buyerCountry = None,
      sellerName = None,
      sellerAddressInUK = None,
      sellerAddressLine1 = None,
      sellerAddressLine2 = None,
      sellerAddressLine3 = None,
      sellerAddressLine4 = None,
      sellerPostcode = None,
      sellerCountry = None,
      connectedPersons = Some(true),
      applyingForRelief = Some(false),
      whatReliefAreYouApplyingFor = None,
      securitiesTarget = None,
      companyRegistrationNumber = None,
      chargingPoint = ParsedValue.Valid(LocalDate.of(2026, 2, 20)),
      taxRate = None,
      whatTypeOfSecurities = Some("Shares"),
      typeOfShares = None,
      securitiesQuantity = Some("10000"),
      amountPaidForSecurities = Some("15000"),
      totalMarketValue = Some("20000"),
      minSharePrice = None,
      maxSharePrice = None,
      sharePurchaseReason = Some("treasury"),
      purchaseForCancellation = Some(false),
      purchasingShares = None
    )

  private val mockRepository = mock[ParsedStcRowsRepository]
  private val mockCyaService = mock[CheckYourAnswersService]
  private val mockSessionRepository = mock[SessionRepository]
  private val mockTransactionSubmissionService = mock[TransactionSubmissionService]
  private val mockNavigator = mock[Navigator]


  private val withRefStubNavigator = new StubPersistentNavigator(testNextPage, completeUserAnswers, "sh03", "")
  private val withoutRefStubNavigator = new StubPersistentNavigator(testNextPage, answersWithoutAgentRef, "sh03", "")

  private val rows: Seq[ParsedStcRow] = Seq(validSh03Row)

  private val someTestDoc: ParsedStcRowsDocument = ParsedStcRowsDocument(_id = "id", rows = rows, createdAt = Instant.now, fileName = "testFile")
  private val expectedRows: Seq[ValidatedStcRow] = someTestDoc.rows.map(ValidatedStcRow.withNoErrors)
  private val testPaymentDueBy: LocalDate = LocalDate.of(2026, 3, 22)
  private val testTaxDue: BigDecimal = BigDecimal(75.00)

  private def capturedNavigatorAnswers(): UserAnswers = {
    val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
    verify(mockNavigator).nextPage(eqTo(BulkCheckYourAnswersPage), eqTo(NormalMode), captor.capture(), anyBoolean())(any())
    captor.getValue
  }

  "SH03 Agent Bulk CheckYourAnswersController" - {
    
    "onPageLoad" - {
      "must return OK and the correct view for a GET" in {
        when(mockRepository.findDocumentByReference(reference)).thenReturn(Future.successful(Some(someTestDoc)))

        val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
          .overrides(
            bind[Navigator].qualifiedWith(Names.named("agentsSh03")).toInstance(withRefStubNavigator),
            bind[PersistentNavigator].qualifiedWith(Names.named("agentsSh03")).toInstance(withRefStubNavigator),
            bind[ParsedStcRowsRepository].toInstance(mockRepository)
          ).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("Check your answers")
          contentAsString(result) must include("File details")
        }
      }

      "must redirect to Journey Recovery for a GET if findDocumentByReference does not find a document" in {
        when(mockRepository.findDocumentByReference(anyString())).thenReturn(Future.successful(None))

        val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
          .overrides(
            bind[Navigator].qualifiedWith(Names.named("agentsSh03")).toInstance(withRefStubNavigator),
            bind[PersistentNavigator].qualifiedWith(Names.named("agentsSh03")).toInstance(withRefStubNavigator),
            bind[ParsedStcRowsRepository].toInstance(mockRepository)
          ).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
    
    "onSubmit" - {
      "must save the bulk confirmation details, start the SH03 submission and redirect to the next page" in {
        when(mockRepository.findDocumentByReference(reference)).thenReturn(Future.successful(Some(someTestDoc)))
        when(mockCyaService.paymentDueBy(someTestDoc)).thenReturn(testPaymentDueBy)
        when(mockCyaService.totalTaxDue(someTestDoc)).thenReturn(testTaxDue)
        when(mockNavigator.nextPage(any(), any(), any(), anyBoolean())(any())).thenReturn(Future.successful(testNextPage))
        when(mockTransactionSubmissionService.submitBulkSh03(any())(using any[StcDataRequest[?]]())).thenReturn(Future.successful(true))

        val application = applicationBuilder(userAnswers = Some(completeUserAnswers), sessionRepository = mockSessionRepository)
          .overrides(
            bind[Navigator].qualifiedWith(Names.named("agentsSh03")).toInstance(mockNavigator),
            bind[PersistentNavigator].qualifiedWith(Names.named("agentsSh03")).toInstance(withRefStubNavigator),
            bind[ParsedStcRowsRepository].toInstance(mockRepository),
            bind[CheckYourAnswersService].toInstance(mockCyaService),
            bind[TransactionSubmissionService].toInstance(mockTransactionSubmissionService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual testNextPage.url

          val answers = capturedNavigatorAnswers()

          answers.submissionId mustEqual submissionId
          answers.journeyType mustEqual SH03
          answers.fileUploadReference mustBe Some(reference)
          answers.get(BulkConfirmationDetailsPage).value mustEqual
            BulkConfirmationDetails(
              submissionId = completeUserAnswers.submissionId,
              paymentDueBy = testPaymentDueBy,
              taxDue = testTaxDue,
              agentReference = Some("Reference1")
            )

          verify(mockTransactionSubmissionService).submitBulkSh03(eqTo(expectedRows))(using any[StcDataRequest[?]]())
        }
      }

      "must save no agent reference when one has not been provided" in {
        when(mockRepository.findDocumentByReference(reference)).thenReturn(Future.successful(Some(someTestDoc)))
        when(mockCyaService.paymentDueBy(someTestDoc)).thenReturn(testPaymentDueBy)
        when(mockCyaService.totalTaxDue(someTestDoc)).thenReturn(testTaxDue)
        when(mockNavigator.nextPage(any(), any(), any(), anyBoolean())(any())).thenReturn(Future.successful(testNextPage))
        when(mockTransactionSubmissionService.submitBulkSh03(any())(using any[StcDataRequest[?]]())).thenReturn(Future.successful(true))

        val application = applicationBuilder(userAnswers = Some(answersWithoutAgentRef), sessionRepository = mockSessionRepository)
          .overrides(
            bind[Navigator].qualifiedWith(Names.named("agentsSh03")).toInstance(mockNavigator),
            bind[PersistentNavigator].qualifiedWith(Names.named("agentsSh03")).toInstance(withoutRefStubNavigator),
            bind[ParsedStcRowsRepository].toInstance(mockRepository),
            bind[CheckYourAnswersService].toInstance(mockCyaService),
            bind[TransactionSubmissionService].toInstance(mockTransactionSubmissionService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual testNextPage.url

          val answers = capturedNavigatorAnswers()

          answers.submissionId mustEqual submissionId
          answers.journeyType mustEqual SH03
          answers.fileUploadReference mustBe Some(reference)
          answers.get(BulkConfirmationDetailsPage).value mustEqual
            BulkConfirmationDetails(
              submissionId = answersWithoutAgentRef.submissionId,
              paymentDueBy = testPaymentDueBy,
              taxDue = testTaxDue,
              agentReference = None
            )

          verify(mockTransactionSubmissionService).submitBulkSh03(eqTo(expectedRows))(using any[StcDataRequest[?]]())
        }
      }

      "must redirect to Journey Recovery and not save or submit anything if findDocumentByReference does not find a document" in {
        when(mockRepository.findDocumentByReference(anyString())).thenReturn(Future.successful(None))

        val application = applicationBuilder(userAnswers = Some(completeUserAnswers), sessionRepository = mockSessionRepository)
          .overrides(
            bind[Navigator].qualifiedWith(Names.named("agentsSh03")).toInstance(withRefStubNavigator),
            bind[PersistentNavigator].qualifiedWith(Names.named("agentsSh03")).toInstance(withRefStubNavigator),
            bind[ParsedStcRowsRepository].toInstance(mockRepository),
            bind[CheckYourAnswersService].toInstance(mockCyaService),
            bind[TransactionSubmissionService].toInstance(mockTransactionSubmissionService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockSessionRepository, never()).set(any())
          verify(mockTransactionSubmissionService, never()).submitBulkSh03(any())(using any[StcDataRequest[?]]())
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None, sessionRepository = mockSessionRepository)
          .overrides(
            bind[TransactionSubmissionService].toInstance(mockTransactionSubmissionService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockSessionRepository, never()).set(any())
          verify(mockTransactionSubmissionService, never()).submitBulkSh03(any())(using any[StcDataRequest[?]]())
        }
      }
    }
  }
}
