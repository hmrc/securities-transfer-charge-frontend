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

package navigation.stf

import base.stubs.StubAnswerPersistenceService
import base.{Fixtures, SpecBase}
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations.bulk.routes as orgBulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations.routes as orgRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations.single.routes as orgSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes as sharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.*
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.stf.organisations.StfOrgNavigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.{HowToNotifyAboutSecuritiesTransferPage, SubmissionsDashboardPage}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.*

import java.time.LocalDate

class StfOrgNavigatorSpec extends SpecBase with ScalaFutures {

  private val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]
  when(mockConfig.firstChargingPoint).thenReturn(LocalDate.of(2026, 1, 1))

  val validAnswer = 0

  val navigator = new StfOrgNavigator(mockConfig, StubAnswerPersistenceService())

  "OrgNavigator" - {

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

      "must go from SubmissionsDashboardPage to HowToNotifyAboutSecuritiesTransferController" in {
        val result = navigator.nextPage(SubmissionsDashboardPage, NormalMode, emptyUserAnswers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe orgRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad(NormalMode)
        }
      }

      "must go from the HowToNotifyAboutSecuritiesTransfer to ConfirmAddressController when one at a time is selected" in {
        val answers = emptyUserAnswers.set(HowToNotifyAboutSecuritiesTransferPage, HowToNotifyAboutSecuritiesTransfer.OneAtATime).get
        val result = navigator.nextPage(HowToNotifyAboutSecuritiesTransferPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe orgSingleRoutes.ConfirmAddressController.onPageLoad()
        }
      }

      "must go from the HowToNotifyAboutSecuritiesTransfer to TemplateInstructionsController when more than one at a time is selected" in {
        val answers = emptyUserAnswers.set(HowToNotifyAboutSecuritiesTransferPage, HowToNotifyAboutSecuritiesTransfer.MoreThanOneAtATime).get
        val result = navigator.nextPage(HowToNotifyAboutSecuritiesTransferPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe orgBulkRoutes.TemplateInstructionsController.onPageLoad()
        }
      }

      "must go from the ConfirmAddressPage to NameOfSellerController" in {
        val answers = emptyUserAnswers.set(ConfirmAddressPage, Fixtures.confirmableAddress).get
        val result = navigator.nextPage(ConfirmAddressPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe orgSingleRoutes.NameOfSellerController.onPageLoad(NormalMode)
        }
      }

      "must go from the NameOfSellerPage to StfSellerAddressPage" in {
        val answers = emptyUserAnswers.set(NameOfSellerPage, "John").get
        val result = navigator.nextPage(NameOfSellerPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe orgSingleRoutes.StfSellerAddressController.onPageLoad()
        }
      }

      "must go from the StfBuyersAddressPage to NameOfSellerController" in {
        val answers = emptyUserAnswers.set(StfBuyersAddressPage, Fixtures.fakeAlfConfirmedAddress).get
        val result = navigator.nextPage(StfBuyersAddressPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe orgSingleRoutes.NameOfSellerController.onPageLoad(NormalMode)
        }
      }

      "must go from the StfSellerAddressPage to ConnectedPersonsController" in {
        val answers = emptyUserAnswers.set(StfSellerAddressPage, Fixtures.fakeAlfConfirmedAddress).get
        val result = navigator.nextPage(StfSellerAddressPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe orgSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode)
        }
      }

      "must go from the ConnectedPersonsPage to ApplyingForReliefController" in {
        val answers = emptyUserAnswers.set(ConnectedPersonsPage, true).get
        val result = navigator.nextPage(ConnectedPersonsPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe orgSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
        }
      }

      "must go from the ApplyingForReliefPage to WhatReliefAreYouApplyingForController when yes is selected" in {
        val answers = emptyUserAnswers.set(ApplyingForReliefPage, true).get
        val result = navigator.nextPage(ApplyingForReliefPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe orgSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode)
        }
      }

      "must go from the ApplyingForReliefPage to SecuritiesTargetController when no is selected" in {
        val answers = emptyUserAnswers.set(ApplyingForReliefPage, false).get
        val result = navigator.nextPage(ApplyingForReliefPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe orgSingleRoutes.SecuritiesTargetController.onPageLoad(NormalMode)
        }
      }

      "must go from the WhatReliefAreYouApplyingForPage to SecuritiesTargetController" in {
        val answers = emptyUserAnswers.set(WhatReliefAreYouApplyingForPage, "Some relief").get
        val result = navigator.nextPage(WhatReliefAreYouApplyingForPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe orgSingleRoutes.SecuritiesTargetController.onPageLoad(NormalMode)
        }
      }

      "must go from the SecuritiesTargetPage to ChargingPointController" in {
        val answers = emptyUserAnswers.set(SecuritiesTargetPage, SecuritiesTarget("Business 1",Some("12345678"))).get
        val result = navigator.nextPage(SecuritiesTargetPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe orgSingleRoutes.ChargingPointController.onPageLoad(NormalMode)
        }
      }

      "must go from the ChargingPointPage to JourneyRecoveryController when the date entered is before 2026-01-01" in {
        val answers = emptyUserAnswers.set(ChargingPointPage, LocalDate.of(2025,1,2)).get
        val result = navigator.nextPage(ChargingPointPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe routes.JourneyRecoveryController.onPageLoad()
        }
      }

      "must go from the ChargingPointPage to TaxRateController when the date entered is after 2026-01-01" in {
        val answers = emptyUserAnswers.set(ChargingPointPage, LocalDate.now()).get
        val result = navigator.nextPage(ChargingPointPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe orgSingleRoutes.TaxRateController.onPageLoad(NormalMode)
        }
      }

      "must go from the TaxRatePage to WhatTypeOfSecuritiesController" in {
        val answers = emptyUserAnswers.set(TaxRatePage, TaxRate.HalfPercent).get
        val result = navigator.nextPage(TaxRatePage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe orgSingleRoutes.WhatTypeOfSecuritiesController.onPageLoad(NormalMode)
        }
      }

      "must go from the WhatTypeOfSecuritiesPage to DetailsOfThisTransferController when shares is selected" in {
        val answers = emptyUserAnswers.set(WhatTypeOfSecuritiesPage, WhatTypeOfSecurities.Shares).get
        val result = navigator.nextPage(WhatTypeOfSecuritiesPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe orgSingleRoutes.DetailsOfThisTransferController.onPageLoad(NormalMode)
        }
      }

      "must go from the WhatTypeOfSecuritiesPage to OtherSecuritiesTypeController when other is selected" in {
        val answers = emptyUserAnswers.set(WhatTypeOfSecuritiesPage, WhatTypeOfSecurities.Other).get
        val result = navigator.nextPage(WhatTypeOfSecuritiesPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe orgSingleRoutes.OtherSecuritiesTypeController.onPageLoad(NormalMode)
        }
      }

      "must go from the OtherSecuritiesTypePage to AmountPaidForSecuritiesController" in {
        val answers = emptyUserAnswers.set(OtherSecuritiesTypePage, "bonds").get
        val result = navigator.nextPage(OtherSecuritiesTypePage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe orgSingleRoutes.AmountPaidForSecuritiesController.onPageLoad(NormalMode)
        }
      }

      "must go from the AmountPaidForSecuritiesPage to TotalMarketValueController when connected persons is true" in {
        val answers = emptyUserAnswers.set(AmountPaidForSecuritiesPage, BigDecimal(500)).get
        val updated = answers.set(ConnectedPersonsPage, true).get
        val result = navigator.nextPage(AmountPaidForSecuritiesPage, NormalMode, updated)(fakeRequest)
        whenReady(result) { res =>
          res mustBe orgSingleRoutes.TotalMarketValueController.onPageLoad(NormalMode)
        }
      }

      "must go from the AmountPaidForSecuritiesPage to CheckYourAnswersController when connected persons is false" in {
        val answers = emptyUserAnswers.set(AmountPaidForSecuritiesPage, BigDecimal(500)).get
        val updated = answers.set(ConnectedPersonsPage, false).get
        val result = navigator.nextPage(AmountPaidForSecuritiesPage, NormalMode, updated)(fakeRequest)
        whenReady(result) { res =>
          res mustBe routes.CheckYourAnswersController.onPageLoad()
        }
      }

      "must go from the DetailsOfThisTransferPage to CheckYourAnswersController" in {
        val answers = emptyUserAnswers.set(DetailsOfThisTransferPage, DetailsOfThisTransfer(numberOfShares = 200,
          typeOfShares = "ordinary share", amountPaid = BigDecimal(500), marketValue = Some(BigDecimal(1500)))).get
        val result = navigator.nextPage(DetailsOfThisTransferPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe routes.CheckYourAnswersController.onPageLoad()
        }
      }

      "must go from the TotalMarketValuePage to CheckYourAnswersController" in {
        val answers = emptyUserAnswers.set(TotalMarketValuePage, BigDecimal(500)).get
        val result = navigator.nextPage(TotalMarketValuePage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe routes.CheckYourAnswersController.onPageLoad()
        }
      }
    }

    "in Check mode" - {

      "must go from a page that doesn't exist in the edit route map to CheckYourAnswers" in {

        case object UnknownPage extends Page
        val result = navigator.nextPage(UnknownPage, CheckMode, UserAnswers("id", submissionId))(fakeRequest)
        whenReady(result) { res =>
          res mustBe routes.CheckYourAnswersController.onPageLoad()
        }
      }
    }

    "Previous Pages" - {

      "must go from a page that doesn't exist in the previous route map to Journey Recovery" in {
        case object UnknownPage extends Page
        val result = navigator.previousPage(UnknownPage, NormalMode, emptyUserAnswers)
        result mustBe navigator.defaultPage
      }

      "must go from the TotalMarketValuePage to AmountPaidForSecurities" in {
        val result = navigator.previousPage(TotalMarketValuePage, NormalMode, emptyUserAnswers)
        result mustBe orgSingleRoutes.AmountPaidForSecuritiesController.onPageLoad(NormalMode)
      }

      "must go from the AmountPaidForSecuritiesPage to OtherSecuritiesType" in {
        val result = navigator.previousPage(AmountPaidForSecuritiesPage, NormalMode, emptyUserAnswers)
        result mustBe orgSingleRoutes.OtherSecuritiesTypeController.onPageLoad(NormalMode)
      }

      "must go from the OtherSecuritiesTypePage to WhatTypeOfSecurities" in {
        val result = navigator.previousPage(OtherSecuritiesTypePage, NormalMode, emptyUserAnswers)
        result mustBe orgSingleRoutes.WhatTypeOfSecuritiesController.onPageLoad(NormalMode)
      }

      "must go from the DetailsOfThisTransferPage to WhatTypeOfSecurities" in {
        val result = navigator.previousPage(DetailsOfThisTransferPage, NormalMode, emptyUserAnswers)
        result mustBe orgSingleRoutes.WhatTypeOfSecuritiesController.onPageLoad(NormalMode)
      }

      "must go from the WhatTypeOfSecuritiesPage to TaxRate" in {
        val result = navigator.previousPage(WhatTypeOfSecuritiesPage, NormalMode, emptyUserAnswers)
        result mustBe orgSingleRoutes.TaxRateController.onPageLoad(NormalMode)
      }

      "must go from the TaxRatePage to ChargingPoint" in {
        val result = navigator.previousPage(TaxRatePage, NormalMode, emptyUserAnswers)
        result mustBe orgSingleRoutes.ChargingPointController.onPageLoad(NormalMode)
      }

      "must go from the ChargingPointPage to SecuritiesTarget" in {
        val result = navigator.previousPage(ChargingPointPage, NormalMode, emptyUserAnswers)
        result mustBe orgSingleRoutes.SecuritiesTargetController.onPageLoad(NormalMode)
      }

      "must go from the SecuritiesTarget to WhatReliefAreYouApplyingFor if applying for a relief" in {
        val answers = emptyUserAnswers.set(ApplyingForReliefPage, true).get
        val result = navigator.previousPage(SecuritiesTargetPage, NormalMode, answers)
        result mustBe orgSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode)
      }

      "must go from the SecuritiesTarget to ApplyingForRelief if not applying for a relief" in {
        val answers = emptyUserAnswers.set(ApplyingForReliefPage, false).get
        val result = navigator.previousPage(SecuritiesTargetPage, NormalMode, answers)
        result mustBe orgSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
      }

      "must go from the WhatReliefAreYouApplyingForPage to ApplyingForRelief" in {
        val result = navigator.previousPage(WhatReliefAreYouApplyingForPage, NormalMode, emptyUserAnswers)
        result mustBe orgSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
      }

      "must go from the ApplyingForReliefPage to ConnectedPersons" in {
        val result = navigator.previousPage(ApplyingForReliefPage, NormalMode, emptyUserAnswers)
        result mustBe orgSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode)
      }

      "must go from the ConnectedPersonsPage to StfSellerAddress" in {
        val result = navigator.previousPage(ConnectedPersonsPage, NormalMode, emptyUserAnswers)
        result mustBe orgSingleRoutes.StfSellerAddressController.onPageLoad()
      }

      "must go from the NameOfSellerPage to ConfirmAddress" in {
        val result = navigator.previousPage(NameOfSellerPage, NormalMode, emptyUserAnswers)
        result mustBe orgSingleRoutes.ConfirmAddressController.onPageLoad()
      }

      "must go from the StfBuyersAddressPage to HowToNotifyAboutSecuritiesTransfer" in {
        val result = navigator.previousPage(StfBuyersAddressPage, NormalMode, emptyUserAnswers)
        result mustBe orgRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad(NormalMode)
      }

      "must go from the ConfirmAddressPage to HowToNotifyAboutSecuritiesTransfer" in {
        val result = navigator.previousPage(ConfirmAddressPage, NormalMode, emptyUserAnswers)
        result mustBe orgRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad(NormalMode)
      }

      "must go from the HowToNotifyAboutSecuritiesTransferPage to SubmissionsDashboard" in {
        val result = navigator.previousPage(HowToNotifyAboutSecuritiesTransferPage, NormalMode, emptyUserAnswers)
        result mustBe sharedRoutes.SubmissionsDashboardController.onPageLoad()
      }
    }

  }
}
