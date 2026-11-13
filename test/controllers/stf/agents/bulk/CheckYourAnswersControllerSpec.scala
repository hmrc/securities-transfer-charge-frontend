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

package controllers.stf.agents.bulk

import base.SpecBase
import base.stubs.StubPersistentNavigator
import com.google.inject.name.Names
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, anyBoolean, anyString, eq => eqTo}
import org.mockito.Mockito.{never, reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.requests.StcDataRequest
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.agents.bulk.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.models.shared.AgentReference
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.HowToNotifyAboutSecuritiesTransfer
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{ParsedStcRow, ParsedStcRowsDocument, ParsedValue, ValidatedStcRow}
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.BulkConfirmationDetails
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.{Navigator, PersistentNavigator}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.BulkConfirmationDetailsPage
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.bulk.{BulkAgentReferencePage, BulkCheckYourAnswersPage}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.HowToNotifyAboutSecuritiesTransferPage
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.{CyaHtmlData, CyaHtmlRepository, ParsedStcRowsRepository}
import uk.gov.hmrc.securitiestransferchargefrontend.services.TransactionSubmissionService
import uk.gov.hmrc.securitiestransferchargefrontend.services.stf.agents.bulk.CheckYourAnswersService

import java.time.{Instant, LocalDate}
import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with BeforeAndAfterEach {

  private def answersWithoutAgentRef: UserAnswers =
    emptyUserAnswers
      .set(HowToNotifyAboutSecuritiesTransferPage, HowToNotifyAboutSecuritiesTransfer.MoreThanOneAtATime).success.value
      .setFileUploadReference("testReference")

  private def completeUserAnswers: UserAnswers =
    answersWithoutAgentRef
      .set(BulkAgentReferencePage, AgentReference(Some("Reference1"))).success.value

  private val validRow: ParsedStcRow =
    ParsedStcRow(
      rowNumber = 3,
      buyerName = Some("Bob buyer"),
      buyerAddressInUK = Some(true),
      buyerAddressLine1 = Some("1 Seller Street"),
      buyerAddressLine2 = Some("Seller District"),
      buyerAddressLine3 = Some("Seller City"),
      buyerAddressLine4 = None,
      buyerPostcode = Some("AA1 1AA"),
      buyerCountry = Some("United Kingdom"),
      sellerName = Some("Seller Ltd"),
      sellerAddressInUK = Some(true),
      sellerAddressLine1 = Some("1 Test"),
      sellerAddressLine2 = Some("Test Region"),
      sellerAddressLine3 = None,
      sellerAddressLine4 = None,
      sellerPostcode = Some("AA1 1AA"),
      sellerCountry = None,
      connectedPersons = Some(true),
      applyingForRelief = Some(false),
      whatReliefAreYouApplyingFor = None,
      securitiesTarget = Some("Target Ltd"),
      companyRegistrationNumber = Some("12345678"),
      chargingPoint = ParsedValue.Valid(LocalDate.of(2026, 2, 20)),
      taxRate = Some(BigDecimal("0.5")),
      whatTypeOfSecurities = Some("Shares"),
      typeOfShares = Some("Ordinary"),
      securitiesQuantity = Some("10000"),
      amountPaidForSecurities = Some("15000"),
      totalMarketValue = Some("20000"),
      minSharePrice = None,
      maxSharePrice = None,
      sharePurchaseReason = None,
      purchaseForCancellation = None,
      purchasingShares = None
    )

  private val rows: Seq[ParsedStcRow] = Seq(validRow)
  private val someTestDoc: ParsedStcRowsDocument = ParsedStcRowsDocument(_id = "id", rows = rows, createdAt = Instant.now, fileName = "testFile")

  private val expectedRows: Seq[ValidatedStcRow] = someTestDoc.rows.map(ValidatedStcRow.withNoErrors)
  private val testPaymentDueBy: LocalDate = LocalDate.of(2026, 3, 22)
  private val testTaxDue: BigDecimal = BigDecimal(75.00)

  private val mockRepository = mock[ParsedStcRowsRepository]
  private val mockCyaService = mock[CheckYourAnswersService]
  private val mockTransactionSubmissionService = mock[TransactionSubmissionService]
  private val mockNavigator = mock[Navigator]
  private val mockCyaHtmlRepository = mock[CyaHtmlRepository]

  private val withRefStubNavigator = new StubPersistentNavigator(testNextPage, completeUserAnswers, "stf", "")
  private val withoutRefStubNavigator = new StubPersistentNavigator(testNextPage, answersWithoutAgentRef, "stf", "")

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRepository, mockCyaService, mockTransactionSubmissionService, mockNavigator, mockCyaHtmlRepository)
    when(mockCyaHtmlRepository.store(any[CyaHtmlData]())).thenReturn(Future.successful(()))
  }

  private def capturedNavigatorAnswers(): UserAnswers = {
    val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
    verify(mockNavigator).nextPage(eqTo(BulkCheckYourAnswersPage), eqTo(NormalMode), captor.capture(), anyBoolean())(any())
    captor.getValue
  }

  "STF Agent Bulk CheckYourAnswersController" - {

    "onPageLoad" - {
      "must return OK and the correct view for a GET" in {
        when(mockRepository.findDocumentByReference("testReference")).thenReturn(Future.successful(Some(someTestDoc)))

        val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
          .overrides(
            bind[Navigator].qualifiedWith(Names.named("agents")).toInstance(withRefStubNavigator),
            bind[PersistentNavigator].qualifiedWith(Names.named("agents")).toInstance(withRefStubNavigator),
            bind[ParsedStcRowsRepository].toInstance(mockRepository),
            bind[CyaHtmlRepository].toInstance(mockCyaHtmlRepository)
          ).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) must include("Check your answers")
          contentAsString(result) must include("File details")

          val captor = ArgumentCaptor.forClass(classOf[CyaHtmlData])
          verify(mockCyaHtmlRepository).store(captor.capture())
          captor.getValue.submissionId mustEqual completeUserAnswers.submissionId
          captor.getValue.html.toString mustEqual contentAsString(result)
        }
      }

      "must redirect to Journey Recovery and not store the CYA HTML for a GET if findDocumentByReference does not find a document" in {
        when(mockRepository.findDocumentByReference(anyString())).thenReturn(Future.successful(None))

        val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
          .overrides(
            bind[Navigator].qualifiedWith(Names.named("agents")).toInstance(withRefStubNavigator),
            bind[PersistentNavigator].qualifiedWith(Names.named("agents")).toInstance(withRefStubNavigator),
            bind[ParsedStcRowsRepository].toInstance(mockRepository),
            bind[CyaHtmlRepository].toInstance(mockCyaHtmlRepository)
          ).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockCyaHtmlRepository, never()).store(any[CyaHtmlData]())
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
      "must save the bulk confirmation details, start the STF submission and redirect to the next page" in {
        when(mockRepository.findDocumentByReference("testReference")).thenReturn(Future.successful(Some(someTestDoc)))
        when(mockCyaService.paymentDueBy(someTestDoc)).thenReturn(testPaymentDueBy)
        when(mockCyaService.totalTaxDue(someTestDoc)).thenReturn(testTaxDue)
        when(mockNavigator.nextPage(any(), any(), any(), anyBoolean())(any())).thenReturn(Future.successful(testNextPage))
        when(mockTransactionSubmissionService.submitBulkStf(any())(using any[StcDataRequest[?]]())).thenReturn(Future.successful(true))

        val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
          .overrides(
            bind[Navigator].qualifiedWith(Names.named("agents")).toInstance(mockNavigator),
            bind[PersistentNavigator].qualifiedWith(Names.named("agents")).toInstance(withRefStubNavigator),
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

          capturedNavigatorAnswers().get(BulkConfirmationDetailsPage).value mustEqual
            BulkConfirmationDetails(
              submissionId = completeUserAnswers.submissionId,
              paymentDueBy = testPaymentDueBy,
              taxDue = testTaxDue,
              agentReference = Some("Reference1")
            )

          verify(mockTransactionSubmissionService).submitBulkStf(eqTo(expectedRows))(using any[StcDataRequest[?]]())
        }
      }

      "must save no agent reference when one has not been provided" in {
        when(mockRepository.findDocumentByReference("testReference")).thenReturn(Future.successful(Some(someTestDoc)))
        when(mockCyaService.paymentDueBy(someTestDoc)).thenReturn(testPaymentDueBy)
        when(mockCyaService.totalTaxDue(someTestDoc)).thenReturn(testTaxDue)
        when(mockNavigator.nextPage(any(), any(), any(), anyBoolean())(any())).thenReturn(Future.successful(testNextPage))
        when(mockTransactionSubmissionService.submitBulkStf(any())(using any[StcDataRequest[?]]())).thenReturn(Future.successful(true))

        val application = applicationBuilder(userAnswers = Some(answersWithoutAgentRef))
          .overrides(
            bind[Navigator].qualifiedWith(Names.named("agents")).toInstance(mockNavigator),
            bind[PersistentNavigator].qualifiedWith(Names.named("agents")).toInstance(withoutRefStubNavigator),
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

          capturedNavigatorAnswers().get(BulkConfirmationDetailsPage).value mustEqual
            BulkConfirmationDetails(
              submissionId = answersWithoutAgentRef.submissionId,
              paymentDueBy = testPaymentDueBy,
              taxDue = testTaxDue,
              agentReference = None
            )

          verify(mockTransactionSubmissionService).submitBulkStf(eqTo(expectedRows))(using any[StcDataRequest[?]]())
        }
      }

      "must redirect to Journey Recovery and not navigate or submit anything if findDocumentByReference does not find a document" in {
        when(mockRepository.findDocumentByReference(anyString())).thenReturn(Future.successful(None))

        val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
          .overrides(
            bind[Navigator].qualifiedWith(Names.named("agents")).toInstance(mockNavigator),
            bind[PersistentNavigator].qualifiedWith(Names.named("agents")).toInstance(withRefStubNavigator),
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

          verify(mockNavigator, never()).nextPage(any(), any(), any(), anyBoolean())(any())
          verify(mockTransactionSubmissionService, never()).submitBulkStf(any())(using any[StcDataRequest[?]]())
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None)
          .overrides(
            bind[Navigator].qualifiedWith(Names.named("agents")).toInstance(mockNavigator),
            bind[TransactionSubmissionService].toInstance(mockTransactionSubmissionService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockNavigator, never()).nextPage(any(), any(), any(), anyBoolean())(any())
          verify(mockTransactionSubmissionService, never()).submitBulkStf(any())(using any[StcDataRequest[?]]())
        }
      }
    }
  }
}