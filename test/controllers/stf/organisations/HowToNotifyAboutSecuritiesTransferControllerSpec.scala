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
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.organisations.HowToNotifyAboutSecuritiesTransferFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.{HowToNotifyAboutSecuritiesTransfer, NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.HowToNotifyAboutSecuritiesTransferPage
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.organisations.HowToNotifyAboutSecuritiesTransferView
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations.routes as orgRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator

class HowToNotifyAboutSecuritiesTransferControllerSpec extends SpecBase {

  lazy val howToNotifyAboutSecuritiesTransferRoute: String = orgRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad(NormalMode).url

  val formProvider = new HowToNotifyAboutSecuritiesTransferFormProvider()
  val form: Form[HowToNotifyAboutSecuritiesTransfer] = formProvider()

  "HowToNotifyAboutSecuritiesTransfer Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[Navigator].qualifiedWith("organisations").toInstance(getNavigator))
        .build()

      running(application) {
        val request = FakeRequest(GET, howToNotifyAboutSecuritiesTransferRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[HowToNotifyAboutSecuritiesTransferView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId, submissionId).set(HowToNotifyAboutSecuritiesTransferPage, HowToNotifyAboutSecuritiesTransfer.values.head).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[Navigator].qualifiedWith("organisations").toInstance(getNavigator))
        .build()

      running(application) {
        val request = FakeRequest(GET, howToNotifyAboutSecuritiesTransferRoute)

        val view = application.injector.instanceOf[HowToNotifyAboutSecuritiesTransferView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(HowToNotifyAboutSecuritiesTransfer.values.head), NormalMode, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[Navigator].qualifiedWith("organisations").toInstance(getNavigator))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, howToNotifyAboutSecuritiesTransferRoute).withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[HowToNotifyAboutSecuritiesTransferView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery when no existing data is found on GET" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, howToNotifyAboutSecuritiesTransferRoute)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect after valid submission" in {

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()

      running(application) {

        val request =
          FakeRequest(POST, howToNotifyAboutSecuritiesTransferRoute)
            .withFormUrlEncodedBody("value" -> HowToNotifyAboutSecuritiesTransfer.OneAtATime.toString)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
      }
    }
  }
}