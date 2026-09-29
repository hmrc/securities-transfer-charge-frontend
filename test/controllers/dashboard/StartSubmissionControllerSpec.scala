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

package controllers.dashboard

import base.SpecBase
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.dashboard.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.shared.routes as sh03Routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.agents.routes as stfAgentRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.routes as stfIndividualRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations.routes as stfOrganisationRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes as frontendRoutes

class StartSubmissionControllerSpec extends SpecBase {

  lazy val startStfRoute: String = routes.StartSubmissionController.startStf().url

  lazy val startSh03Route: String = routes.StartSubmissionController.startSh03().url

  "StartSubmissionController" - {

    "startStf" - {

      "must redirect an Individual to the individual STF journey" in {

        val application = applicationBuilder(affinityGroup = AffinityGroup.Individual).build()

        running(application) {

          val request =
            FakeRequest(GET, startStfRoute)

          val result =
            route(application, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result) mustBe Some(stfIndividualRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad().url)
        }
      }

      "must redirect an Agent to the agent STF journey" in {

        val application = applicationBuilder(affinityGroup = AffinityGroup.Agent).build()

        running(application) {

          val request = FakeRequest(GET, startStfRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result) mustBe Some(stfAgentRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad().url)
        }
      }

      "must redirect an Organisation to the organisation STF journey" in {

        val application = applicationBuilder(affinityGroup = AffinityGroup.Organisation).build()

        running(application) {

          val request = FakeRequest(GET, startStfRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result) mustBe Some(stfOrganisationRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad().url)
        }
      }
    }

    "startSh03" - {

      "must redirect an Agent to the SH03 journey" in {

        val application = applicationBuilder(affinityGroup = AffinityGroup.Agent).build()

        running(application) {

          val request = FakeRequest(GET, startSh03Route)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result) mustBe Some(sh03Routes.BeforeYouStartController.onPageLoad().url)
        }
      }

      "must redirect an Organisation to the SH03 journey" in {

        val application = applicationBuilder(affinityGroup = AffinityGroup.Organisation).build()

        running(application) {

          val request = FakeRequest(GET, startSh03Route)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result) mustBe Some(sh03Routes.BeforeYouStartController.onPageLoad().url)
        }
      }

      "must redirect an Individual to Journey Recovery" in {

        val application = applicationBuilder(affinityGroup = AffinityGroup.Individual).build()

        running(application) {

          val request = FakeRequest(GET, startSh03Route)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result) mustBe Some(frontendRoutes.JourneyRecoveryController.onPageLoad().url)
        }
      }
    }
  }
}