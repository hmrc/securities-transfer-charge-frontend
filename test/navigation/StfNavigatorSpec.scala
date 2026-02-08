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

package navigation

import base.SpecBase
import base.stubs.StubSessionRepository
import clients.FakeSaveAndReturnClient
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.*
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.StfNavigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.Page

class StfNavigatorSpec extends SpecBase with ScalaFutures {

  val navigator = new StfNavigator(StubSessionRepository(), FakeSaveAndReturnClient())

  "Navigator" - {

    "in Normal mode" - {

      "must go from a page that doesn't exist in the route map to Journey Recovery" in {

        case object UnknownPage extends Page
        val result = navigator.nextPage(UnknownPage, NormalMode, UserAnswers("id", submissionId))
        whenReady(result) { res =>
          res mustBe routes.JourneyRecoveryController.onPageLoad()
        }

      }
    }

    "in Check mode" - {

      "must go from a page that doesn't exist in the edit route map to CheckYourAnswers" in {

        case object UnknownPage extends Page
        val result = navigator.nextPage(UnknownPage, CheckMode, UserAnswers("id", submissionId))
        whenReady(result) { res =>
          res mustBe routes.CheckYourAnswersController.onPageLoad()
        }
      }
    }
  }
}
