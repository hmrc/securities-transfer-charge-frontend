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

package controllers.stf.individuals.single

import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.single.routes as individualRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.individuals.TaxRateFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.TaxRate
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.TaxRatePage
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.SessionRepository
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.individuals.single.TaxRateView

import scala.concurrent.Future

class TaxRateControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  lazy val taxRateRoute: String = individualRoutes.TaxRateController.onPageLoad(NormalMode).url

  val formProvider = new TaxRateFormProvider()
  val form: Form[TaxRate] = formProvider()

  "TaxRate Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = individualAffinity)
        .overrides(bind[Navigator].qualifiedWith("individuals").toInstance(getNavigator))
        .build()

      running(application) {
        val request = FakeRequest(GET, taxRateRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[TaxRateView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(testUserId, testGroupIdentifier, submissionId).set(TaxRatePage, TaxRate.values.head).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers), affinityGroup = individualAffinity)
        .overrides(bind[Navigator].qualifiedWith("individuals").toInstance(getNavigator))
        .build()

      running(application) {
        val request = FakeRequest(GET, taxRateRoute)

        val view = application.injector.instanceOf[TaxRateView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(TaxRate.values.head), NormalMode, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val userAnswers = UserAnswers(testUserId, testGroupIdentifier, submissionId).set(TaxRatePage, TaxRate.values.head).success.value

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(())

      val application =
        applicationBuilder(userAnswers = Some(userAnswers), sessionRepository = mockSessionRepository, affinityGroup = individualAffinity)
          .build()

      running(application) {
        val request =
          FakeRequest(POST, taxRateRoute)
            .withFormUrlEncodedBody(("value", TaxRate.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual individualRoutes.WhatTypeOfSecuritiesController.onPageLoad(NormalMode).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = individualAffinity)
        .overrides(bind[Navigator].qualifiedWith("individuals").toInstance(getNavigator))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, taxRateRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[TaxRateView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None, affinityGroup = individualAffinity).build()

      running(application) {
        val request = FakeRequest(GET, taxRateRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None, affinityGroup = individualAffinity).build()

      running(application) {
        val request =
          FakeRequest(POST, taxRateRoute)
            .withFormUrlEncodedBody(("value", TaxRate.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
