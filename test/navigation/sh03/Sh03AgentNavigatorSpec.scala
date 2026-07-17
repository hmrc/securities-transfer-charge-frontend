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
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.routes as sh03AgentRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.single.routes as sh03AgentSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.HowToNotifyAboutShareBuyback
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.shared.AgentReference
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.sh03.agents.Sh03AgentNavigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.Page
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.*

import java.time.LocalDate

class Sh03AgentNavigatorSpec extends SpecBase with ScalaFutures {

  private val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]
  when(mockConfig.firstChargingPoint).thenReturn(LocalDate.of(2026, 1, 1))


  val navigator = new Sh03AgentNavigator(StubAnswerPersistenceService(), mockConfig)

  private val companyDetails = CompanyDetails(
    companyName = "Company1",
    companyRegistrationNumber = "12345678",
    isPlc = true)

  private val purchaseDetails = DetailsOfThisSharePurchase(
    numberOfShares = 1,
    typeOfShares = "ordinary",
    amountPaid = BigDecimal(250),
    marketValue = Some(BigDecimal(50)))

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

      "must go from the HowToNotifyAboutShareBuybackPage to AgentReferencePage when one at a time is selected" in {
        val answers = emptyUserAnswers.set(HowToNotifyAboutShareBuybackPage, HowToNotifyAboutShareBuyback.OneAtATime).get
        val result = navigator.nextPage(HowToNotifyAboutShareBuybackPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03AgentSingleRoutes.AgentReferenceController.onPageLoad(NormalMode)
        }
      }

      "must go from AgentReferencePage to CompanyDetailsPage" in {
        val answers = emptyUserAnswers.set(HowToNotifyAboutShareBuybackPage, HowToNotifyAboutShareBuyback.OneAtATime).get.set(AgentReferencePage, AgentReference(Some("HMRC"))).get
        val result = navigator.nextPage(AgentReferencePage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03AgentSingleRoutes.CompanyDetailsController.onPageLoad(NormalMode)
        }
      }

      "must go from CompanyDetailsPage to ReasonForPurchasePage" in {
        val answers = emptyUserAnswers.set(CompanyDetailsPage, companyDetails).get
        val result = navigator.nextPage(CompanyDetailsPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03AgentSingleRoutes.ReasonForPurchaseController.onPageLoad(NormalMode)
        }
      }

      "must go from ReasonForPurchasePage to TreasurySharesPage when 'For Cancellation' is selected" in {
        val answers = emptyUserAnswers.set(ReasonForPurchasePage, ReasonForPurchase.ForCancellation).get
        val result = navigator.nextPage(ReasonForPurchasePage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03AgentSingleRoutes.TreasurySharesController.onPageLoad(NormalMode)
        }
      }

      "must go from ReasonForPurchasePage to ConnectedPersonsPage when 'To Place Into Treasury' is selected" in {
        val answers = emptyUserAnswers.set(ReasonForPurchasePage, ReasonForPurchase.ToPlaceIntoTreasury).get
        val result = navigator.nextPage(ReasonForPurchasePage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03AgentSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode)
        }
      }

      "must go from TreasurySharesPage to ConnectedPersonsPage" in {
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

      "must go from ApplyingForReliefPage to WhatReliefAreYouApplyingForPage when applying for a relief" in {
        val answers = emptyUserAnswers.set(ApplyingForReliefPage, true).get
        val result = navigator.nextPage(ApplyingForReliefPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03AgentSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode)
        }
      }

      "must go from ApplyingForReliefPage to DetailsOfThisSharePurchasePage" in {
        val answers = emptyUserAnswers.set(ApplyingForReliefPage, false).get
        val result = navigator.nextPage(ApplyingForReliefPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03AgentSingleRoutes.DetailsOfThisSharePurchaseController.onPageLoad(NormalMode)
        }
      }

      "must go from WhatReliefAreYouApplyingForPage to DetailsOfThisSharePurchasePage" in {
        val answers = emptyUserAnswers.set(WhatReliefAreYouApplyingForPage, "Group Relief").get
        val result = navigator.nextPage(WhatReliefAreYouApplyingForPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03AgentSingleRoutes.DetailsOfThisSharePurchaseController.onPageLoad(NormalMode)
        }
      }

      "must go from DetailsOfThisSharePurchasePage to MaximumAmountPaidPage is company is a PLC" in {
        val answers = emptyUserAnswers
          .set(CompanyDetailsPage, companyDetails)
          .flatMap(_.set(DetailsOfThisSharePurchasePage, purchaseDetails))
          .get
        val result = navigator.nextPage(DetailsOfThisSharePurchasePage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03AgentSingleRoutes.MaximumAmountPaidController.onPageLoad(NormalMode)
        }
      }

      "must go from DetailsOfThisSharePurchasePage to ChargingPointPage is company is not a PLC" in {
        val answers = emptyUserAnswers
          .set(CompanyDetailsPage, companyDetails.copy(isPlc = false))
          .flatMap(_.set(DetailsOfThisSharePurchasePage, purchaseDetails))
          .get
        val result = navigator.nextPage(DetailsOfThisSharePurchasePage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03AgentSingleRoutes.ChargingPointController.onPageLoad(NormalMode)
        }
      }

      "must go from MaximumAmountPaidPage to MinimumAmountPaidPage" in {
        val answers = emptyUserAnswers.set(MaximumAmountPaidPage, BigDecimal(350)).get
        val result = navigator.nextPage(MaximumAmountPaidPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03AgentSingleRoutes.MinimumAmountPaidController.onPageLoad(NormalMode)
        }
      }

      "must go from MinimumAmountPaidPage to ChargingPointPage" in {
        val answers = emptyUserAnswers.set(MinimumAmountPaidPage, BigDecimal(100)).get
        val result = navigator.nextPage(MinimumAmountPaidPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03AgentSingleRoutes.ChargingPointController.onPageLoad(NormalMode)
        }
      }

      "must go from ChargingPointPage to RoleAtPurchasingCompanyPage" in {
        val answers = emptyUserAnswers.set(ChargingPointPage, LocalDate.now()).get
        val result = navigator.nextPage(ChargingPointPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03AgentSingleRoutes.RoleAtPurchasingCompanyController.onPageLoad(NormalMode)
        }
      }

      "must go from RoleAtPurchasingCompanyPage to CheckYourAnswerPage" in {
        val answers = emptyUserAnswers.set(RoleAtPurchasingCompanyPage, RoleAtPurchasingCompany(role = "Director", uksOrgan = None)).get
        val result = navigator.nextPage(RoleAtPurchasingCompanyPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03AgentSingleRoutes.CheckYourAnswersController.onPageLoad()
        }
      }

      "must go from RoleAtPurchasingCompanyPage to CannotSubmitFormErrorPage when a selects None of these (unsupportedRole)" in {
        val answers = emptyUserAnswers.set(RoleAtPurchasingCompanyPage, RoleAtPurchasingCompany(role = "unsupportedRole", uksOrgan = None)).get
        val result = navigator.nextPage(RoleAtPurchasingCompanyPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03AgentSingleRoutes.CannotSubmitFormErrorController.onPageLoad()
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

    "must go from the AgentReferencePage to HowToNotifyAboutShareBuybackPage" in {
      val result = navigator.previousPage(AgentReferencePage, NormalMode, emptyUserAnswers)
      result mustBe sh03AgentRoutes.HowToNotifyAboutShareBuybackController.onPageLoad()
    }

    "must go from the CompanyDetailsPage to AgentReferencePage" in {
      val result = navigator.previousPage(CompanyDetailsPage, NormalMode, emptyUserAnswers)
      result mustBe sh03AgentSingleRoutes.AgentReferenceController.onPageLoad(NormalMode)
    }

    "must go from the ReasonForPurchasePage to CompanyDetailsPage" in {
      val result = navigator.previousPage(ReasonForPurchasePage, NormalMode, emptyUserAnswers)
      result mustBe sh03AgentSingleRoutes.CompanyDetailsController.onPageLoad(NormalMode)
    }

    "must go from the TreasurySharesPage to ReasonForPurchasePage" in {
      val result = navigator.previousPage(TreasurySharesPage, NormalMode, emptyUserAnswers)
      result mustBe sh03AgentSingleRoutes.ReasonForPurchaseController.onPageLoad(NormalMode)
    }

    "must go from the ConnectedPersonsPage to TreasurySharesPage when reason for purchase is For cancellation" in {
      val answers = emptyUserAnswers.set(ReasonForPurchasePage, ReasonForPurchase.ForCancellation).get
      val result = navigator.previousPage(ConnectedPersonsPage, NormalMode, answers)
      result mustBe sh03AgentSingleRoutes.TreasurySharesController.onPageLoad(NormalMode)
    }

    "must go from the ConnectedPersonsPage to ReasonForPurchasePage when reason for purchase is TO place into treasury" in {
      val answers = emptyUserAnswers.set(ReasonForPurchasePage, ReasonForPurchase.ToPlaceIntoTreasury).get
      val result = navigator.previousPage(ConnectedPersonsPage, NormalMode, answers)
      result mustBe sh03AgentSingleRoutes.ReasonForPurchaseController.onPageLoad(NormalMode)
    }

    "must go from the ApplyingForReliefPage to ConnectedPersonsPage" in {
      val result = navigator.previousPage(ApplyingForReliefPage, NormalMode, emptyUserAnswers)
      result mustBe sh03AgentSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode)
    }

    "must go from the WhatReliefAreYouApplyingForPage to ApplyingForReliefPage" in {
      val result = navigator.previousPage(WhatReliefAreYouApplyingForPage, NormalMode, emptyUserAnswers)
      result mustBe sh03AgentSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
    }

    "must go from the DetailsOfThisSharePurchasePage to ApplyingForReliefPage when applying for a relief is false" in {
      val answers = emptyUserAnswers.set(ApplyingForReliefPage, false).get
      val result = navigator.previousPage(DetailsOfThisSharePurchasePage, NormalMode, answers)
      result mustBe sh03AgentSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
    }

    "must go from the DetailsOfThisSharePurchasePage to WhatReliefAreYouApplyingForPage when applying for a relief is true" in {
      val answers = emptyUserAnswers.set(ApplyingForReliefPage, true).get
      val result = navigator.previousPage(DetailsOfThisSharePurchasePage, NormalMode, answers)
      result mustBe sh03AgentSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode)
    }

    "must go from the MaximumAmountPaidPage to DetailsOfThisSharePurchasePage" in {
      val result = navigator.previousPage(MaximumAmountPaidPage, NormalMode, emptyUserAnswers)
      result mustBe sh03AgentSingleRoutes.DetailsOfThisSharePurchaseController.onPageLoad(NormalMode)
    }

    "must go from the MinimumAmountPaidPage to MaximumAmountPaidPage" in {
      val result = navigator.previousPage(MinimumAmountPaidPage, NormalMode, emptyUserAnswers)
      result mustBe sh03AgentSingleRoutes.MaximumAmountPaidController.onPageLoad(NormalMode)
    }

    "must go from the ChargingPointPage to MinimumAmountPaidPage when company is a PLC" in {
      val answers = emptyUserAnswers.set(CompanyDetailsPage, companyDetails).get
      val result = navigator.previousPage(ChargingPointPage, NormalMode, answers)
      result mustBe sh03AgentSingleRoutes.MinimumAmountPaidController.onPageLoad(NormalMode)
    }

    "must go from the ChargingPointPage to DetailsOfThisSharePurchasePage when company is not a PLC" in {
      val answers = emptyUserAnswers.set(CompanyDetailsPage, companyDetails.copy(isPlc = false)).get
      val result = navigator.previousPage(ChargingPointPage, NormalMode, answers)
      result mustBe sh03AgentSingleRoutes.DetailsOfThisSharePurchaseController.onPageLoad(NormalMode)
    }

    "must go from the RoleAtPurchasingCompanyPage to ChargingPointPage" in {
      val result = navigator.previousPage(RoleAtPurchasingCompanyPage, NormalMode, emptyUserAnswers)
      result mustBe sh03AgentSingleRoutes.ChargingPointController.onPageLoad(NormalMode)
    }
  }
}
