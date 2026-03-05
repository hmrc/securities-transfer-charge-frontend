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

package controllers.stf.organisations

import base.SpecBase
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations.routes as orgRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.individuals.TaxRateFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, TaxRate, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.TaxRatePage
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.organisations.TaxRateView

class TaxRateControllerSpec extends SpecBase with MockitoSugar {
  
  lazy val taxRateRoute: String = orgRoutes.TaxRateController.onPageLoad(NormalMode).url

  val formProvider = new TaxRateFormProvider()
  val form: Form[TaxRate] = formProvider()

  "TaxRate Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers),affinityGroup = AffinityGroup.Organisation)
        .overrides(bind[Navigator].qualifiedWith("organisations").toInstance(getNavigator))
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

      val userAnswers = UserAnswers(userAnswersId, submissionId).set(TaxRatePage, TaxRate.values.head).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers), affinityGroup = AffinityGroup.Organisation)
        .overrides(bind[Navigator].qualifiedWith("organisations").toInstance(getNavigator))
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
      val userAnswers = UserAnswers(userAnswersId, submissionId).set(TaxRatePage, TaxRate.values.head).success.value

      

      val application =
        applicationBuilder(userAnswers = Some(userAnswers),affinityGroup = AffinityGroup.Organisation)
          .build()

      running(application) {
        val request =
          FakeRequest(POST, taxRateRoute)
            .withFormUrlEncodedBody(("value", TaxRate.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual orgRoutes.WhatTypeOfSecuritiesController.onPageLoad(NormalMode).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = AffinityGroup.Organisation)
        .overrides(bind[Navigator].qualifiedWith("organisations").toInstance(getNavigator))
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

      val application = applicationBuilder(userAnswers = None, affinityGroup = AffinityGroup.Organisation).build()

      running(application) {
        val request = FakeRequest(GET, taxRateRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None, affinityGroup = AffinityGroup.Organisation).build()

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
