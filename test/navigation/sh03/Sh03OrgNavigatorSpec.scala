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
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.organisations.routes as sh03OrgRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.organisations.single.routes as sh03OrgSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.shared.routes as sharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.organisations.bulk.routes as sh03OrgBulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes as stfSharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes as commonRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.HowToNotifyAboutShareBuyback
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.sh03.organisations.Sh03OrgNavigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.Page
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.fileUpload.routes as fileUploadRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType.SH03
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.bulk.{BulkCheckYourAnswersPage, BulkCompanyDetailsPage, BulkRoleAtPurchasingCompanyPage, CannotSubmitFormErrorPage}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.shared.CheckYourAnswersPage

import java.time.LocalDate

class Sh03OrgNavigatorSpec extends SpecBase with ScalaFutures {

  private val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]
  when(mockConfig.firstChargingPoint).thenReturn(LocalDate.of(2026, 1, 1))

  lazy val cyaPage: Call = sh03OrgSingleRoutes.CheckYourAnswersController.onPageLoad()
  val navigator = new Sh03OrgNavigator(StubAnswerPersistenceService(), mockConfig)

  private val companyDetails = CompanyDetails(
    companyName = "Company1",
    companyRegistrationNumber = "12345678",
    isPlc = true)

  private val purchaseDetails = DetailsOfThisSharePurchase(
    numberOfShares = 1,
    typeOfShares = "ordinary",
    amountPaid = BigDecimal(250),
    marketValue = Some(BigDecimal(50)))

  "Sh03OrgNavigator" - {

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

      "must go from HowToNotifyAboutShareBuybackPage to CompanyDetailsPage when OneAtATime is selected" in {
        val answers = emptyUserAnswers.set(HowToNotifyAboutShareBuybackPage, HowToNotifyAboutShareBuyback.OneAtATime).get
        val result = navigator.nextPage(HowToNotifyAboutShareBuybackPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03OrgSingleRoutes.CompanyDetailsController.onPageLoad(NormalMode)
        }
      }

      "must go from HowToNotifyAboutShareBuybackPage to Bulk CompanyDetailsPage when MoreThanOneAtATime is selected" in {
        val answers = emptyUserAnswers.set(HowToNotifyAboutShareBuybackPage, HowToNotifyAboutShareBuyback.MoreThanOneAtATime).get
        val result = navigator.nextPage(HowToNotifyAboutShareBuybackPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03OrgBulkRoutes.CompanyDetailsController.onPageLoad(NormalMode)
        }
      }

      "must go from CompanyDetailsPage to ReasonForPurchasePage" in {
        val answers = emptyUserAnswers.set(CompanyDetailsPage, companyDetails).get
        val result = navigator.nextPage(CompanyDetailsPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03OrgSingleRoutes.ReasonForPurchaseController.onPageLoad(NormalMode)
        }
      }

      "must go from ReasonForPurchasePage to TreasurySharesPage when 'For Cancellation' is selected" in {
        val answers = emptyUserAnswers.set(ReasonForPurchasePage, ReasonForPurchase.ForCancellation).get
        val result = navigator.nextPage(ReasonForPurchasePage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03OrgSingleRoutes.TreasurySharesController.onPageLoad(NormalMode)
        }
      }

      "must go from ReasonForPurchasePage to ConnectedPersonsPage when 'To Place Into Treasury' is selected" in {
        val answers = emptyUserAnswers.set(ReasonForPurchasePage, ReasonForPurchase.ToPlaceIntoTreasury).get
        val result = navigator.nextPage(ReasonForPurchasePage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03OrgSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode)
        }
      }

      "must go from TreasurySharesPage to ConnectedPersonsPage" in {
        val answers = emptyUserAnswers.set(TreasurySharesPage, true).get
        val result = navigator.nextPage(TreasurySharesPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03OrgSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode)
        }
      }

      "must go from the ConnectedPersonsPage to ApplyingForReliefPage" in {
        val answers = emptyUserAnswers.set(ConnectedPersonsPage, true).get
        val result = navigator.nextPage(ConnectedPersonsPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03OrgSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
        }
      }

      "must go from ApplyingForReliefPage to WhatReliefAreYouApplyingForPage when applying for a relief" in {
        val answers = emptyUserAnswers.set(ApplyingForReliefPage, true).get
        val result = navigator.nextPage(ApplyingForReliefPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03OrgSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode)
        }
      }

      "must go from ApplyingForReliefPage to DetailsOfThisSharePurchasePage" in {
        val answers = emptyUserAnswers.set(ApplyingForReliefPage, false).get
        val result = navigator.nextPage(ApplyingForReliefPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03OrgSingleRoutes.DetailsOfThisSharePurchaseController.onPageLoad(NormalMode)
        }
      }

      "must go from WhatReliefAreYouApplyingForPage to DetailsOfThisSharePurchasePage" in {
        val answers = emptyUserAnswers.set(WhatReliefAreYouApplyingForPage, "Group Relief").get
        val result = navigator.nextPage(WhatReliefAreYouApplyingForPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03OrgSingleRoutes.DetailsOfThisSharePurchaseController.onPageLoad(NormalMode)
        }
      }

      "must go from DetailsOfThisSharePurchasePage to MaximumAmountPaidPage is company is a PLC" in {
        val answers = emptyUserAnswers
          .set(CompanyDetailsPage, companyDetails)
          .flatMap(_.set(DetailsOfThisSharePurchasePage, purchaseDetails))
          .get
        val result = navigator.nextPage(DetailsOfThisSharePurchasePage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03OrgSingleRoutes.MaximumAmountPaidController.onPageLoad(NormalMode)
        }
      }

      "must go from DetailsOfThisSharePurchasePage to ChargingPointPage is company is not a PLC" in {
        val answers = emptyUserAnswers
          .set(CompanyDetailsPage, companyDetails.copy(isPlc = false))
          .flatMap(_.set(DetailsOfThisSharePurchasePage, purchaseDetails))
          .get
        val result = navigator.nextPage(DetailsOfThisSharePurchasePage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03OrgSingleRoutes.ChargingPointController.onPageLoad(NormalMode)
        }
      }

      "must go from MaximumAmountPaidPage to MinimumAmountPaidPage" in {
        val answers = emptyUserAnswers.set(MaximumAmountPaidPage, BigDecimal(350)).get
        val result = navigator.nextPage(MaximumAmountPaidPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03OrgSingleRoutes.MinimumAmountPaidController.onPageLoad(NormalMode)
        }
      }

      "must go from MinimumAmountPaidPage to ChargingPointPage" in {
        val answers = emptyUserAnswers.set(MinimumAmountPaidPage, BigDecimal(100)).get
        val result = navigator.nextPage(MinimumAmountPaidPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03OrgSingleRoutes.ChargingPointController.onPageLoad(NormalMode)
        }
      }

      "must go from ChargingPointPage to RoleAtPurchasingCompanyPage" in {
        val answers = emptyUserAnswers.set(ChargingPointPage, LocalDate.now()).get
        val result = navigator.nextPage(ChargingPointPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03OrgSingleRoutes.RoleAtPurchasingCompanyController.onPageLoad(NormalMode)
        }
      }

      "must go from RoleAtPurchasingCompanyPage to CheckYourAnswerPage" in {
        val answers = emptyUserAnswers.set(RoleAtPurchasingCompanyPage, RoleAtPurchasingCompany(role = "Director", uksOrgan = None)).get
        val result = navigator.nextPage(RoleAtPurchasingCompanyPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe cyaPage
        }
      }

      "must go from RoleAtPurchasingCompanyPage to CannotSubmitFormErrorPage when a user selects None of these (unsupportedRole)" in {
        val answers = emptyUserAnswers.set(RoleAtPurchasingCompanyPage, RoleAtPurchasingCompany(role = "unsupportedRole", uksOrgan = None)).get
        val result = navigator.nextPage(RoleAtPurchasingCompanyPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03OrgSingleRoutes.CannotSubmitFormErrorController.onPageLoad()
        }
      }

      "must go from CheckYourAnswersPage to ConfirmationPage" in {
        val result = navigator.nextPage(CheckYourAnswersPage, NormalMode, emptyUserAnswers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe stfSharedRoutes.ConfirmationController.onPageLoad()
        }
      }

      "must go from BulkRoleAtPurchasingCompanyPage to CannotSubmitFormErrorPage when user selects None of these (unsupportedRole)" in {
        val answers = emptyUserAnswers.set(BulkRoleAtPurchasingCompanyPage, RoleAtPurchasingCompany(role = "unsupportedRole", uksOrgan = None)).get
        val result = navigator.nextPage(BulkRoleAtPurchasingCompanyPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03OrgBulkRoutes.CannotSubmitFormErrorController.onPageLoad()
        }
      }

      "must go from BulkRoleAtPurchasingCompanyPage to CheckYourAnswersPage when a supported role is selected and file reference exists" in {
        val answers = emptyUserAnswers
          .set(BulkRoleAtPurchasingCompanyPage, RoleAtPurchasingCompany(role = "Director", uksOrgan = None)).get
          .setFileUploadReference("test-ref")

        val result = navigator.nextPage(BulkRoleAtPurchasingCompanyPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe sh03OrgBulkRoutes.CheckYourAnswersController.onPageLoad("test-ref")
        }
      }

      "must go from BulkCheckYourAnswersPage to ConfirmationPage" in {
        val result = navigator.nextPage(BulkCheckYourAnswersPage, NormalMode, emptyUserAnswers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe stfSharedRoutes.ConfirmationController.onPageLoad()
        }
      }

      "Backwards routes" - {

        "must go from the BulkCompanyDetailsPage to HowToNotifyAboutShareBuybackPage" in {
          val result = navigator.previousPage(BulkCompanyDetailsPage, NormalMode, emptyUserAnswers)
          result mustBe sh03OrgRoutes.HowToNotifyAboutShareBuybackController.onPageLoad()
        }

        "must go from the BulkRoleAtPurchasingCompanyPage to FileUploadPage" in {
          val result = navigator.previousPage(BulkRoleAtPurchasingCompanyPage, NormalMode, emptyUserAnswers)
          result mustBe fileUploadRoutes.FileUploadController.onPageLoad(SH03)
        }

        "must go from the CannotSubmitFormErrorPage to BulkRoleAtPurchasingCompanyPage" in {
          val result = navigator.previousPage(CannotSubmitFormErrorPage, NormalMode, emptyUserAnswers)
          result mustBe sh03OrgBulkRoutes.RoleAtPurchasingCompanyController.onPageLoad(NormalMode)
        }

        "must go from the CheckYourAnswersPage to RoleAtPurchasingCompanyPage" in {
          val result = navigator.previousPage(CheckYourAnswersPage, NormalMode, emptyUserAnswers)
          result mustBe sh03OrgSingleRoutes.RoleAtPurchasingCompanyController.onPageLoad(NormalMode)
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

    "must go from BulkCompanyDetailsPage to the Bulk Check Your Answers page when file reference exists" in {
      val answers = emptyUserAnswers.setFileUploadReference("test-ref")
      val result = navigator.nextPage(BulkCompanyDetailsPage, CheckMode, answers)(fakeRequest)
      whenReady(result) { res =>
        res mustBe sh03OrgBulkRoutes.CheckYourAnswersController.onPageLoad("test-ref")
      }
    }

    "must go from BulkCompanyDetailsPage to Journey Recovery when file reference is missing" in {
      val result = navigator.nextPage(BulkCompanyDetailsPage, CheckMode, emptyUserAnswers)(fakeRequest)
      whenReady(result) { res =>
        res mustBe commonRoutes.JourneyRecoveryController.onPageLoad()
      }
    }

    "must go from BulkRoleAtPurchasingCompanyPage to the Bulk Check Your Answers page when file reference exists" in {
      val answers = emptyUserAnswers.setFileUploadReference("test-ref")
      val result = navigator.nextPage(BulkRoleAtPurchasingCompanyPage, CheckMode, answers)(fakeRequest)
      whenReady(result) { res =>
        res mustBe sh03OrgBulkRoutes.CheckYourAnswersController.onPageLoad("test-ref")
      }
    }

    "must go from BulkRoleAtPurchasingCompanyPage to Journey Recovery when file reference is missing" in {
      val result = navigator.nextPage(BulkRoleAtPurchasingCompanyPage, CheckMode, emptyUserAnswers)(fakeRequest)
      whenReady(result) { res =>
        res mustBe commonRoutes.JourneyRecoveryController.onPageLoad()
      }
    }

    "must go from BulkCompanyDetailsPage to defaultPage when file reference is missing" in {
      val result = navigator.previousPage(BulkCompanyDetailsPage, CheckMode, emptyUserAnswers)
      result mustBe navigator.defaultPage
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

    "must go from the CompanyDetailsPage to HowToNotifyAboutShareBuybackPage" in {
      val result = navigator.previousPage(CompanyDetailsPage, NormalMode, emptyUserAnswers)
      result mustBe sh03OrgRoutes.HowToNotifyAboutShareBuybackController.onPageLoad()
    }

    "must go from the ReasonForPurchasePage to CompanyDetailsPage" in {
      val result = navigator.previousPage(ReasonForPurchasePage, NormalMode, emptyUserAnswers)
      result mustBe sh03OrgSingleRoutes.CompanyDetailsController.onPageLoad(NormalMode)
    }

    "must go from the TreasurySharesPage to ReasonForPurchasePage" in {
      val result = navigator.previousPage(TreasurySharesPage, NormalMode, emptyUserAnswers)
      result mustBe sh03OrgSingleRoutes.ReasonForPurchaseController.onPageLoad(NormalMode)
    }

    "must go from the ConnectedPersonsPage to TreasurySharesPage when reason for purchase is For cancellation" in {
      val answers = emptyUserAnswers.set(ReasonForPurchasePage, ReasonForPurchase.ForCancellation).get
      val result = navigator.previousPage(ConnectedPersonsPage, NormalMode, answers)
      result mustBe sh03OrgSingleRoutes.TreasurySharesController.onPageLoad(NormalMode)
    }

    "must go from the ConnectedPersonsPage to ReasonForPurchasePage when reason for purchase is TO place into treasury" in {
      val answers = emptyUserAnswers.set(ReasonForPurchasePage, ReasonForPurchase.ToPlaceIntoTreasury).get
      val result = navigator.previousPage(ConnectedPersonsPage, NormalMode, answers)
      result mustBe sh03OrgSingleRoutes.ReasonForPurchaseController.onPageLoad(NormalMode)
    }

    "must go from the ApplyingForReliefPage to ConnectedPersonsPage" in {
      val result = navigator.previousPage(ApplyingForReliefPage, NormalMode, emptyUserAnswers)
      result mustBe sh03OrgSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode)
    }

    "must go from the WhatReliefAreYouApplyingForPage to ApplyingForReliefPage" in {
      val result = navigator.previousPage(WhatReliefAreYouApplyingForPage, NormalMode, emptyUserAnswers)
      result mustBe sh03OrgSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
    }

    "must go from the DetailsOfThisSharePurchasePage to ApplyingForReliefPage when applying for a relief is false" in {
      val answers = emptyUserAnswers.set(ApplyingForReliefPage, false).get
      val result = navigator.previousPage(DetailsOfThisSharePurchasePage, NormalMode, answers)
      result mustBe sh03OrgSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
    }

    "must go from the DetailsOfThisSharePurchasePage to WhatReliefAreYouApplyingForPage when applying for a relief is true" in {
      val answers = emptyUserAnswers.set(ApplyingForReliefPage, true).get
      val result = navigator.previousPage(DetailsOfThisSharePurchasePage, NormalMode, answers)
      result mustBe sh03OrgSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode)
    }

    "must go from the MaximumAmountPaidPage to DetailsOfThisSharePurchasePage" in {
      val result = navigator.previousPage(MaximumAmountPaidPage, NormalMode, emptyUserAnswers)
      result mustBe sh03OrgSingleRoutes.DetailsOfThisSharePurchaseController.onPageLoad(NormalMode)
    }

    "must go from the MinimumAmountPaidPage to MaximumAmountPaidPage" in {
      val result = navigator.previousPage(MinimumAmountPaidPage, NormalMode, emptyUserAnswers)
      result mustBe sh03OrgSingleRoutes.MaximumAmountPaidController.onPageLoad(NormalMode)
    }

    "must go from the ChargingPointPage to MinimumAmountPaidPage when company is a PLC" in {
      val answers = emptyUserAnswers.set(CompanyDetailsPage, companyDetails).get
      val result = navigator.previousPage(ChargingPointPage, NormalMode, answers)
      result mustBe sh03OrgSingleRoutes.MinimumAmountPaidController.onPageLoad(NormalMode)
    }

    "must go from the ChargingPointPage to DetailsOfThisSharePurchasePage when company is not a PLC" in {
      val answers = emptyUserAnswers.set(CompanyDetailsPage, companyDetails.copy(isPlc = false)).get
      val result = navigator.previousPage(ChargingPointPage, NormalMode, answers)
      result mustBe sh03OrgSingleRoutes.DetailsOfThisSharePurchaseController.onPageLoad(NormalMode)
    }

    "must go from the RoleAtPurchasingCompanyPage to ChargingPointPage" in {
      val result = navigator.previousPage(RoleAtPurchasingCompanyPage, NormalMode, emptyUserAnswers)
      result mustBe sh03OrgSingleRoutes.ChargingPointController.onPageLoad(NormalMode)
    }

    "must go from the BulkCheckYourAnswersPage to BulkRoleAtPurchasingCompanyPage" in {
      val result = navigator.previousPage(BulkCheckYourAnswersPage, NormalMode, emptyUserAnswers)
      result mustBe sh03OrgBulkRoutes.RoleAtPurchasingCompanyController.onPageLoad(NormalMode)
    }
  }

  "UserAnswersValidator" - {

    "must return Right(true) for a complete and valid set of user answers" in {
      val answers = emptyUserAnswers
        .set(CompanyDetailsPage, companyDetails).get
        .set(ReasonForPurchasePage, ReasonForPurchase.ForCancellation).get
        .set(TreasurySharesPage, true).get
        .set(ConnectedPersonsPage, true).get
        .set(ApplyingForReliefPage, false).get
        .set(DetailsOfThisSharePurchasePage, purchaseDetails).get
        .set(MaximumAmountPaidPage, BigDecimal(350)).get
        .set(MinimumAmountPaidPage, BigDecimal(100)).get
        .set(ChargingPointPage, LocalDate.now()).get
        .set(RoleAtPurchasingCompanyPage, RoleAtPurchasingCompany(role = "Director", uksOrgan = None)).get

      val result = navigator.userAnswersValidator.validate(answers)(fakeRequest).futureValue
      result mustBe Right(true)
    }

    "must return Left(Call) to DetailsOfThisSharePurchasePage when ConnectedPersons is true but marketValue is None" in {
      val answers = emptyUserAnswers
        .set(CompanyDetailsPage, companyDetails).get
        .set(ReasonForPurchasePage, ReasonForPurchase.ForCancellation).get
        .set(TreasurySharesPage, true).get
        .set(ConnectedPersonsPage, true).get
        .set(ApplyingForReliefPage, false).get
        .set(DetailsOfThisSharePurchasePage, purchaseDetails.copy(marketValue = None)).get
        .set(MaximumAmountPaidPage, BigDecimal(350)).get
        .set(MinimumAmountPaidPage, BigDecimal(100)).get
        .set(ChargingPointPage, LocalDate.now()).get
        .set(RoleAtPurchasingCompanyPage, RoleAtPurchasingCompany(role = "Director", uksOrgan = None)).get

      val result = navigator.userAnswersValidator.validate(answers)(fakeRequest).futureValue
      result mustBe Left(sh03OrgSingleRoutes.DetailsOfThisSharePurchaseController.onPageLoad(CheckMode))
    }

    "must return Right(true) when ConnectedPersons is false and marketValue is None" in {
      val answers = emptyUserAnswers
        .set(CompanyDetailsPage, companyDetails).get
        .set(ReasonForPurchasePage, ReasonForPurchase.ForCancellation).get
        .set(TreasurySharesPage, true).get
        .set(ConnectedPersonsPage, false).get
        .set(ApplyingForReliefPage, false).get
        .set(DetailsOfThisSharePurchasePage, purchaseDetails.copy(marketValue = None)).get
        .set(MaximumAmountPaidPage, BigDecimal(350)).get
        .set(MinimumAmountPaidPage, BigDecimal(100)).get
        .set(ChargingPointPage, LocalDate.now()).get
        .set(RoleAtPurchasingCompanyPage, RoleAtPurchasingCompany(role = "Director", uksOrgan = None)).get

      val result = navigator.userAnswersValidator.validate(answers)(fakeRequest).futureValue
      result mustBe Right(true)
    }
  }
}