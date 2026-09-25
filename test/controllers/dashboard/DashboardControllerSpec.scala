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
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.dashboard.routes
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{GroupIdentifier, SubscriptionId, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.services.{DashboardCounts, DashboardService}
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.dashboard.SubmissionsViewModel
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.dashboard.DashboardView

import scala.concurrent.Future

class DashboardControllerSpec extends SpecBase with MockitoSugar {

  lazy val dashboardRoute: String = routes.DashboardController.onPageLoad().url

  val mockDashboardService: DashboardService = mock[DashboardService]

  private val submissionsViewModel = SubmissionsViewModel(overdueCount = 10, readyToPayCount = 5, draftCount = 1)

  private def mockCounts(): Unit =
    when(
      mockDashboardService.getCounts(any[UserId], any[GroupIdentifier], any[SubscriptionId])(any[HeaderCarrier])
    ).thenReturn(Future.successful(DashboardCounts(overdue = 10, readyToPay = 5, drafts = 1)))

  "DashboardController" - {

    "must return OK and the correct view for an Individual" in {

      mockCounts()

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = AffinityGroup.Individual)
          .overrides(inject.bind[DashboardService].toInstance(mockDashboardService))
          .build()

      running(application) {

        val request = FakeRequest(GET, dashboardRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DashboardView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(submissionsViewModel, "First Last", true)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for an Agent" in {

      mockCounts()

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = AffinityGroup.Agent)
          .overrides(inject.bind[DashboardService].toInstance(mockDashboardService))
          .build()

      running(application) {

        val request = FakeRequest(GET, dashboardRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DashboardView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(submissionsViewModel, "Agent Name", false)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for an Organisation" in {

      mockCounts()

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = AffinityGroup.Organisation)
          .overrides(inject.bind[DashboardService].toInstance(mockDashboardService))
          .build()

      running(application) {

        val request = FakeRequest(GET, dashboardRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DashboardView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(submissionsViewModel, "Securities Transfer Tax", false)(request, messages(application)).toString
      }
    }
  }
}
