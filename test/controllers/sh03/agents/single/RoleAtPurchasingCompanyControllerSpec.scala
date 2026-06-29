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
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes.JourneyRecoveryController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.single.routes
import uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.agents.single.RoleAtPurchasingCompanyFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.agents.RoleAtPurchasingCompany
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.agents.RoleAtPurchasingCompanyPage
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.agents.single.RoleAtPurchasingCompanyView

class RoleAtPurchasingCompanyControllerSpec extends SpecBase {

  lazy val roleAtPurchasingCompanyRoute: String = routes.RoleAtPurchasingCompanyController.onPageLoad(NormalMode).url

  val formProvider = new RoleAtPurchasingCompanyFormProvider()
  val form: Form[RoleAtPurchasingCompany] = formProvider()

  val validData: RoleAtPurchasingCompany = RoleAtPurchasingCompany("director", None)
  val validUkSocietasData: RoleAtPurchasingCompany = RoleAtPurchasingCompany("ukSocietas", Some("Management Board"))

  "RoleAtPurchasingCompany Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = agentAffinity)
        .overrides(bind[Navigator].qualifiedWith("agentsSh03").toInstance(getNavigator))
        .build()

      running(application) {
        val request = FakeRequest(GET, roleAtPurchasingCompanyRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RoleAtPurchasingCompanyView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered (Standard Role)" in {

      val userAnswers = emptyUserAnswers.set(RoleAtPurchasingCompanyPage, validData).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers), affinityGroup = agentAffinity)
        .overrides(bind[Navigator].qualifiedWith("agentsSh03").toInstance(getNavigator))
        .build()

      running(application) {
        val request = FakeRequest(GET, roleAtPurchasingCompanyRoute)

        val view = application.injector.instanceOf[RoleAtPurchasingCompanyView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(validData), NormalMode, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered (UK Societas)" in {

      val userAnswers = emptyUserAnswers.set(RoleAtPurchasingCompanyPage, validUkSocietasData).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers), affinityGroup = agentAffinity)
        .overrides(bind[Navigator].qualifiedWith("agentsSh03").toInstance(getNavigator))
        .build()

      running(application) {
        val request = FakeRequest(GET, roleAtPurchasingCompanyRoute)

        val view = application.injector.instanceOf[RoleAtPurchasingCompanyView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(validUkSocietasData), NormalMode, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid standard data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = agentAffinity)
        .overrides(bind[Navigator].qualifiedWith("agentsSh03").toInstance(getNavigator))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, roleAtPurchasingCompanyRoute)
            .withFormUrlEncodedBody(("role", "director"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual testNextPage.url
      }
    }

    "must redirect to the next page when valid UK Societas data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = agentAffinity)
        .overrides(bind[Navigator].qualifiedWith("agentsSh03").toInstance(getNavigator))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, roleAtPurchasingCompanyRoute)
            .withFormUrlEncodedBody(("role", "ukSocietas"), ("uksOrgan", "Management Board"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual testNextPage.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted (missing role)" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = agentAffinity)
        .overrides(bind[Navigator].qualifiedWith("agentsSh03").toInstance(getNavigator))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, roleAtPurchasingCompanyRoute)
            .withFormUrlEncodedBody(("role", ""))

        val boundForm = form.bind(Map("role" -> ""))

        val view = application.injector.instanceOf[RoleAtPurchasingCompanyView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must return a Bad Request and errors when invalid data is submitted (UK Societas missing organ name)" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = agentAffinity)
        .overrides(bind[Navigator].qualifiedWith("agentsSh03").toInstance(getNavigator))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, roleAtPurchasingCompanyRoute)
            .withFormUrlEncodedBody(("role", "ukSocietas"), ("uksOrgan", ""))

        val boundForm = form.bind(Map("role" -> "ukSocietas", "uksOrgan" -> ""))

        val view = application.injector.instanceOf[RoleAtPurchasingCompanyView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None, affinityGroup = agentAffinity).build()

      running(application) {
        val request = FakeRequest(GET, roleAtPurchasingCompanyRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None, affinityGroup = agentAffinity).build()

      running(application) {
        val request =
          FakeRequest(POST, roleAtPurchasingCompanyRoute)
            .withFormUrlEncodedBody(("role", "director"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}