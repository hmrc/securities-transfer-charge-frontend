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
import base.Fixtures.{overdue,readyToPay,drafts}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.dashboard.routes
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{GroupIdentifier, SubscriptionId, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.SubmissionSummary
import uk.gov.hmrc.securitiestransferchargefrontend.services.DashboardService
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.dashboard.SubmissionsViewModel
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.dashboard.SubmissionsView

import scala.concurrent.Future

class SubmissionsControllerSpec extends SpecBase with MockitoSugar {

  lazy val submissionsRoute: String = routes.SubmissionsController.onPageLoad().url

  val mockDashboardService: DashboardService = mock[DashboardService]
  val recent: Seq[SubmissionSummary] = drafts ++ overdue ++ overdue

  private def mockServiceCalls(): Unit = {
    when(mockDashboardService.getDrafts(any[UserId], any[GroupIdentifier])(any[HeaderCarrier]))
      .thenReturn(Future.successful(drafts))

    when(mockDashboardService.getOverdue(any[SubscriptionId])(any[HeaderCarrier]))
      .thenReturn(Future.successful(overdue))

    when(mockDashboardService.getReadyToPay(any[SubscriptionId])(any[HeaderCarrier]))
      .thenReturn(Future.successful(readyToPay))

    when(mockDashboardService.getRecent(any[UserId], any[GroupIdentifier],any[SubscriptionId])(any[HeaderCarrier]))
      .thenReturn(Future.successful(recent))
  }


  "SubmissionsController" - {

    "must return OK and the correct view" in {

      mockServiceCalls()

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(inject.bind[DashboardService].toInstance(mockDashboardService))
          .build()

      running(application) {

        val request = FakeRequest(GET, submissionsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SubmissionsView]

        implicit val msgs: Messages = messages(application)

        val submissionsViewModel = SubmissionsViewModel.build(
          drafts = drafts,
          readyToPay = readyToPay,
          overdue = overdue,
          recent = recent
        )

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(submissionsViewModel)(request, messages(application)).toString
      }
    }
  }
}