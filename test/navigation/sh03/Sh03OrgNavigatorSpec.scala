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

package navigation.sh03

import base.SpecBase
import base.stubs.StubAnswerPersistenceService
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.Call
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.HowToNotifyAboutShareBuyback
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, NormalMode}
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.organisations.single.routes as sh03OrgSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.sh03.organisations.Sh03OrgNavigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.Page
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.*

import java.time.LocalDate

class Sh03OrgNavigatorSpec extends SpecBase with ScalaFutures {

  private val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]
  when(mockConfig.firstChargingPoint).thenReturn(LocalDate.of(2026, 1, 1))
  
  val navigator = new Sh03OrgNavigator(StubAnswerPersistenceService(), mockConfig)

  "Sh03OrgNavigator" - {

    "in Normal mode" - {

      "must go from a page that doesn't exist in the route map to default page" in {
        case object UnknownPage extends Page
        val result = navigator.nextPage(UnknownPage, NormalMode, emptyUserAnswers)(fakeRequest)
        whenReady(result) { (res: Call) =>
          res mustEqual navigator.defaultPage
        }
      }

      "must go from any page to the dashboard page if isReturn is true" in {
        case object AnyPage extends Page
        val result = navigator.nextPage(AnyPage, NormalMode, emptyUserAnswers, true)(fakeRequest)
        whenReady(result) { (res: Call) =>
          res mustEqual navigator.dashboardPage
        }
      }

      "must go from HowToNotifyAboutShareBuyback to CompanyDetailsController when 'One at a time' is selected" in {
        val answers = emptyUserAnswers.set(HowToNotifyAboutShareBuybackPage, HowToNotifyAboutShareBuyback.OneAtATime).get
        val result = navigator.nextPage(HowToNotifyAboutShareBuybackPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03OrgSingleRoutes.CompanyDetailsController.onPageLoad(NormalMode)
        }
      }

      "must go from HowToNotifyAboutShareBuyback to default page (placeholder) when 'More than one at a time' is selected" in {
        val answers = emptyUserAnswers.set(HowToNotifyAboutShareBuybackPage, HowToNotifyAboutShareBuyback.MoreThanOneAtATime).success.value
        val result = navigator.nextPage(HowToNotifyAboutShareBuybackPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { (res: Call) =>
          res mustEqual navigator.defaultPage
        }
      }
    }

    "in Check mode" - {

      "must go from a page that doesn't exist in the edit route map to CheckYourAnswers" in {
        case object UnknownPage extends Page
        val result = navigator.nextPage(UnknownPage, CheckMode, emptyUserAnswers)(fakeRequest)
        whenReady(result) { (res: Call) =>
          res mustEqual routes.CheckYourAnswersController.onPageLoad()
        }
      }
    }

    "in Previous Pages" - {

      "must go from a page that doesn't exist in the previous route map to default page (Journey Recovery)" in {
        case object UnknownPage extends Page
        val result = navigator.previousPage(UnknownPage, NormalMode, emptyUserAnswers)
        result mustEqual navigator.defaultPage
      }

      "must go from the HowToNotifyAboutShareBuybackPage to default page (placeholder)" in {
        val answers = emptyUserAnswers.set(HowToNotifyAboutShareBuybackPage, HowToNotifyAboutShareBuyback.OneAtATime).success.value
        val result = navigator.previousPage(HowToNotifyAboutShareBuybackPage, NormalMode, answers)
        result mustEqual navigator.defaultPage
      }
    }
  }
}