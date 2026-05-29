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

package controllers.stf.agents.single

import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.agents.single.routes as agentSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.agents.TotalMarketValueFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.TotalMarketValuePage
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.SessionRepository
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.agents.single.TotalMarketValueView

import scala.concurrent.Future
import scala.language.postfixOps

class TotalMarketValueControllerSpec extends SpecBase with MockitoSugar {

  val formProvider = new TotalMarketValueFormProvider()
  val form: Form[BigDecimal] = formProvider()

  val validAnswer = 0

  lazy val totalMarketValueRoute: String = agentSingleRoutes.TotalMarketValueController.onPageLoad(NormalMode).url

  "TotalMarketValue Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = agentAffinity)
        .overrides(bind[Navigator].qualifiedWith("agents").toInstance(getNavigator))
        .build()

      running(application) {
        val request = FakeRequest(GET, totalMarketValueRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[TotalMarketValueView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = emptyUserAnswers.set(TotalMarketValuePage, validAnswer).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers), affinityGroup = agentAffinity)
        .overrides(bind[Navigator].qualifiedWith("agents").toInstance(getNavigator))
        .build()

      running(application) {
        val request = FakeRequest(GET, totalMarketValueRoute)

        val view = application.injector.instanceOf[TotalMarketValueView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(validAnswer), NormalMode, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(())

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers), sessionRepository = mockSessionRepository, affinityGroup = agentAffinity)
          .overrides(bind[Navigator].qualifiedWith("agents").toInstance(getNavigator))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, totalMarketValueRoute)
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual testNextPage.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = agentAffinity)
          .overrides(bind[Navigator].qualifiedWith("agents").toInstance(getNavigator))
          .build()
          
      running(application) {
        val request =
          FakeRequest(POST, totalMarketValueRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[TotalMarketValueView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None, affinityGroup = agentAffinity).build()

      running(application) {
        val request = FakeRequest(GET, totalMarketValueRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None, affinityGroup = agentAffinity).build()

      running(application) {
        val request =
          FakeRequest(POST, totalMarketValueRoute)
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
