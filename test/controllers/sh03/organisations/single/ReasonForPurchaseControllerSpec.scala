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
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes.JourneyRecoveryController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.organisations.single.routes
import uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.shared.ReasonForPurchaseFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.ReasonForPurchase
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.ReasonForPurchasePage
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.organisations.single.ReasonForPurchaseView

class ReasonForPurchaseControllerSpec extends SpecBase with MockitoSugar {
  
  lazy val reasonForPurchaseRoute: String = routes.ReasonForPurchaseController.onPageLoad(NormalMode).url

  val formProvider = new ReasonForPurchaseFormProvider()
  val form: Form[ReasonForPurchase] = formProvider()

  "ReasonForPurchase Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = orgAffinity)
        .overrides(bind[Navigator].qualifiedWith("orgSh03").toInstance(getNavigator))
        .build()

      running(application) {
        val request = FakeRequest(GET, reasonForPurchaseRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ReasonForPurchaseView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode,testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(testUserId, testGroupIdentifier, submissionId).set(ReasonForPurchasePage, ReasonForPurchase.values.head).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers), affinityGroup = orgAffinity)
        .overrides(bind[Navigator].qualifiedWith("orgSh03").toInstance(getNavigator))
        .build()

      running(application) {
        val request = FakeRequest(GET, reasonForPurchaseRoute)

        val view = application.injector.instanceOf[ReasonForPurchaseView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(ReasonForPurchase.values.head), NormalMode,testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {


      val updatedAnswers = emptyUserAnswers.set(ReasonForPurchasePage, ReasonForPurchase.ForCancellation).success.value
      val application =
        applicationBuilder(userAnswers = Some(updatedAnswers), affinityGroup = orgAffinity)
          .overrides(bind[Navigator].qualifiedWith("orgSh03").toInstance(getNavigator))
          .build()


      running(application) {
        val request =
          FakeRequest(POST, reasonForPurchaseRoute)
            .withFormUrlEncodedBody(("value", ReasonForPurchase.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual testNextPage.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = orgAffinity)
          .overrides(bind[Navigator].qualifiedWith("orgSh03").toInstance(getNavigator))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, reasonForPurchaseRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[ReasonForPurchaseView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode,testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, reasonForPurchaseRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual JourneyRecoveryController.onPageLoad().url
      }
    }

    "redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, reasonForPurchaseRoute)
            .withFormUrlEncodedBody(("value", ReasonForPurchase.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
