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

package controllers.sh03.organisations.single

import base.SpecBase
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.organisations.single.routes as sh03Routes
import uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.shared.MinimumAmountPaidFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.MinimumAmountPaidPage
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.organisations.single.MinimumAmountPaidView

class MinimumAmountPaidControllerSpec extends SpecBase with MockitoSugar {

  val formProvider = new MinimumAmountPaidFormProvider()
  val form: Form[BigDecimal] = formProvider(affinityKey=affinityGroupKeyOrg)

  val validAnswer = 100

  lazy val minimumAmountPaidRoute: String = sh03Routes.MinimumAmountPaidController.onPageLoad(NormalMode).url

  "MinimumAmountPaidController" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = orgAffinity)
        .overrides(bind[Navigator].qualifiedWith("orgSh03").toInstance(getNavigator))
        .build()

      running(application) {
        val request = FakeRequest(GET, minimumAmountPaidRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[MinimumAmountPaidView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(testUserId, testGroupIdentifier, submissionId).set(MinimumAmountPaidPage, validAnswer).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers), affinityGroup = orgAffinity)
        .overrides(bind[Navigator].qualifiedWith("orgSh03").toInstance(getNavigator))
        .build()

      running(application) {
        val request = FakeRequest(GET, minimumAmountPaidRoute)

        val view = application.injector.instanceOf[MinimumAmountPaidView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(validAnswer), NormalMode, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {


      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = orgAffinity)
          .overrides(bind[Navigator].qualifiedWith("orgSh03").toInstance(getNavigator))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, minimumAmountPaidRoute)
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual testNextPageCall.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = orgAffinity)
        .overrides(bind[Navigator].qualifiedWith("orgSh03").toInstance(getNavigator))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, minimumAmountPaidRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[MinimumAmountPaidView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None, affinityGroup = orgAffinity).build()

      running(application) {
        val request = FakeRequest(GET, minimumAmountPaidRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None, affinityGroup = orgAffinity).build()

      running(application) {
        val request =
          FakeRequest(POST, minimumAmountPaidRoute)
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
