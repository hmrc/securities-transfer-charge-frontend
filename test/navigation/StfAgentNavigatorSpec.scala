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

import base.stubs.StubAnswerPersistenceService
import base.{Fixtures, SpecBase}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.agents.routes as agentRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.agents.single.routes as agentSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes as sharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.{AgentReference, HowToNotifyAboutSecuritiesTransfer}
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.stf.agents.StfAgentNavigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.Page
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.HowToNotifyAboutSecuritiesTransferPage
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.*

class StfAgentNavigatorSpec extends SpecBase with ScalaFutures {

  private val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]
  val navigator = new StfAgentNavigator(mockConfig, StubAnswerPersistenceService())

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

    "must go from the HowToNotifyAboutSecuritiesTransfer to AgentReferencePage when one at a time is selected" in {
      val answers = emptyUserAnswers.set(HowToNotifyAboutSecuritiesTransferPage, HowToNotifyAboutSecuritiesTransfer.OneAtATime).get
      val result = navigator.nextPage(HowToNotifyAboutSecuritiesTransferPage, NormalMode, answers)(fakeRequest)
      whenReady(result) { res =>
        res mustBe agentSingleRoutes.AgentReferenceController.onPageLoad(NormalMode)
      }
    }

    "must go from the AgentReferencePage to NameOfBuyerPage when one at a time is selected" in {
      val answers = emptyUserAnswers.set(AgentReferencePage, AgentReference(Some("HMRC"))).get
      val result = navigator.nextPage(AgentReferencePage, NormalMode, answers)(fakeRequest)
      whenReady(result) { res =>
        res mustBe agentSingleRoutes.NameOfBuyerController.onPageLoad(NormalMode)
      }
    }

    "must go from the NameOfBuyerPage to StfBuyersAddressPage" in {
      val answers = emptyUserAnswers.set(NameOfBuyerPage, "John").get
      val result = navigator.nextPage(NameOfBuyerPage, NormalMode, answers)(fakeRequest)
      whenReady(result) { res =>
        res mustBe agentSingleRoutes.AddressController.onPageLoad()
      }
    }

    "must go from the StfBuyersAddressPage to NameOfSellerPage" in {
      val answers = emptyUserAnswers.set(StfBuyersAddressPage, Fixtures.fakeAlfConfirmedAddress).get
      val result = navigator.nextPage(StfBuyersAddressPage, NormalMode, answers)(fakeRequest)
      whenReady(result) { res =>
        res mustBe agentSingleRoutes.NameOfSellerController.onPageLoad(NormalMode)
      }
    }

    "must go from the NameOfSellerPage to StfSellerAddressPage" in {
      val answers = emptyUserAnswers.set(NameOfSellerPage, "John").get
      val result = navigator.nextPage(NameOfSellerPage, NormalMode, answers)(fakeRequest)
      whenReady(result) { res =>
        res mustBe agentSingleRoutes.StfSellerAddressController.onPageLoad()
      }
    }

    "must go from the StfSellerAddressPage to ConnectedPersonsPage" in {
      val answers = emptyUserAnswers.set(StfSellerAddressPage, Fixtures.fakeAlfConfirmedAddress).get
      val result = navigator.nextPage(StfSellerAddressPage, NormalMode, answers)(fakeRequest)
      whenReady(result) { res =>
        res mustBe agentSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode)
      }
    }

    "Previous Pages" - {

      "must go from a page that doesn't exist in the previous route map to Journey Recovery" in {
        case object UnknownPage extends Page
        val result = navigator.previousPage(UnknownPage, NormalMode, emptyUserAnswers)
        result mustBe navigator.defaultPage
      }

      "must go from the ConnectedPersonsPage to StfSellerAddressPage" in {
        val result = navigator.previousPage(ConnectedPersonsPage, NormalMode, emptyUserAnswers)
        result mustBe agentSingleRoutes.StfSellerAddressController.onPageLoad()
      }

      "must go from the StfSellerAddressPage to NameOfSellerPage" in {
        val result = navigator.previousPage(StfSellerAddressPage, NormalMode, emptyUserAnswers)
        result mustBe agentSingleRoutes.NameOfSellerController.onPageLoad(NormalMode)
      }

      "must go from the NameOfSellerPage to StfBuyersAddressPage" in {
        val result = navigator.previousPage(NameOfSellerPage, NormalMode, emptyUserAnswers)
        result mustBe agentSingleRoutes.AddressController.onPageLoad()
      }

      "must go from the StfBuyersAddressPage to NameOfBuyerPage" in {
        val result = navigator.previousPage(StfBuyersAddressPage, NormalMode, emptyUserAnswers)
        result mustBe agentSingleRoutes.NameOfBuyerController.onPageLoad(NormalMode)
      }

      "must go from the NameOfBuyerPage to AgentReferencePage" in {
        val result = navigator.previousPage(NameOfBuyerPage, NormalMode, emptyUserAnswers)
        result mustBe agentSingleRoutes.AgentReferenceController.onPageLoad(NormalMode)
      }

      "must go from the AgentReferencePage to HowToNotifyAboutSecuritiesTransferPage" in {
        val result = navigator.previousPage(AgentReferencePage, NormalMode, emptyUserAnswers)
        result mustBe agentRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad(NormalMode)
      }

      "must go from the HowToNotifyAboutSecuritiesTransferPage to SubmissionsDashboard" in {
        val result = navigator.previousPage(HowToNotifyAboutSecuritiesTransferPage, NormalMode, emptyUserAnswers)
        result mustBe sharedRoutes.SubmissionsDashboardController.onPageLoad()
      }
      
    }
  }
}