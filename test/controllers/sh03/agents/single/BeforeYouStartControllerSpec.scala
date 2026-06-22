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

package controllers.sh03.agents.single


import base.SpecBase
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.single.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.routes as agentSH03Route
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.agents.single.BeforeYouStartView

class BeforeYouStartControllerSpec extends SpecBase {

  private lazy val onPageLoadRoute = routes.BeforeYouStartController.onPageLoad().url

  private lazy val onSubmitRoute = routes.BeforeYouStartController.onSubmit().url

  "BeforeYouStartController" - {

    "must return OK and the correct view for a GET" in {

      val application =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          affinityGroup = agentAffinity
        ).build()

      running(application) {

        val request = FakeRequest(GET, onPageLoadRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[BeforeYouStartView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view()(request, messages(application)).toString
      }
    }

    //Todo test once next page is in
    "must redirect to the journey recovery page on submit" in {

      val application =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          affinityGroup = agentAffinity
        ).build()

      running(application) {

        val request = FakeRequest(POST, onSubmitRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual agentSH03Route.HowToNotifyAboutShareBuybackController.onPageLoad(NormalMode).url
      }
    }
  }
}
