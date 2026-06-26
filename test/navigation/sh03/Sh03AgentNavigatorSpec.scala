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
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.routes as sh03AgentRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.single.routes as sh03AgentSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.HowToNotifyAboutShareBuyback
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.agents.CompanyDetails
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.ReasonForPurchase
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.sh03.agents.Sh03AgentNavigator
import uk.gov.hmrc.securitiestransferchargefrontend.models.shared.AgentReference
import uk.gov.hmrc.securitiestransferchargefrontend.pages.Page
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.agents.{AgentReferencePage, CompanyDetailsPage}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.shared.HowToNotifyAboutShareBuybackPage
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.single.{ApplyingForReliefPage, DetailsOfThisSharePurchasePage, ReasonForPurchasePage, TreasurySharesPage}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.shared.ConnectedPersonsPage

class Sh03AgentNavigatorSpec extends SpecBase with ScalaFutures {

  val navigator = new Sh03AgentNavigator(StubAnswerPersistenceService())

  "Sh03AgentNavigator" - {

    "in Normal mode" - {

      "must go from a page that doesn't exist in the route map to default page" in {
        case object UnknownPage extends Page
        val result = navigator.nextPage(UnknownPage, NormalMode, UserAnswers(testUserId, testGroupIdentifier, submissionId))(fakeRequest)
        whenReady(result) { res =>
          res mustBe navigator.defaultPage
        }
      }

      "must go from any page to the dashboard page if isReturn is true" in {
        case object AnyPage extends Page
        val result = navigator.nextPage(AnyPage, NormalMode, UserAnswers(testUserId, testGroupIdentifier, submissionId), true)(fakeRequest)
        whenReady(result) { res =>
          res mustBe navigator.dashboardPage
        }
      }

      "must go from the HowToNotifyAboutShareBuyback to AgentReferenceController when one at a time is selected" in {
        val answers = emptyUserAnswers.set(HowToNotifyAboutShareBuybackPage, HowToNotifyAboutShareBuyback.OneAtATime).get
        val result = navigator.nextPage(HowToNotifyAboutShareBuybackPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03AgentSingleRoutes.AgentReferenceController.onPageLoad(NormalMode)
        }
      }

      "must go from the HowToNotifyAboutShareBuyback to AgentReferenceController when more than one at a time is selected" ignore {
        //TODO AT BULK JOURNEY
      }

      "must go from AgentReference to CompanyDetailsControllerPage" in {
        val answers = emptyUserAnswers.set(HowToNotifyAboutShareBuybackPage, HowToNotifyAboutShareBuyback.OneAtATime).get.set(AgentReferencePage, AgentReference(Some("HMRC"))).get
        val result = navigator.nextPage(AgentReferencePage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03AgentSingleRoutes.CompanyDetailsController.onPageLoad(NormalMode)
        }
      }

      "must go from CompanyDetails to ReasonForPurchasePage" in {
        val answers = emptyUserAnswers.set(CompanyDetailsPage, CompanyDetails("Business 1", "12345678", true)).get
        val result = navigator.nextPage(CompanyDetailsPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03AgentSingleRoutes.ReasonForPurchaseController.onPageLoad(NormalMode)
        }
      }

      "must go from ReasonForPurchase to TreasuryShares when 'For Cancellation' is selected" in {
        val answers = emptyUserAnswers.set(ReasonForPurchasePage, ReasonForPurchase.ForCancellation).get
        val result = navigator.nextPage(ReasonForPurchasePage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03AgentSingleRoutes.TreasurySharesController.onPageLoad(NormalMode)
        }
      }

      "must go from ReasonForPurchase to ConnectedPersonsPage when 'To Place Into Treasury' is selected" in {
        val answers = emptyUserAnswers.set(ReasonForPurchasePage, ReasonForPurchase.ToPlaceIntoTreasury).get
        val result = navigator.nextPage(ReasonForPurchasePage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03AgentSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode)
        }
      }

      "must go from TreasuryShares to ConnectedPersonsPage" in {
        val answers = emptyUserAnswers.set(TreasurySharesPage, true).get
        val result = navigator.nextPage(TreasurySharesPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03AgentSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode)
        }
      }

      "must go from the ConnectedPersonsPage to ApplyingForReliefPage" in {
        val answers = emptyUserAnswers.set(ConnectedPersonsPage, true).get
        val result = navigator.nextPage(ConnectedPersonsPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03AgentSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
        }
      }

