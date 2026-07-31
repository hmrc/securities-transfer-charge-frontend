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
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.organisations.single.routes as sh03OrgSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.shared.CompanyDetailsFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.CompanyDetails
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.CompanyDetailsPage
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.organisations.single.CompanyDetailsView

class CompanyDetailsControllerSpec extends SpecBase with MockitoSugar {

  val formProvider = new CompanyDetailsFormProvider()
  val form: Form[CompanyDetails] = formProvider(affinityGroupKeyOrg)

  lazy val companyDetailsRoute: String = sh03OrgSingleRoutes.CompanyDetailsController.onPageLoad(NormalMode).url

  val validAnswer = CompanyDetails("Test Company Ltd", "AB123456", true)

  "CompanyDetailsController" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = orgAffinity)
        .overrides(bind[Navigator].qualifiedWith("orgSh03").toInstance(getNavigator))
        .build()

      running(application) {
        val request = FakeRequest(GET, companyDetailsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CompanyDetailsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(testUserId, testGroupIdentifier, submissionId)
        .set(CompanyDetailsPage, validAnswer).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers), affinityGroup = orgAffinity)
        .overrides(bind[Navigator].qualifiedWith("orgSh03").toInstance(getNavigator))
        .build()

      running(application) {
        val request = FakeRequest(GET, companyDetailsRoute)

        val view = application.injector.instanceOf[CompanyDetailsView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(validAnswer), NormalMode, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must redirect to next page when valid data is submitted" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = orgAffinity)
        .overrides(bind[Navigator].qualifiedWith("orgSh03").toInstance(getNavigator))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, companyDetailsRoute)
            .withFormUrlEncodedBody(
              ("companyName", "Test Company Ltd"),
              ("companyRegistrationNumber", "AB123456"),
              ("isPlc", "true")
            )

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
          FakeRequest(POST, companyDetailsRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[CompanyDetailsView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, testBackLinkRoute)(request, messages(application)).toString
      }
    }

    "must return a Bad Request when company name is empty" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = orgAffinity)
        .overrides(bind[Navigator].qualifiedWith("orgSh03").toInstance(getNavigator))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, companyDetailsRoute)
            .withFormUrlEncodedBody(
              ("companyName", ""),
              ("companyRegistrationNumber", "AB123456"),
              ("isPlc", "true")
            )

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

    "must return a Bad Request when CRN is not 8 characters" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = orgAffinity)
        .overrides(bind[Navigator].qualifiedWith("orgSh03").toInstance(getNavigator))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, companyDetailsRoute)
            .withFormUrlEncodedBody(
              ("companyName", "Test Company"),
              ("companyRegistrationNumber", "ABC123"),
              ("isPlc", "true")
            )

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

    "must return a Bad Request when CRN contains invalid characters" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = orgAffinity)
        .overrides(bind[Navigator].qualifiedWith("orgSh03").toInstance(getNavigator))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, companyDetailsRoute)
            .withFormUrlEncodedBody(
              ("companyName", "Test Company"),
              ("companyRegistrationNumber", "AB12-456"),
              ("isPlc", "true")
            )

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None, affinityGroup = orgAffinity).build()

      running(application) {
        val request = FakeRequest(GET, companyDetailsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None, affinityGroup = orgAffinity).build()

      running(application) {
        val request =
          FakeRequest(POST, companyDetailsRoute)
            .withFormUrlEncodedBody(
              ("companyName", "Test Company Ltd"),
              ("companyRegistrationNumber", "AB123456"),
              ("isPlc", "true")
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
