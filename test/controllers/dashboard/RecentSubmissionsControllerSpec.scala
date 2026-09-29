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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.dashboard.routes as dashboardRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.{SubmissionStatus, SubmissionSummary}
import uk.gov.hmrc.securitiestransferchargefrontend.services.{DashboardService}
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.dashboard.RecentSubmissionsView

import java.time.LocalDate
import scala.concurrent.Future

class RecentSubmissionsControllerSpec extends SpecBase with MockitoSugar {

  lazy val recentSubmissionsRoute: String = dashboardRoutes.RecentSubmissionsController.onPageLoad().url

  val mockDashboardService: DashboardService = mock[DashboardService]

  "RecentSubmissions Controller" - {

    "must return OK and the empty submissions view when there are no recent submissions" in {

      when(mockDashboardService.getRecent(any(), any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Seq.empty))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = individualAffinity)
        .overrides(inject.bind[DashboardService].toInstance(mockDashboardService))
        .build()

      running(application) {
        val request = FakeRequest(GET, recentSubmissionsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RecentSubmissionsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(request, messages(application)).toString
      }
    }

    "must redirect to the Dashboard when recent submissions exist" in {

      val recentSummaries = Seq(
        SubmissionSummary("STC-001", Some("2026-10-10"), SubmissionStatus.ReadyToPay, LocalDate.parse("2026-09-10"))
      )

      when(mockDashboardService.getRecent(any(), any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(recentSummaries))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = individualAffinity)
        .overrides(inject.bind[DashboardService].toInstance(mockDashboardService))
        .build()

      running(application) {
        val request = FakeRequest(GET, recentSubmissionsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual dashboardRoutes.DashboardController.onPageLoad().url
      }
    }
  }
}