      "must go from ApplyingForRelief to DetailsOfThisSharePurchasePage" ignore{
        val answers = emptyUserAnswers.set(ApplyingForReliefPage, false).get
        val result = navigator.nextPage(ApplyingForReliefPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03AgentSingleRoutes.DetailsOfThisSharePurchaseController.onPageLoad(NormalMode)
        }
      }
    }
  }

  "in Check mode" - {

    "must go from a page that doesn't exist in the edit route map to CheckYourAnswers" in {

      case object UnknownPage extends Page
      val result = navigator.nextPage(UnknownPage, CheckMode, UserAnswers(testUserId, testGroupIdentifier, submissionId))(fakeRequest)
      whenReady(result) { res =>
        res mustBe routes.CheckYourAnswersController.onPageLoad()
      }
    }
  }

  "in Previous Pages" - {

    "must go from a page that doesn't exist in the previous route map to Journey Recovery" in {
      case object UnknownPage extends Page
      val result = navigator.previousPage(UnknownPage, NormalMode, emptyUserAnswers)
      result mustBe navigator.defaultPage
    }

    "must go from the AgentReferencePage to HowToNotifyAboutShareBuybackPage if it is in single journey" in {
      val answers = emptyUserAnswers.set(HowToNotifyAboutShareBuybackPage, HowToNotifyAboutShareBuyback.OneAtATime).get
      val result = navigator.previousPage(AgentReferencePage, NormalMode, answers)
      result mustBe sh03AgentRoutes.HowToNotifyAboutShareBuybackController.onPageLoad()
    }

    "must go from the CompanyDetailsPage to AgentReferencePage if it is in single journey" in {
      val answers = emptyUserAnswers.set(AgentReferencePage, AgentReference(Some("HMRC"))).get
      val result = navigator.previousPage(CompanyDetailsPage, NormalMode, answers)
      result mustBe sh03AgentSingleRoutes.AgentReferenceController.onPageLoad(NormalMode)
    }

    "must go from the ReasonForPurchasePage to CompanyDetailsPage if it is in single journey" in {
      val answers = emptyUserAnswers.set(CompanyDetailsPage, CompanyDetails("Business 1", "12345678", true)).get
      val result = navigator.previousPage(ReasonForPurchasePage, NormalMode, answers)
      result mustBe sh03AgentSingleRoutes.CompanyDetailsController.onPageLoad(NormalMode)
    }

    "must go from the TreasurySharesPage to ReasonForPurchasePage if it is in single journey" in {
      val answers = emptyUserAnswers.set(ReasonForPurchasePage, ReasonForPurchase.ForCancellation).get
      val result = navigator.previousPage(TreasurySharesPage, NormalMode, answers)
      result mustBe sh03AgentSingleRoutes.ReasonForPurchaseController.onPageLoad(NormalMode)
    }

    "must go from the ConnectedPersonsPage to TreasurySharesPage when reason for purchase is FOR CANCELLATION" in {
      val answers = emptyUserAnswers.set(ReasonForPurchasePage, ReasonForPurchase.ForCancellation).get
      val result = navigator.previousPage(ConnectedPersonsPage, NormalMode, answers)
      result mustBe sh03AgentSingleRoutes.TreasurySharesController.onPageLoad(NormalMode)
    }

    "must go from the ConnectedPersonsPage to ReasonForPurchasePage when reason for purchase is TO PLACE INTO TREASURY" in {
      val answers = emptyUserAnswers.set(ReasonForPurchasePage, ReasonForPurchase.ToPlaceIntoTreasury).get
      val result = navigator.previousPage(ConnectedPersonsPage, NormalMode, answers)
      result mustBe sh03AgentSingleRoutes.ReasonForPurchaseController.onPageLoad(NormalMode)
    }

    "must go from the ApplyingForReliefPage to ConnectedPersonsPage if it is in single journey" in {
      val answers = emptyUserAnswers.set(ConnectedPersonsPage, true).get
      val result = navigator.previousPage(ApplyingForReliefPage, NormalMode, answers)
      result mustBe sh03AgentSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode)
    }

    "must go from the DetailsOfThisSharePurchasePage to ApplyingForReliefPage if it is in single journey" in {
      val answers = emptyUserAnswers.set(ApplyingForReliefPage, false).get
      val result = navigator.previousPage(DetailsOfThisSharePurchasePage, NormalMode, answers)
      result mustBe sh03AgentSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
    }
  }
}
