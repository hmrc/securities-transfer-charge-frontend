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

package controllers.stf.individuals

import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SaveAndReturnClient
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.routes as individualRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.individuals.HowToNotifyAboutSecuritiesTransferFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.{HowToNotifyAboutSecuritiesTransfer, NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.HowToNotifyAboutSecuritiesTransferPage
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.individuals.HowToNotifyAboutSecuritiesTransferView

import scala.concurrent.Future

class HowToNotifyAboutSecuritiesTransferControllerSpec extends SpecBase {

  lazy val howToNotifyAboutSecuritiesTransferRoute: String = individualRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad(NormalMode).url

  val formProvider = new HowToNotifyAboutSecuritiesTransferFormProvider()
  val form: Form[HowToNotifyAboutSecuritiesTransfer] = formProvider()

  "HowToNotifyAboutSecuritiesTransfer Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[Navigator].qualifiedWith("individuals").toInstance(getNavigator))
        .build()

      running(application) {
        val request = FakeRequest(GET, howToNotifyAboutSecuritiesTransferRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[HowToNotifyAboutSecuritiesTransferView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, affinityGroupKeyInd, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId, submissionId).set(HowToNotifyAboutSecuritiesTransferPage, HowToNotifyAboutSecuritiesTransfer.values.head).success.value
      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[Navigator].qualifiedWith("individuals").toInstance(getNavigator))
        .build()

      running(application) {
        val request = FakeRequest(GET, howToNotifyAboutSecuritiesTransferRoute)

        val view = application.injector.instanceOf[HowToNotifyAboutSecuritiesTransferView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(HowToNotifyAboutSecuritiesTransfer.values.head), NormalMode, affinityGroupKeyInd, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[Navigator].qualifiedWith("individuals").toInstance(getNavigator))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, howToNotifyAboutSecuritiesTransferRoute).withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[HowToNotifyAboutSecuritiesTransferView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, affinityGroupKeyInd, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must redirect to confirm your address page when one at a time is selected" in {
      val saveAndReturnClient = mock[SaveAndReturnClient]

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), saveAndReturnClient = saveAndReturnClient)
        .build()

      when(saveAndReturnClient.save(any[UserAnswers]())(any[HeaderCarrier]()))
        .thenReturn(Future.successful(()))

      running(application) {
        val request =
          FakeRequest(POST, howToNotifyAboutSecuritiesTransferRoute)
            .withFormUrlEncodedBody("value" -> HowToNotifyAboutSecuritiesTransfer.values.head.toString)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual individualRoutes.ConfirmAddressController.onPageLoad().url
      }
    }
  }
}
