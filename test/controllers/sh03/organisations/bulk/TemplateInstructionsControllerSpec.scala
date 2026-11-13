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

package controllers.sh03.organisations.bulk

import base.SpecBase
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes.JourneyRecoveryController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.dashboard.routes as dashboardRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.organisations.bulk.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType.SH03
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.SessionRepository
import uk.gov.hmrc.securitiestransferchargefrontend.utils.CommonHelpers
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.organisations.bulk.TemplateInstructionsView

import scala.concurrent.Future

class TemplateInstructionsControllerSpec extends SpecBase {

  "SH03 Organisation Bulk TemplateInstructionsController" - {

    "onPageLoad" - {
      "must return OK and the correct view for a GET" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = orgAffinity).build()

        running(application) {
          val request = FakeRequest(GET, routes.TemplateInstructionsController.onPageLoad().url)
          val result = route(application, request).value
          val view = application.injector.instanceOf[TemplateInstructionsView]
          val templateUrl = CommonHelpers.linkToTemplateFor(SH03)(orgAffinity)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(templateUrl)(request, messages(application)).toString
        }
      }

      "must redirect to journey recovery if there are no user answers" in {
        val application = applicationBuilder(affinityGroup = orgAffinity).build()

        running(application) {
          val request = FakeRequest(GET, routes.TemplateInstructionsController.onReturnToDashboard().url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "onReturnToDashboard" - {
      "must clear the user answers and redirect to the dashboard" in {
        val mockSessionRepository = mock[SessionRepository]
        when(mockSessionRepository.clear(any())).thenReturn(Future.successful(()))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = orgAffinity, sessionRepository = mockSessionRepository).build()

        running(application) {
          val request = FakeRequest(GET, routes.TemplateInstructionsController.onReturnToDashboard().url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual dashboardRoutes.DashboardController.onPageLoad().url

          verify(mockSessionRepository).clear(eqTo(testUserId))
        }
      }

      "must redirect to journey recovery if there are no user answers" in {
        val mockSessionRepository = mock[SessionRepository]
        when(mockSessionRepository.clear(any())).thenReturn(Future.failed(new RuntimeException("boom")))

        val application = applicationBuilder(affinityGroup = orgAffinity, sessionRepository = mockSessionRepository).build()

        running(application) {
          val request = FakeRequest(GET, routes.TemplateInstructionsController.onReturnToDashboard().url)
          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}