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

package controllers.stf.organisations.bulk

import base.{FileUploadFixtures, SpecBase}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, anyBoolean, eq as eqTo}
import org.mockito.Mockito.{never, reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Lang
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SubmissionIdClient
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.requests.StcDataRequest
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations.bulk.routes as bulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType.STF
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{ParsedStcRow, ParsedStcRowsDocument, ValidatedStcRow}
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.BulkConfirmationDetails
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.BulkConfirmationDetailsPage
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.bulk.BulkCheckYourAnswersPage
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.ParsedStcRowsRepository
import uk.gov.hmrc.securitiestransferchargefrontend.services.TransactionSubmissionService
import uk.gov.hmrc.securitiestransferchargefrontend.services.stf.bulk.CheckYourAnswersService
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.stf.fileupload.{CheckYourAnswersViewModel, Transfer}
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.organisations.bulk.CheckYourAnswersView

import java.time.LocalDate
import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with MockitoSugar with FileUploadFixtures with BeforeAndAfterEach {

  lazy val checkYourAnswersRoute: String = bulkRoutes.CheckYourAnswersController.onPageLoad(reference).url
  lazy val onSubmitRoute: String = bulkRoutes.CheckYourAnswersController.onSubmit(reference).url

  val row1: ParsedStcRow = parsedStcRow(1)
  val testFileName: String = "test-file.csv"

  private val mockRepository = mock[ParsedStcRowsRepository]
  private val mockCyaService = mock[CheckYourAnswersService]
  private val mockTransactionSubmissionService = mock[TransactionSubmissionService]
  private val mockSubmissionIdClient = mock[SubmissionIdClient]
  private val mockNavigator = mock[Navigator]

  private val parsedStcRowsDocument = ParsedStcRowsDocument(
    _id = reference,
    rows = Seq(row1),
    fileName = testFileName
  )

  private val expectedRows: Seq[ValidatedStcRow] = parsedStcRowsDocument.rows.map(ValidatedStcRow.withNoErrors)
  private val testPaymentDueBy: LocalDate = LocalDate.of(2026, 3, 22)
  private val testTaxDue: BigDecimal = BigDecimal(100.00)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRepository, mockCyaService, mockTransactionSubmissionService, mockSubmissionIdClient, mockNavigator)
  }

  private def capturedNavigatorAnswers(): UserAnswers = {
    val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
    verify(mockNavigator).nextPage(eqTo(BulkCheckYourAnswersPage), eqTo(NormalMode), captor.capture(), eqTo(false))(any())
    captor.getValue
  }

  "STF Organisation Bulk CheckYourAnswersController" - {

    "onPageLoad" - {
      "must return OK and the correct view for a GET when a document exists" in {
        val viewModel = CheckYourAnswersViewModel(
          fileName = testFileName,
          numberOfTransfers = 1,
          taxDue = "£100.00",
          paymentDueBy = "26 September 2026",
          transfers = Seq(
            Transfer(
              seller = "Test Seller",
              securitiesBoughtIn = "Test Company",
              consideration = BigDecimal(10000),
              taxDue = BigDecimal(100)
            )
          )
        )

        when(mockCyaService.buildViewModel(any())(any[Lang])).thenReturn(viewModel)
        when(mockRepository.findDocumentByReference(reference)).thenReturn(Future.successful(Some(parsedStcRowsDocument)))

        val application = applicationBuilder(userAnswers = None, affinityGroup = orgAffinity)
          .overrides(
            bind[Navigator].qualifiedWith("organisations").toInstance(getNavigator),
            bind[CheckYourAnswersService].toInstance(mockCyaService),
            bind[ParsedStcRowsRepository].toInstance(mockRepository)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, checkYourAnswersRoute)
          val result = route(application, request).value
          val view = application.injector.instanceOf[CheckYourAnswersView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(viewModel, testBackLinkRoute, reference)(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery for a GET when no document exists" in {
        when(mockRepository.findDocumentByReference(reference))
          .thenReturn(Future.successful(None))

        val application = applicationBuilder(userAnswers = None, affinityGroup = orgAffinity)
          .overrides(
            bind[Navigator].qualifiedWith("organisations").toInstance(getNavigator),
            bind[ParsedStcRowsRepository].toInstance(mockRepository)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, checkYourAnswersRoute)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {

      "must create user answers with a new submission ID, save the bulk confirmation details, start the STF submission and redirect to the next page" in {
        when(mockRepository.findDocumentByReference(reference)).thenReturn(Future.successful(Some(parsedStcRowsDocument)))
        when(mockSubmissionIdClient.nextSubmissionId()(any())).thenReturn(Future.successful(submissionId))
        when(mockCyaService.paymentDueBy(parsedStcRowsDocument)).thenReturn(testPaymentDueBy)
        when(mockCyaService.totalTaxDue(parsedStcRowsDocument)).thenReturn(testTaxDue)
        when(mockNavigator.nextPage(any(), any(), any(), anyBoolean())(any())).thenReturn(Future.successful(testNextPage))
        when(mockTransactionSubmissionService.submitBulkStf(any())(using any[StcDataRequest[?]]())).thenReturn(Future.successful(true))

        val application =
          applicationBuilder(userAnswers = None, affinityGroup = orgAffinity)
            .overrides(
              bind[Navigator].qualifiedWith("organisations").toInstance(mockNavigator),
              bind[ParsedStcRowsRepository].toInstance(mockRepository),
              bind[CheckYourAnswersService].toInstance(mockCyaService),
              bind[TransactionSubmissionService].toInstance(mockTransactionSubmissionService),
              bind[SubmissionIdClient].toInstance(mockSubmissionIdClient)
            )
            .build()

        running(application) {
          val result = route(application, FakeRequest(POST, onSubmitRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual testNextPage.url

          val answers = capturedNavigatorAnswers()

          answers.submissionId mustEqual submissionId
          answers.journeyType mustEqual STF
          answers.fileUploadReference mustBe Some(reference)
          answers.get(BulkConfirmationDetailsPage).value mustEqual
            BulkConfirmationDetails(
              submissionId = submissionId,
              paymentDueBy = testPaymentDueBy,
              taxDue = testTaxDue,
              agentReference = None
            )

          val requestCaptor = ArgumentCaptor.forClass(classOf[StcDataRequest[?]])
          verify(mockTransactionSubmissionService).submitBulkStf(eqTo(expectedRows))(using requestCaptor.capture())
          requestCaptor.getValue.userAnswers.submissionId mustEqual submissionId
        }
      }

      "must redirect to Journey Recovery and not generate an ID, navigate or submit anything if findDocumentByReference does not find a document" in {
        when(mockRepository.findDocumentByReference(reference)).thenReturn(Future.successful(None))

        val application =
          applicationBuilder(userAnswers = None, affinityGroup = orgAffinity)
            .overrides(
              bind[Navigator].qualifiedWith("organisations").toInstance(mockNavigator),
              bind[ParsedStcRowsRepository].toInstance(mockRepository),
              bind[TransactionSubmissionService].toInstance(mockTransactionSubmissionService),
              bind[SubmissionIdClient].toInstance(mockSubmissionIdClient)
            )
            .build()

        running(application) {
          val result = route(application, FakeRequest(POST, onSubmitRoute)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url

          verify(mockSubmissionIdClient, never()).nextSubmissionId()(any())
          verify(mockNavigator, never()).nextPage(any(), any(), any(), anyBoolean())(any())
          verify(mockTransactionSubmissionService, never()).submitBulkStf(any())(using any[StcDataRequest[?]]())
        }
      }
    }
  }
}