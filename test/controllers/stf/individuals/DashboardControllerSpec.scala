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

package controllers.stf.individuals

import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.DashboardClient
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.routes as individualRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.dashboard.individual.SubmissionsViewModel
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.individuals.DashboardView

import scala.concurrent.Future

class DashboardControllerSpec extends SpecBase with MockitoSugar {

  lazy val dashboardRoute: String = individualRoutes.DashboardController.onPageLoad().url

  val mockDashboardClient: DashboardClient = mock[DashboardClient]
  "Dashboard Controller" - {

    "must return OK and the correct view for a GET" in {

      when(mockDashboardClient.getReadyToPayTransactionsCount(any())(any[HeaderCarrier])).thenReturn(Future.successful(5))
      when(mockDashboardClient.getOverdueTransactionsCount(any())(any[HeaderCarrier])).thenReturn(Future.successful(10))

      val displayName = "Test Name"
      val submissionsViewModel = SubmissionsViewModel(overdueCount = 10, readyToPayCount = 5, draftCount = 1)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = individualAffinity)
        .overrides(inject.bind[DashboardClient].toInstance(mockDashboardClient))
        .build()

      running(application) {
        val request = FakeRequest(GET, dashboardRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DashboardView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(submissionsViewModel, displayName)(request, messages(application)).toString
      }
    }

  }
}
