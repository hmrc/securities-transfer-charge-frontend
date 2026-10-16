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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.dashboard.routes
import uk.gov.hmrc.securitiestransferchargefrontend.services.DashboardService
import uk.gov.hmrc.securitiestransferchargefrontend.services.stf.shared.FormattingService
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.dashboard.SubmissionDetailsViewModel
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.dashboard.SubmissionDetailsView

import scala.concurrent.Future

class SubmissionDetailsControllerSpec extends SpecBase {

  "SubmissionDetails Controller" - {

    "must return OK and the correct view for a GET" in {

      val mockDashboardService = mock[DashboardService]

      when(mockDashboardService.getSubmission(any(), any())(any()))
        .thenReturn(Future.successful(singleSubmissionReadyToPay))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[DashboardService].toInstance(mockDashboardService))
        .build()


      running(application) {
        val request = FakeRequest(GET, routes.SubmissionDetailsController.onPageLoad(testSubmissionId.toString).url)

        val formatter = application.injector.instanceOf[FormattingService]
        val view = application.injector.instanceOf[SubmissionDetailsView]
        val viewModel: SubmissionDetailsViewModel =
          SubmissionDetailsViewModel.build(singleSubmissionReadyToPay, formatter)(messages(application))

        val result = route(application, request).value


        status(result) mustEqual OK
        contentAsString(result) mustEqual view(viewModel)(request, messages(application)).toString
      }
    }
  }
}
