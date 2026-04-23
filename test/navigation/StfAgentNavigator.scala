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
import base.stubs.StubAnswerPersistenceService
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.stf.organisations.StfOrgNavigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.Page

import java.time.LocalDate

class StfAgentNavigator extends SpecBase with ScalaFutures {

  private val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]
  when(mockConfig.firstChargingPoint).thenReturn(LocalDate.of(2026, 1, 1))

  val navigator = new StfOrgNavigator(mockConfig, StubAnswerPersistenceService())

  "in Normal mode" - {

    "must go from a page that doesn't exist in the route map to default page" in {
      case object UnknownPage extends Page
      val result = navigator.nextPage(UnknownPage, NormalMode, UserAnswers("id", submissionId))(fakeRequest)
      whenReady(result) { res =>
        res mustBe navigator.defaultPage
      }
    }

    "must go from any page to the dashboard page if isReturn is true" in {
      case object AnyPage extends Page
      val result = navigator.nextPage(AnyPage, NormalMode, UserAnswers("id", submissionId), true)(fakeRequest)
      whenReady(result) { res =>
        res mustBe navigator.dashboardPage
      }
    }

    "Previous Pages" - {

      "must go from a page that doesn't exist in the previous route map to Journey Recovery" in {
        case object UnknownPage extends Page
        val result = navigator.previousPage(UnknownPage, NormalMode, emptyUserAnswers)
        result mustBe navigator.defaultPage
      }
      
    }
  }
}