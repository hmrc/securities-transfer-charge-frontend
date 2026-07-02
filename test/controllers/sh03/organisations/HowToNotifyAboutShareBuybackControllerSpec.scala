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

package controllers.sh03.organisations

import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SubmissionIdClient
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.organisations.routes as orgRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.shared.HowToNotifyAboutShareBuybackFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.HowToNotifyAboutShareBuyback
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.HowToNotifyAboutShareBuybackPage
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.organisations.HowToNotifyAboutShareBuybackView

import scala.concurrent.Future
import scala.language.postfixOps

class HowToNotifyAboutShareBuybackControllerSpec extends SpecBase with MockitoSugar {

  lazy val howToNotifyAboutShareBuybackRoute: String = orgRoutes.HowToNotifyAboutShareBuybackController.onPageLoad().url

  val formProvider = new HowToNotifyAboutShareBuybackFormProvider()
  val form: Form[HowToNotifyAboutShareBuyback] = formProvider(affinityGroupKeyOrg)

  "HowToNotifyAboutShareBuybackController" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = orgAffinity)
        .overrides(bind[Navigator].qualifiedWith("orgSh03").toInstance(getNavigator))
        .build()

      running(application) {
        val request = FakeRequest(GET, howToNotifyAboutShareBuybackRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[HowToNotifyAboutShareBuybackView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, affinityGroupKeyOrg)(request, messages(application)).toString
      }
    }

    "must redirect to the appropriate next page when One at a time is submitted" in {

      val mockIdClient = mock[SubmissionIdClient]

      when(mockIdClient.nextSubmissionId()(any()))
        .thenReturn(Future.successful(submissionId))

      val userAnswers = UserAnswers(testUserId, testGroupIdentifier, submissionId)
        .set(HowToNotifyAboutShareBuybackPage, HowToNotifyAboutShareBuyback.values.head).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers), affinityGroup = orgAffinity)
        .overrides(
          bind[Navigator].qualifiedWith("orgSh03").toInstance(getNavigator),
          bind[SubmissionIdClient].toInstance(mockIdClient)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, howToNotifyAboutShareBuybackRoute)
            .withFormUrlEncodedBody("value" -> HowToNotifyAboutShareBuyback.values.head.toString)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual testNextPage.url
      }
    }

    "must redirect to the appropriate next page when More than one at a time is selected" in {

      val mockIdClient = mock[SubmissionIdClient]

      when(mockIdClient.nextSubmissionId()(any()))
        .thenReturn(Future.successful(submissionId))

      val userAnswers = UserAnswers(testUserId, testGroupIdentifier, submissionId)
        .set(HowToNotifyAboutShareBuybackPage, HowToNotifyAboutShareBuyback.values.last).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers), affinityGroup = orgAffinity)
        .overrides(
          bind[Navigator].qualifiedWith("orgSh03").toInstance(getNavigator),
          bind[SubmissionIdClient].toInstance(mockIdClient)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, howToNotifyAboutShareBuybackRoute)
            .withFormUrlEncodedBody("value" -> HowToNotifyAboutShareBuyback.values.last.toString)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual testNextPage.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = orgAffinity)
        .overrides(bind[Navigator].qualifiedWith("orgSh03").toInstance(getNavigator))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, howToNotifyAboutShareBuybackRoute).withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[HowToNotifyAboutShareBuybackView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, affinityGroupKeyOrg)(request, messages(application)).toString
      }
    }
  }
}