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

package controllers.dashboard

import base.SpecBase
import base.Fixtures.*
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import uk.gov.hmrc.auth.core.AffinityGroup
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.dashboard.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes as appRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.SttSubmission
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.GetTransactionResult
import uk.gov.hmrc.securitiestransferchargefrontend.services.{DashboardService, EtmpSubmissionService}
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.dashboard.TransferDetailsViewModel
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.dashboard.TransferDetailsView

import scala.concurrent.Future

class TransferDetailsControllerSpec extends SpecBase {

  private val firstTransfer = readyToPaySttTransferTaxOnly.copy(utrn = "900459000001")
  private val secondTransfer = readyToPaySttTransferWithLateFilingCharge.copy(utrn = "900459000002")
  private val bulkSubmission: SttSubmission = singleSubmissionReadyToPay.copy(transfers = Seq(firstTransfer, secondTransfer))

  private def setUp(
                     submission: Future[SttSubmission],
                     transaction: Future[GetTransactionResult],
                     affinityGroup: AffinityGroup = AffinityGroup.Individual
                   ) = {
    val dashboardService = mock[DashboardService]
    val etmpSubmissionService = mock[EtmpSubmissionService]
    when(dashboardService.getSubmission(any(), any())(any())).thenReturn(submission)
    when(etmpSubmissionService.getTransaction(any(), any())(any())).thenReturn(transaction)

    val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = affinityGroup)
      .overrides(
        bind[DashboardService].toInstance(dashboardService),
        bind[EtmpSubmissionService].toInstance(etmpSubmissionService)
      )
      .build()

    (application, etmpSubmissionService)
  }

  private def url(transferNumber: Int) =
    routes.TransferDetailsController.onPageLoad(testSubmissionId.toString, transferNumber).url

  "TransferDetails Controller" - {

    "must return OK and the correct view for the requested transfer" in {
      val (application, etmpSubmissionService) =
        setUp(Future.successful(bulkSubmission), Future.successful(GetTransactionResult.Found(viewTransferResponse)))

      running(application) {
        val request = FakeRequest(GET, url(2))
        val result = route(application, request).value

        val view = application.injector.instanceOf[TransferDetailsView]
        val viewModel = TransferDetailsViewModel.build(bulkSubmission, 2, secondTransfer, viewTransferResponse, affinityGroupKeyInd)(messages(application))

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(viewModel)(request, messages(application)).toString
        verify(etmpSubmissionService).getTransaction(any(), eqTo(secondTransfer.utrn))(any())
      }
    }

    "must show the buyer details for an agent" in {
      val (application, _) =
        setUp(Future.successful(bulkSubmission), Future.successful(GetTransactionResult.Found(viewTransferResponse)), AffinityGroup.Agent)

      running(application) {
        val request = FakeRequest(GET, url(2))
        val result = route(application, request).value

        val view = application.injector.instanceOf[TransferDetailsView]
        val viewModel = TransferDetailsViewModel.build(bulkSubmission, 2, secondTransfer, viewTransferResponse, affinityGroupKeyAgent)(messages(application))

        status(result) mustEqual OK
        viewModel.buyerList mustBe defined
        contentAsString(result) mustEqual view(viewModel)(request, messages(application)).toString
        contentAsString(result) must include(messages(application)("transferDetails.buyer.heading"))
      }
    }

    "must not show the buyer details for an organisation" in {
      val (application, _) =
        setUp(Future.successful(bulkSubmission), Future.successful(GetTransactionResult.Found(viewTransferResponse)), AffinityGroup.Organisation)

      running(application) {
        val request = FakeRequest(GET, url(2))
        val result = route(application, request).value

        val view = application.injector.instanceOf[TransferDetailsView]
        val viewModel = TransferDetailsViewModel.build(bulkSubmission, 2, secondTransfer, viewTransferResponse, affinityGroupKeyOrg)(messages(application))

        status(result) mustEqual OK
        viewModel.buyerList mustBe None
        contentAsString(result) mustEqual view(viewModel)(request, messages(application)).toString
        contentAsString(result) must not include messages(application)("transferDetails.buyer.heading")
      }
    }

    "must use transfer-1 for a single submission" in {
      val (application, etmpSubmissionService) =
        setUp(Future.successful(singleSubmissionReadyToPay), Future.successful(GetTransactionResult.Found(viewTransferResponse)))

      running(application) {
        val result = route(application, FakeRequest(GET, url(1))).value

        status(result) mustEqual OK
        verify(etmpSubmissionService).getTransaction(any(), eqTo(readyToPaySttTransferTaxOnly.utrn))(any())
      }
    }

    "must redirect to Journey Recovery when the transfer number is not in the submission" in {
      val (application, etmpSubmissionService) =
        setUp(Future.successful(bulkSubmission), Future.successful(GetTransactionResult.Found(viewTransferResponse)))

      running(application) {
        val result = route(application, FakeRequest(GET, url(3))).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual appRoutes.JourneyRecoveryController.onPageLoad().url
        verify(etmpSubmissionService, never()).getTransaction(any(), any())(any())
      }
    }

    "must redirect to Journey Recovery for transfer 0" in {
      val (application, _) =
        setUp(Future.successful(bulkSubmission), Future.successful(GetTransactionResult.Found(viewTransferResponse)))

      running(application) {
        val result = route(application, FakeRequest(GET, url(0))).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual appRoutes.JourneyRecoveryController.onPageLoad().url
      }
    }

    Seq(
      GetTransactionResult.UtrnNotFound,
      GetTransactionResult.StcIdNotFound,
      GetTransactionResult.InvalidRequest
    ).foreach { notFound =>
      s"must redirect to Journey Recovery when getting the transaction returns $notFound" in {
        val (application, _) = setUp(Future.successful(singleSubmissionReadyToPay), Future.successful(notFound))

        running(application) {
          val result = route(application, FakeRequest(GET, url(1))).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual appRoutes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "must fail when the submission cannot be retrieved" in {
      val (application, _) =
        setUp(Future.failed(new RuntimeException("search failed")), Future.successful(GetTransactionResult.UtrnNotFound))

      running(application) {
        val result = route(application, FakeRequest(GET, url(1))).value

        whenReady(result.failed) { _.getMessage mustEqual "search failed" }
      }
    }

    "must not match a transfer path that is not a number" in {
      val (application, _) =
        setUp(Future.successful(singleSubmissionReadyToPay), Future.successful(GetTransactionResult.UtrnNotFound))

      running(application) {
        val nonNumericUrl = url(1).replace("transfer-1", "transfer-abc")
        val result = route(application, FakeRequest(GET, nonNumericUrl)).value

        status(result) mustEqual NOT_FOUND
      }
    }
  }
}
