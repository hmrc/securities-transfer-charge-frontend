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
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.single.routes as sh03AgentsRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.agents.single.TreasurySharesFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.single.TreasurySharesPage
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.agents.single.TreasurySharesView

class TreasurySharesControllerSpec extends SpecBase with MockitoSugar {
  
  val formProvider = new TreasurySharesFormProvider()
  val form: Form[Boolean] = formProvider()

  lazy val treasurySharesRoute: String = sh03AgentsRoutes.TreasurySharesController.onPageLoad(NormalMode).url

  "TreasuryShares Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = agentAffinity)
        .overrides(bind[Navigator].qualifiedWith("agentsSh03").toInstance(getNavigator))
        .build()

      running(application) {
        val request = FakeRequest(GET, treasurySharesRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[TreasurySharesView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = testUserAnswers.set(TreasurySharesPage, true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers), affinityGroup = agentAffinity)
        .overrides(bind[Navigator].qualifiedWith("agentsSh03").toInstance(getNavigator))
        .build()

      running(application) {
        val request = FakeRequest(GET, treasurySharesRoute)

        val view = application.injector.instanceOf[TreasurySharesView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {


      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = agentAffinity)
        .overrides(bind[Navigator].qualifiedWith("agentsSh03").toInstance(getNavigator))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, treasurySharesRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual testNextPage.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = agentAffinity)
        .overrides(bind[Navigator].qualifiedWith("agentsSh03").toInstance(getNavigator))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, treasurySharesRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[TreasurySharesView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, testBackLinkRoute)(request, messages(application)).toString
      }
    }
    
    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None, affinityGroup = agentAffinity).build()

      running(application) {
        val request =
          FakeRequest(POST, treasurySharesRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
