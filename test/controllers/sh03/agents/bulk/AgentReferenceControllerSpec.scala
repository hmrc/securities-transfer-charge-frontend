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

package controllers.sh03.agents.bulk

import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SubmissionIdClient
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.bulk.routes as agentsSh03BulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.forms.shared.AgentReferenceFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.models.shared.AgentReference
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.bulk.BulkAgentReferencePage
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.agents.bulk.AgentReferenceView

import scala.concurrent.Future
import scala.util.Random

class AgentReferenceControllerSpec extends SpecBase {

  val formProvider = new AgentReferenceFormProvider()
  val form: Form[AgentReference] = formProvider()

  lazy val agentReferenceRoute: String = agentsSh03BulkRoutes.AgentReferenceController.onPageLoad(NormalMode).url

  "AgentReferenceController" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = agentAffinity)
        .overrides(
          bind[Navigator].qualifiedWith("agentsSh03").toInstance(getNavigator))
        .build()

      running(application) {
        val request = FakeRequest(GET, agentReferenceRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AgentReferenceView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must return OK and a pre-populated form when an answer already exists" in {

      val userAnswers = emptyUserAnswers
        .set(BulkAgentReferencePage, AgentReference(Some("answer")))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers), affinityGroup = agentAffinity)
        .overrides(
          bind[Navigator].qualifiedWith("agentsSh03").toInstance(getNavigator)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, agentReferenceRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AgentReferenceView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(AgentReference(Some("answer"))), NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val mockIdClient = mock[SubmissionIdClient]

      when(mockIdClient.nextSubmissionId()(any()))
        .thenReturn(Future.successful(submissionId))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers),agentAffinity)
          .overrides(bind[Navigator].qualifiedWith("agentsSh03").toInstance(getNavigator),bind[SubmissionIdClient].toInstance(mockIdClient))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, agentReferenceRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual testNextPage.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = agentAffinity)
        .overrides(bind[Navigator].qualifiedWith("agentsSh03").toInstance(getNavigator))
        .build()

      running(application) {

        val invalidValue = Random.alphanumeric.take(260).mkString

        val request =
          FakeRequest(POST, agentReferenceRoute)
            .withFormUrlEncodedBody(("value", invalidValue))

        val boundForm = form.bind(Map("value" -> invalidValue))

        val view = application.injector.instanceOf[AgentReferenceView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }
  }
}