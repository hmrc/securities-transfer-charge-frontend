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
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.routes as sh03AgentRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.single.routes as sh03AgentSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.bulk.routes as sh03AgentBulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.shared.routes as sharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.fileUpload.routes as fileUploadRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.bulk.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.HowToNotifyAboutShareBuyback
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.bulk.CompanyDetails as AgentBulkCompanyDetails
import uk.gov.hmrc.securitiestransferchargefrontend.models.shared.AgentReference
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.sh03.agents.Sh03AgentNavigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.Page
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.shared.CheckYourAnswersPage
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes as stfSharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType.SH03

import java.time.LocalDate

class Sh03AgentNavigatorSpec extends SpecBase with ScalaFutures {

  private val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]
  when(mockConfig.firstChargingPoint).thenReturn(LocalDate.of(2026, 1, 1))

  private lazy val cyaPage = sh03AgentSingleRoutes.CheckYourAnswersController.onPageLoad()
  private lazy val agentBulkCyaPage = sh03AgentBulkRoutes.CheckYourAnswersController.onPageLoad()
  val navigator = new Sh03AgentNavigator(StubAnswerPersistenceService(), mockConfig)

  private val companyDetails = CompanyDetails(
    companyName = "Company1",
    companyRegistrationNumber = "12345678",
    isPlc = true)

  private val agentBulkCompanyDetails = AgentBulkCompanyDetails(
    companyName = "Company2",
    companyRegistrationNumber = "12345678"
  )

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

      "must go from the HowToNotifyAboutShareBuybackPage to BulkAgentReferencePage when one at a time is selected" in {
        val answers = emptyUserAnswers.set(HowToNotifyAboutShareBuybackPage, HowToNotifyAboutShareBuyback.MoreThanOneAtATime).get
        val result = navigator.nextPage(HowToNotifyAboutShareBuybackPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03AgentBulkRoutes.AgentReferenceController.onPageLoad(NormalMode)
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
          res mustBe cyaPage
        }
      }

      "must go from RoleAtPurchasingCompanyPage to CannotSubmitFormErrorPage when a selects None of these (unsupportedRole)" in {
        val answers = emptyUserAnswers.set(RoleAtPurchasingCompanyPage, RoleAtPurchasingCompany(role = "unsupportedRole", uksOrgan = None)).get
        val result = navigator.nextPage(RoleAtPurchasingCompanyPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03AgentSingleRoutes.CannotSubmitFormErrorController.onPageLoad()
        }
      }

      "must go from CheckYourAnswersPage to ConfirmationController" in {
        val result = navigator.nextPage(CheckYourAnswersPage, NormalMode, emptyUserAnswers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe stfSharedRoutes.ConfirmationController.onPageLoad()
        }
      }

      "must go from BulkCheckYourAnswersPage to ConfirmationController" in {
        val result = navigator.nextPage(BulkCheckYourAnswersPage, NormalMode, emptyUserAnswers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe stfSharedRoutes.ConfirmationController.onPageLoad()
        }
      }

      "must go from BulkAgentReferencePage to the Company Details page" in {
        val answers = emptyUserAnswers.set(BulkAgentReferencePage, AgentReference(Some("HMRC"))).get
        val result = navigator.nextPage(BulkAgentReferencePage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03AgentBulkRoutes.CompanyDetailsController.onPageLoad(NormalMode)
        }
      }

      "must go from BulkCompanyDetailsPage to the Template Instructions page" in {
        val answers = emptyUserAnswers.set(BulkCompanyDetailsPage, agentBulkCompanyDetails).get
        val result = navigator.nextPage(BulkCompanyDetailsPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03AgentBulkRoutes.TemplateInstructionsController.onPageLoad()
        }
      }

      "must go from BulkRoleAtPurchasingCompanyPage to the check your answers page when a supported role is provided" in {
        val answers = emptyUserAnswers.set(BulkRoleAtPurchasingCompanyPage, RoleAtPurchasingCompany(role = "Director", uksOrgan = None)).get
        val result = navigator.nextPage(BulkRoleAtPurchasingCompanyPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe agentBulkCyaPage
        }
      }

      "must go from BulkRoleAtPurchasingCompanyPage to CannotSubmitFormErrorPage when an unsupportedRole is selected" in {
        val answers = emptyUserAnswers.set(BulkRoleAtPurchasingCompanyPage, RoleAtPurchasingCompany(role = "unsupportedRole", uksOrgan = None)).get
        val result = navigator.nextPage(BulkRoleAtPurchasingCompanyPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03AgentBulkRoutes.CannotSubmitFormErrorController.onPageLoad()
        }
      }
    }
  }

  "in Check mode" - {

    "must go from a page that doesn't exist in the edit route map to CheckYourAnswers" in {

      case object UnknownPage extends Page
      val result = navigator.nextPage(UnknownPage, CheckMode, UserAnswers(testUserId, testGroupIdentifier, submissionId))(fakeRequest)
      whenReady(result) { res =>
        res mustBe cyaPage
      }
    }

    "must go from BulkAgentReferencePage to the Bulk Check Your Answers page" in {
      val result = navigator.nextPage(BulkAgentReferencePage, CheckMode, emptyUserAnswers)(fakeRequest)
      whenReady(result) { res =>
        res mustBe agentBulkCyaPage
      }
    }

    "must go from BulkCompanyDetailsPage to the Bulk Check Your Answers page" in {
      val result = navigator.nextPage(BulkCompanyDetailsPage, CheckMode, emptyUserAnswers)(fakeRequest)
      whenReady(result) { res =>
        res mustBe agentBulkCyaPage
      }
    }

    "must go from BulkRoleAtPurchasingCompanyPage to the Bulk Check Your Answers page" in {
      val result = navigator.nextPage(BulkRoleAtPurchasingCompanyPage, CheckMode, emptyUserAnswers)(fakeRequest)
      whenReady(result) { res =>
        res mustBe agentBulkCyaPage
      }
    }

    "must go from ReasonForPurchasePage to the TreasurySharesPage when reason is cancellation and no answer has been provided on the TreasurySharesPage" in {
      val answers = emptyUserAnswers.set(ReasonForPurchasePage, ReasonForPurchase.ForCancellation).get
      val result = navigator.nextPage(ReasonForPurchasePage, CheckMode, answers)(fakeRequest)
      whenReady(result) { res =>
        res mustBe sh03AgentSingleRoutes.TreasurySharesController.onPageLoad(CheckMode)
      }
    }

    "must go from ReasonForPurchasePage to the Check Your Answers page when reason is cancellation and an answer has been provided on the TreasurySharesPage" in {
      val answers = emptyUserAnswers.set(ReasonForPurchasePage, ReasonForPurchase.ForCancellation).get.set(TreasurySharesPage, true).get
      val result = navigator.nextPage(ReasonForPurchasePage, CheckMode, answers)(fakeRequest)
      whenReady(result) { res =>
        res mustBe cyaPage
      }
    }

    "must go from ApplyingForReliefPage to the Check Your Answers page when answer is false" in {
      val answers = emptyUserAnswers.set(ApplyingForReliefPage, false).get
      val result = navigator.nextPage(ApplyingForReliefPage, CheckMode, answers)(fakeRequest)
      whenReady(result) { res =>
        res mustBe cyaPage
      }
    }

    "must go from ApplyingForReliefPage to the WhatReliefAreYouApplyingForPage when answer is true and no answer has been provided on the WhatReliefAreYouApplyingForPage" in {
      val answers = emptyUserAnswers.set(ApplyingForReliefPage, true).get
      val result = navigator.nextPage(ApplyingForReliefPage, CheckMode, answers)(fakeRequest)
      whenReady(result) { res =>
        res mustBe sh03AgentSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad(CheckMode)
      }
    }

  }

  "in Previous Pages" - {

    "must go from a page that doesn't exist in the previous route map to Journey Recovery" in {
      case object UnknownPage extends Page
      val result = navigator.previousPage(UnknownPage, NormalMode, emptyUserAnswers)
      result mustBe navigator.defaultPage
    }

    "must go from the HowToNotifyAboutShareBuybackPage to BeforeYouStartPage" in {
      val result = navigator.previousPage(HowToNotifyAboutShareBuybackPage, NormalMode, emptyUserAnswers)
      result mustBe sharedRoutes.BeforeYouStartController.onPageLoad()
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

    "must go from the CheckYourAnswersPage to RoleAtPurchasingCompanyPage" in {
      val result = navigator.previousPage(CheckYourAnswersPage, NormalMode, emptyUserAnswers)
      result mustBe sh03AgentSingleRoutes.RoleAtPurchasingCompanyController.onPageLoad(NormalMode)
    }

    "must go from the BulkCheckYourAnswersPage to BulkRoleAtPurchasingCompanyPage" in {
      val result = navigator.previousPage(BulkCheckYourAnswersPage, NormalMode, emptyUserAnswers)
      result mustBe sh03AgentBulkRoutes.RoleAtPurchasingCompanyController.onPageLoad(NormalMode)
    }

    "must go from the BulkRoleAtPurchasingCompanyPage to File upload page" in {
      val result = navigator.previousPage(BulkRoleAtPurchasingCompanyPage, NormalMode, emptyUserAnswers)
      result mustBe fileUploadRoutes.FileUploadController.onPageLoad(SH03)
    }

    "must go from the BulkCompanyDetailsPage to BulkAgentReferenceControllerPage" in {
      val result = navigator.previousPage(BulkCompanyDetailsPage, NormalMode, emptyUserAnswers)
      result mustBe sh03AgentBulkRoutes.AgentReferenceController.onPageLoad(NormalMode)
    }

    "must go from the BulkAgentReferencePage to HowToNotifyAboutShareBuyBackPage" in {
      val result = navigator.previousPage(BulkAgentReferencePage, NormalMode, emptyUserAnswers)
      result mustBe sh03AgentRoutes.HowToNotifyAboutShareBuybackController.onPageLoad()
    }

    "must go from the BulkRoleAtPurchasingCompanyPage to BulkCheckYourAnswersPage in CheckMode" in {
      val result = navigator.previousPage(BulkRoleAtPurchasingCompanyPage, CheckMode, emptyUserAnswers)
      result mustBe sh03AgentBulkRoutes.CheckYourAnswersController.onPageLoad()
    }

    "must go from the BulkAgentReferencePage to BulkCheckYourAnswersPage in CheckMode" in {
      val result = navigator.previousPage(BulkAgentReferencePage, CheckMode, emptyUserAnswers)
      result mustBe sh03AgentBulkRoutes.CheckYourAnswersController.onPageLoad()
    }

    "must go from the BulkCompanyDetailsPage to BulkCheckYourAnswersPage in CheckMode" in {
      val result = navigator.previousPage(BulkCompanyDetailsPage, CheckMode, emptyUserAnswers)
      result mustBe sh03AgentBulkRoutes.CheckYourAnswersController.onPageLoad()
    }
  }
}
