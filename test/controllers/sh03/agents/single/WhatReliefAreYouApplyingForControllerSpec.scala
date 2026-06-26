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

import base.Fixtures.testUserAnswers
import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SaveAndReturnClient
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.single.routes as agentSingle
import uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.agents.WhatReliefAreYouApplyingForFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, ReliefsDataSource, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.single.WhatReliefAreYouApplyingForPage
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.agents.single.WhatReliefAreYouApplyingForView

import scala.concurrent.Future

class WhatReliefAreYouApplyingForControllerSpec extends SpecBase with MockitoSugar {
  
  val formProvider = new WhatReliefAreYouApplyingForFormProvider()
  val form: Form[String] = formProvider()

  lazy val whatReliefAreYouApplyingForRoute: String = agentSingle.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode).url

  "WhatReliefAreYouApplyingFor Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = agentAffinity)
        .overrides(bind[Navigator].qualifiedWith("agentsSh03").toInstance(getNavigator))
        .build()

      running(application) {
        val request = FakeRequest(GET, whatReliefAreYouApplyingForRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[WhatReliefAreYouApplyingForView]

        val reliefsDataSource = application.injector.instanceOf[ReliefsDataSource]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode,reliefsDataSource.reliefs, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = testUserAnswers.set(WhatReliefAreYouApplyingForPage, "answer").success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers), affinityGroup = agentAffinity)
        .overrides(bind[Navigator].qualifiedWith("agentsSh03").toInstance(getNavigator))
        .build()

      running(application) {
        val request = FakeRequest(GET, whatReliefAreYouApplyingForRoute)

        val view = application.injector.instanceOf[WhatReliefAreYouApplyingForView]

        val reliefsDataSource = application.injector.instanceOf[ReliefsDataSource]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill("answer"), NormalMode,reliefsDataSource.reliefs, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      
      when(saveAndReturnClient.save(any[UserAnswers]())(any[HeaderCarrier]()))
        .thenReturn(Future.successful(()))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = agentAffinity)
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whatReliefAreYouApplyingForRoute)
            .withFormUrlEncodedBody(("reliefs", "test-relief"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = agentAffinity)
        .overrides(bind[Navigator].qualifiedWith("agentsSh03").toInstance(getNavigator))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, whatReliefAreYouApplyingForRoute)
            .withFormUrlEncodedBody(("reliefs", ""))

        val boundForm = form.bind(Map("reliefs" -> ""))

        val view = application.injector.instanceOf[WhatReliefAreYouApplyingForView]
        val reliefsDataSource = application.injector.instanceOf[ReliefsDataSource]


        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode,reliefsDataSource.reliefs, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None, affinityGroup = agentAffinity).build()

      running(application) {
        val request = FakeRequest(GET, whatReliefAreYouApplyingForRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, whatReliefAreYouApplyingForRoute)
            .withFormUrlEncodedBody(("reliefs", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
