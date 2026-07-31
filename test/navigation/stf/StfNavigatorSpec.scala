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
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.bulk.routes as individualBulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.routes as individualRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.single.routes as individualSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes as sharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.single.routes as stfSingleCyaRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.*
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.stf.individuals.StfNavigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.{HowToNotifyAboutSecuritiesTransferPage, SubmissionsDashboardPage}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.*

import java.time.LocalDate

class StfNavigatorSpec extends SpecBase with ScalaFutures {

  private val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]
  when(mockConfig.firstChargingPoint).thenReturn(LocalDate.of(2026, 1, 1))

  val validAnswer = 0
  
  val navigator = new StfNavigator(mockConfig, StubAnswerPersistenceService())

  "StfNavigator" - {

    "in Normal mode" - {

      "must go from a page that doesn't exist in the route map to default page" in {
        case object UnknownPage extends Page
        val result = navigator.nextPageCall(UnknownPage, NormalMode, UserAnswers(testUserId, testGroupIdentifier, submissionId))
        whenReady(result) { res =>
          res mustBe navigator.defaultPage
        }
      }

      "must go from any page to the dashboard page if isReturn is true" in {
        case object AnyPage extends Page
        val result = navigator.nextPageCall(AnyPage, NormalMode, UserAnswers(testUserId, testGroupIdentifier, submissionId), true)
        whenReady(result) { res =>
          res mustBe navigator.dashboardPage
        }
      }

      "must go from SubmissionsDashboardPage to HowToNotifyAboutSecuritiesTransferController" in {
        val result = navigator.nextPageCall(SubmissionsDashboardPage, NormalMode, emptyUserAnswers)
        whenReady(result) { res =>
          res mustBe individualRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad()
        }
      }

      "must go from the HowToNotifyAboutSecuritiesTransfer to ConfirmAddressController when one at a time is selected" in {
        val answers = emptyUserAnswers.set(HowToNotifyAboutSecuritiesTransferPage, HowToNotifyAboutSecuritiesTransfer.OneAtATime).get
        val result = navigator.nextPageCall(HowToNotifyAboutSecuritiesTransferPage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe individualSingleRoutes.ConfirmAddressController.onPageLoad()
        }
      }

      "must go from the HowToNotifyAboutSecuritiesTransfer to TemplateInstructionsController when more than one at a time is selected" in {
        val answers = emptyUserAnswers.set(HowToNotifyAboutSecuritiesTransferPage, HowToNotifyAboutSecuritiesTransfer.MoreThanOneAtATime).get
        val result = navigator.nextPageCall(HowToNotifyAboutSecuritiesTransferPage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe individualBulkRoutes.TemplateInstructionsController.onPageLoad()
        }
      }

      "must go from the ConfirmAddressPage to NameOfSellerController" in {
        val answers = emptyUserAnswers.set(ConfirmAddressPage, Fixtures.confirmableAddress).get
        val result = navigator.nextPageCall(ConfirmAddressPage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe individualSingleRoutes.NameOfSellerController.onPageLoad(NormalMode)
        }
      }

      "must go from the StfBuyersAddressPage to NameOfSellerController" in {
        val answers = emptyUserAnswers.set(StfBuyersAddressPage, Fixtures.fakeAlfConfirmedAddress).get
        val result = navigator.nextPageCall(StfBuyersAddressPage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe individualSingleRoutes.NameOfSellerController.onPageLoad(NormalMode)
        }
      }

      "must go from the NameOfSellerPage to StfSellerAddressPage" in {
        val answers = emptyUserAnswers.set(NameOfSellerPage, "John").get
        val result = navigator.nextPageCall(NameOfSellerPage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe individualSingleRoutes.StfSellerAddressController.onPageLoad()
        }
      }

      "must go from the sellers AddressLookup to ConnectedPersonsController" in {
        val answers = emptyUserAnswers.set(StfSellerAddressPage, Fixtures.fakeAlfConfirmedAddress).get
        val result = navigator.nextPageCall(StfSellerAddressPage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe individualSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode)
        }
      }

      "must go from the ConnectedPersonsPage to ApplyingForReliefController" in {
        val answers = emptyUserAnswers.set(ConnectedPersonsPage, true).get
        val result = navigator.nextPageCall(ConnectedPersonsPage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe individualSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
        }
      }

      "must go from the ApplyingForReliefPage to WhatReliefAreYouApplyingForController when yes is selected" in {
        val answers = emptyUserAnswers.set(ApplyingForReliefPage, true).get
        val result = navigator.nextPageCall(ApplyingForReliefPage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe individualSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode)
        }
      }

      "must go from the ApplyingForReliefPage to SecuritiesTargetController when no is selected" in {
        val answers = emptyUserAnswers.set(ApplyingForReliefPage, false).get
        val result = navigator.nextPageCall(ApplyingForReliefPage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe individualSingleRoutes.SecuritiesTargetController.onPageLoad(NormalMode)
        }
      }

      "must go from the WhatReliefAreYouApplyingForPage to SecuritiesTargetController" in {
        val answers = emptyUserAnswers.set(WhatReliefAreYouApplyingForPage, "Some relief").get
        val result = navigator.nextPageCall(WhatReliefAreYouApplyingForPage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe individualSingleRoutes.SecuritiesTargetController.onPageLoad(NormalMode)
        }
      }

      "must go from the SecuritiesTargetPage to ChargingPointController" in {
        val answers = emptyUserAnswers.set(SecuritiesTargetPage, SecuritiesTarget("Business 1",Some("12345678"))).get
        val result = navigator.nextPageCall(SecuritiesTargetPage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe individualSingleRoutes.ChargingPointController.onPageLoad(NormalMode)
        }
      }

      "must go from the ChargingPointPage to JourneyRecoveryController when the date entered is before 2026-01-01" in {
        val answers = emptyUserAnswers.set(ChargingPointPage, LocalDate.of(2025,1,2)).get
        val result = navigator.nextPageCall(ChargingPointPage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe routes.JourneyRecoveryController.onPageLoad()
        }
      }

      "must go from the ChargingPointPage to TaxRateController when the date entered is or after 2026-01-01" in {
        val answers = emptyUserAnswers.set(ChargingPointPage, LocalDate.now()).get
        val result = navigator.nextPageCall(ChargingPointPage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe individualSingleRoutes.TaxRateController.onPageLoad(NormalMode)
        }
      }

      "must go from the TaxRatePage to WhatTypeOfSecuritiesController" in {
        val answers = emptyUserAnswers.set(TaxRatePage, TaxRate.HalfPercent).get
        val result = navigator.nextPageCall(TaxRatePage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe individualSingleRoutes.WhatTypeOfSecuritiesController.onPageLoad(NormalMode)
        }
      }

      "must go from the WhatTypeOfSecuritiesPage to DetailsOfThisTransferController when shares is selected" in {
        val answers = emptyUserAnswers.set(WhatTypeOfSecuritiesPage, WhatTypeOfSecurities.Shares).get
        val result = navigator.nextPageCall(WhatTypeOfSecuritiesPage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe individualSingleRoutes.DetailsOfThisTransferController.onPageLoad(NormalMode)
        }
      }

      "must go from the WhatTypeOfSecuritiesPage to OtherSecuritiesTypeController when other is selected" in {
        val answers = emptyUserAnswers.set(WhatTypeOfSecuritiesPage, WhatTypeOfSecurities.Other).get
        val result = navigator.nextPageCall(WhatTypeOfSecuritiesPage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe individualSingleRoutes.OtherSecuritiesTypeController.onPageLoad(NormalMode)
        }
      }

      "must go from the OtherSecuritiesTypePage to AmountPaidForSecuritiesController" in {
        val answers = emptyUserAnswers.set(OtherSecuritiesTypePage, "bonds").get
        val result = navigator.nextPageCall(OtherSecuritiesTypePage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe individualSingleRoutes.AmountPaidForSecuritiesController.onPageLoad(NormalMode)
        }
      }

      "must go from the AmountPaidForSecuritiesPage to TotalMarketValueController when connected persons is true" in {
        val answers = emptyUserAnswers.set(AmountPaidForSecuritiesPage, BigDecimal(500)).get
        val updated = answers.set(ConnectedPersonsPage,true).get
        val result = navigator.nextPageCall(AmountPaidForSecuritiesPage, NormalMode, updated)
        whenReady(result) { res =>
          res mustBe individualSingleRoutes.TotalMarketValueController.onPageLoad(NormalMode)
        }
      }

      "must go from the AmountPaidForSecuritiesPage to CheckYourAnswersController when connected persons is false" in {
        val answers = emptyUserAnswers.set(AmountPaidForSecuritiesPage, BigDecimal(500)).get
        val updated = answers.set(ConnectedPersonsPage, false).get
        val result = navigator.nextPageCall(AmountPaidForSecuritiesPage, NormalMode, updated)
        whenReady(result) { res =>
          res mustBe stfSingleCyaRoutes.CheckYourAnswersController.onPageLoad()
        }
      }

      "must go from the DetailsOfThisTransferPage to CheckYourAnswersController" in {
        val answers = emptyUserAnswers.set(DetailsOfThisTransferPage, DetailsOfThisTransfer(numberOfShares = 200,
          typeOfShares = "ordinary share", amountPaid = BigDecimal(500), marketValue = Some(BigDecimal(1500)))).get
        val result = navigator.nextPageCall(DetailsOfThisTransferPage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe stfSingleCyaRoutes.CheckYourAnswersController.onPageLoad()
        }
      }

      "must go from the TotalMarketValuePage to CheckYourAnswersController" in {
        val answers = emptyUserAnswers.set(TotalMarketValuePage, validAnswer).get
        val result = navigator.nextPageCall(TotalMarketValuePage, NormalMode, answers)
        whenReady(result) { res =>
          res mustBe stfSingleCyaRoutes.CheckYourAnswersController.onPageLoad()
        }
      }
    }

    "in Check mode" - {

      "must go from a page that doesn't exist in the edit route map to CheckYourAnswers" in {

        case object UnknownPage extends Page
        val result = navigator.nextPageCall(UnknownPage, CheckMode, UserAnswers(testUserId, testGroupIdentifier, submissionId))
        whenReady(result) { res =>
          res mustBe stfSingleCyaRoutes.CheckYourAnswersController.onPageLoad()
        }
      }
    }

    "Previous Pages" - {

      "must go from a page that doesn't exist in the previous route map to Journey Recovery" in {
        case object UnknownPage extends Page
        val result = navigator.previousPageCall(UnknownPage, NormalMode, emptyUserAnswers)
        result mustBe navigator.defaultPage
      }

      "must go from the TotalMarketValuePage to AmountPaidForSecurities" in {
        val result = navigator.previousPageCall(TotalMarketValuePage, NormalMode, emptyUserAnswers)
        result mustBe individualSingleRoutes.AmountPaidForSecuritiesController.onPageLoad(NormalMode)
      }

      "must go from the AmountPaidForSecuritiesPage to OtherSecuritiesType" in {
        val result = navigator.previousPageCall(AmountPaidForSecuritiesPage, NormalMode, emptyUserAnswers)
        result mustBe individualSingleRoutes.OtherSecuritiesTypeController.onPageLoad(NormalMode)
      }

      "must go from the OtherSecuritiesTypePage to WhatTypeOfSecurities" in {
        val result = navigator.previousPageCall(OtherSecuritiesTypePage, NormalMode, emptyUserAnswers)
        result mustBe individualSingleRoutes.WhatTypeOfSecuritiesController.onPageLoad(NormalMode)
      }

      "must go from the DetailsOfThisTransferPage to WhatTypeOfSecurities" in {
        val result = navigator.previousPageCall(DetailsOfThisTransferPage, NormalMode, emptyUserAnswers)
        result mustBe individualSingleRoutes.WhatTypeOfSecuritiesController.onPageLoad(NormalMode)
      }

      "must go from the WhatTypeOfSecuritiesPage to TaxRate" in {
        val result = navigator.previousPageCall(WhatTypeOfSecuritiesPage, NormalMode, emptyUserAnswers)
        result mustBe individualSingleRoutes.TaxRateController.onPageLoad(NormalMode)
      }

      "must go from the TaxRatePage to ChargingPoint" in {
        val result = navigator.previousPageCall(TaxRatePage, NormalMode, emptyUserAnswers)
        result mustBe individualSingleRoutes.ChargingPointController.onPageLoad(NormalMode)
      }

      "must go from the ChargingPointPage to SecuritiesTarget" in {
        val result = navigator.previousPageCall(ChargingPointPage, NormalMode, emptyUserAnswers)
        result mustBe individualSingleRoutes.SecuritiesTargetController.onPageLoad(NormalMode)
      }

      "must go from the SecuritiesTarget to WhatReliefAreYouApplyingFor if applying for a relief" in {
        val answers = emptyUserAnswers.set(ApplyingForReliefPage, true).get
        val result = navigator.previousPageCall(SecuritiesTargetPage, NormalMode, answers)
        result mustBe individualSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode)
      }

      "must go from the SecuritiesTarget to ApplyingForRelief if not applying for a relief" in {
        val answers = emptyUserAnswers.set(ApplyingForReliefPage, false).get
        val result = navigator.previousPageCall(SecuritiesTargetPage, NormalMode, answers)
        result mustBe individualSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
      }

      "must go from the WhatReliefAreYouApplyingForPage to ApplyingForRelief" in {
        val result = navigator.previousPageCall(WhatReliefAreYouApplyingForPage, NormalMode, emptyUserAnswers)
        result mustBe individualSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
      }

      "must go from the ApplyingForReliefPage to ConnectedPersons" in {
        val result = navigator.previousPageCall(ApplyingForReliefPage, NormalMode, emptyUserAnswers)
        result mustBe individualSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode)
      }

      "must go from the ConnectedPersonsPage to StfSellerAddress" in {
        val result = navigator.previousPageCall(ConnectedPersonsPage, NormalMode, emptyUserAnswers)
        result mustBe individualSingleRoutes.StfSellerAddressController.onPageLoad()
      }

      "must go from the StfSellerAddressPage to NameOfSeller" in {
        val result = navigator.previousPageCall(StfSellerAddressPage, NormalMode, emptyUserAnswers)
        result mustBe individualSingleRoutes.NameOfSellerController.onPageLoad(NormalMode)
      }

      "must go from the NameOfSellerPage to ConfirmAddress" in {
        val result = navigator.previousPageCall(NameOfSellerPage, NormalMode, emptyUserAnswers)
        result mustBe individualSingleRoutes.ConfirmAddressController.onPageLoad()
      }

      "must go from the StfBuyersAddressPage to HowToNotifyAboutSecuritiesTransfer" in {
        val result = navigator.previousPageCall(StfBuyersAddressPage, NormalMode, emptyUserAnswers)
        result mustBe individualRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad()
      }

      "must go from the ConfirmAddressPage to HowToNotifyAboutSecuritiesTransfer" in {
        val result = navigator.previousPageCall(ConfirmAddressPage, NormalMode, emptyUserAnswers)
        result mustBe individualRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad()
      }

      "must go from the HowToNotifyAboutSecuritiesTransferPage to SubmissionsDashboard" in {
        val result = navigator.previousPageCall(HowToNotifyAboutSecuritiesTransferPage, NormalMode, emptyUserAnswers)
        result mustBe sharedRoutes.SubmissionsDashboardController.onPageLoad()
      }
    }

  }
}
