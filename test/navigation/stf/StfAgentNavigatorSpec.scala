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
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.agents.routes as agentRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.agents.single.routes as agentSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.agents.bulk.routes as agentBulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.bulk.routes as bulkSharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes as sharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.stf.agents.StfAgentNavigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.Page
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.{AgentReferencePage, HowToNotifyAboutSecuritiesTransferPage, SubmissionsDashboardPage}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.*

import java.time.LocalDate

class StfAgentNavigatorSpec extends SpecBase with ScalaFutures {

  private val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]
  when(mockConfig.firstChargingPoint).thenReturn(LocalDate.of(2026, 1, 1))

  val detailsOfThisTransfer: DetailsOfThisTransfer = DetailsOfThisTransfer(numberOfShares = 25,
    typeOfShares = "stocks",
    amountPaid = BigDecimal(100),
    marketValue = Some(BigDecimal(10000)))

  val validAnswer = 0
  
  val navigator = new StfAgentNavigator(mockConfig, StubAnswerPersistenceService())

  "StfAgentNavigator" - {

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

      "must go from SubmissionsDashboardPage to HowToNotifyAboutSecuritiesTransferController" in {
        val result = navigator.nextPage(SubmissionsDashboardPage, NormalMode, emptyUserAnswers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe agentRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad(NormalMode)
        }
      }

      "must go from the HowToNotifyAboutSecuritiesTransfer to AgentReferencePage when one at a time is selected" in {
        val answers = emptyUserAnswers.set(HowToNotifyAboutSecuritiesTransferPage, HowToNotifyAboutSecuritiesTransfer.OneAtATime).get
        val result = navigator.nextPage(HowToNotifyAboutSecuritiesTransferPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe agentSingleRoutes.AgentReferenceController.onPageLoad(NormalMode)
        }
      }

      "must go from the HowToNotifyAboutSecuritiesTransfer to TemplateInstructionsController when more than one at a time is selected" in {
        val answers = emptyUserAnswers.set(HowToNotifyAboutSecuritiesTransferPage, HowToNotifyAboutSecuritiesTransfer.MoreThanOneAtATime).get
        val result = navigator.nextPage(HowToNotifyAboutSecuritiesTransferPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe agentBulkRoutes.TemplateInstructionsController.onPageLoad()
        }
      }

      "must go from the AgentReferencePage to NameOfBuyerPage if it is in single upload" in {
        val answers = emptyUserAnswers.set(HowToNotifyAboutSecuritiesTransferPage, HowToNotifyAboutSecuritiesTransfer.OneAtATime).get.set(AgentReferencePage, AgentReference(Some("HMRC"))).get
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

      "must go from the ConnectedPersonsPage to ApplyingForReliefPage" in {
        val answers = emptyUserAnswers.set(ConnectedPersonsPage, true).get
        val result = navigator.nextPage(ConnectedPersonsPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe agentSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
        }
      }

      "must go from ApplyingForReliefPage to WhatReliefAreYouApplyingForPage when true" in {
        val answers = emptyUserAnswers.set(ApplyingForReliefPage, true).get
        val result = navigator.nextPage(ApplyingForReliefPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe agentSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode)
        }
      }

      "must go from ApplyingForReliefPage to SecuritiesTargetPage when false" in {
        val answers = emptyUserAnswers.set(ApplyingForReliefPage, false).get
        val result = navigator.nextPage(ApplyingForReliefPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe agentSingleRoutes.SecuritiesTargetController.onPageLoad(NormalMode)
        }
      }

      "must go from WhatReliefAreYouApplyingForPage to SecuritiesTargetPage" in {
        val answers = emptyUserAnswers.set(WhatReliefAreYouApplyingForPage, "Some relief").get
        val result = navigator.nextPage(WhatReliefAreYouApplyingForPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe agentSingleRoutes.SecuritiesTargetController.onPageLoad(NormalMode)
        }
      }

      "must go from SecuritiesTargetPage to ChargingPointPage" in {
        val answers = emptyUserAnswers.set(SecuritiesTargetPage, SecuritiesTarget("Business 1", Some("12345678"))).get
        val result = navigator.nextPage(SecuritiesTargetPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe agentSingleRoutes.ChargingPointController.onPageLoad(NormalMode)
        }
      }

      "must go from the ChargingPointPage to JourneyRecoveryController when the date entered is before 2026-01-01" in {
        val answers = emptyUserAnswers.set(ChargingPointPage, LocalDate.of(2025, 1, 2)).get
        val result = navigator.nextPage(ChargingPointPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe routes.JourneyRecoveryController.onPageLoad()
        }
      }

      "must go from the ChargingPointPage to TaxRateController when the date entered is after 2026-01-01" in {
        val answers = emptyUserAnswers.set(ChargingPointPage, LocalDate.now()).get
        val result = navigator.nextPage(ChargingPointPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe agentSingleRoutes.TaxRateController.onPageLoad(NormalMode)
        }
      }

      "must go from TaxRatePage to WhatTypeOfSecuritiesPage" in {
        val answers = emptyUserAnswers.set(TaxRatePage, TaxRate.HalfPercent).get
        val result = navigator.nextPage(TaxRatePage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe agentSingleRoutes.WhatTypeOfSecuritiesController.onPageLoad(NormalMode)
        }
      }

      "must go from WhatTypeOfSecuritiesPage to OtherSecuritiesTypePage when 'Other' is selected" in {
        val answers = emptyUserAnswers.set(WhatTypeOfSecuritiesPage, WhatTypeOfSecurities.Other).get
        val result = navigator.nextPage(WhatTypeOfSecuritiesPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe agentSingleRoutes.OtherSecuritiesTypeController.onPageLoad(NormalMode)
        }
      }

      "must go from WhatTypeOfSecuritiesPage to DetailsOfThisTransferPage when 'Shares' is selected" in {
        val answers = emptyUserAnswers.set(WhatTypeOfSecuritiesPage, WhatTypeOfSecurities.Shares).get
        val result = navigator.nextPage(WhatTypeOfSecuritiesPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe agentSingleRoutes.DetailsOfThisTransferController.onPageLoad(NormalMode)
        }
      }

      "must go from DetailsOfThisTransferPage to CYA Page" in {
        val answers = emptyUserAnswers.set(DetailsOfThisTransferPage, detailsOfThisTransfer).get
        val result = navigator.nextPage(DetailsOfThisTransferPage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe routes.CheckYourAnswersController.onPageLoad()
        }
      }

      "must go from OtherSecuritiesTypePage to AmountPaidForSecuritiesPage" in {
        val answers = emptyUserAnswers.set(OtherSecuritiesTypePage, "Bonds").get
        val result = navigator.nextPage(OtherSecuritiesTypePage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe agentSingleRoutes.AmountPaidForSecuritiesController.onPageLoad(NormalMode)
        }
      }

      "must go from AmountPaidForSecuritiesPage to TotalMarketValuePage when connected persons is true" in {
        val answers = emptyUserAnswers.set(ConnectedPersonsPage, true).get.set(AmountPaidForSecuritiesPage, BigDecimal(1000)).get

        val result = navigator.nextPage(AmountPaidForSecuritiesPage, NormalMode, answers)(fakeRequest)

        whenReady(result) { res =>
          res mustBe agentSingleRoutes.TotalMarketValueController.onPageLoad(NormalMode)
        }
      }

      "must go from AmountPaidForSecuritiesPage to CYA when connected persons is false" in {
        val answers = emptyUserAnswers.set(ConnectedPersonsPage, false).get.set(AmountPaidForSecuritiesPage, BigDecimal(1000)).get

        val result = navigator.nextPage(AmountPaidForSecuritiesPage, NormalMode, answers)(fakeRequest)

        whenReady(result) { res =>
          res mustBe routes.CheckYourAnswersController.onPageLoad()
        }
      }

      "must go from TotalMarketValuePage to CYA Page" in {
        val answers = emptyUserAnswers.set(TotalMarketValuePage, validAnswer).get

        val result = navigator.nextPage(TotalMarketValuePage, NormalMode, answers)(fakeRequest)

        whenReady(result) { res =>
          res mustBe routes.CheckYourAnswersController.onPageLoad()
        }
      }

      "must go from the AgentReferencePage to CYA Page if it is in bulk upload" in {
        val answers = emptyUserAnswers.set(HowToNotifyAboutSecuritiesTransferPage, HowToNotifyAboutSecuritiesTransfer.MoreThanOneAtATime).get.set(AgentReferencePage, AgentReference(Some("HMRC"))).get
        val result = navigator.nextPage(AgentReferencePage, NormalMode, answers)(fakeRequest)
        whenReady(result) { res =>
          res mustBe routes.CheckYourAnswersController.onPageLoad()
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

      "must go from the HowToNotifyAboutSecuritiesTransferPage to SubmissionsDashboard" in {
        val result = navigator.previousPage(HowToNotifyAboutSecuritiesTransferPage, NormalMode, emptyUserAnswers)
        result mustBe sharedRoutes.SubmissionsDashboardController.onPageLoad()
      }

      "must go from the AgentReferencePage to HowToNotifyAboutSecuritiesTransferPage if it is in single journey" in {
        val answers = emptyUserAnswers.set(HowToNotifyAboutSecuritiesTransferPage, HowToNotifyAboutSecuritiesTransfer.OneAtATime).get
        val result = navigator.previousPage(AgentReferencePage, NormalMode, answers)
        result mustBe agentRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad(NormalMode)
      }
      "must go from the AgentReferencePage to HowToNotifyAboutSecuritiesTransferPage if it is in bulk journey" in {
        val answers = emptyUserAnswers.set(HowToNotifyAboutSecuritiesTransferPage, HowToNotifyAboutSecuritiesTransfer.MoreThanOneAtATime).get
        val result = navigator.previousPage(AgentReferencePage, NormalMode, answers)
        result mustBe bulkSharedRoutes.FileUploadController.onPageLoad()
      }

      "must go from the NameOfBuyerPage to AgentReferencePage" in {
        val result = navigator.previousPage(NameOfBuyerPage, NormalMode, emptyUserAnswers)
        result mustBe agentSingleRoutes.AgentReferenceController.onPageLoad(NormalMode)
      }

      "must go from the StfBuyersAddressPage to NameOfBuyerPage" in {
        val result = navigator.previousPage(StfBuyersAddressPage, NormalMode, emptyUserAnswers)
        result mustBe agentSingleRoutes.NameOfBuyerController.onPageLoad(NormalMode)
      }

      "must go from the NameOfSellerPage to StfBuyersAddressPage (AddressLookup)" in {
        val result = navigator.previousPage(NameOfSellerPage, NormalMode, emptyUserAnswers)
        result mustBe agentSingleRoutes.AddressController.onPageLoad()
      }

      "must go from the StfSellerAddressPage to NameOfSellerPage" in {
        val result = navigator.previousPage(StfSellerAddressPage, NormalMode, emptyUserAnswers)
        result mustBe agentSingleRoutes.NameOfSellerController.onPageLoad(NormalMode)
      }

      "must go from the ConnectedPersonsPage to StfSellerAddressPage" in {
        val result = navigator.previousPage(ConnectedPersonsPage, NormalMode, emptyUserAnswers)
        result mustBe agentSingleRoutes.StfSellerAddressController.onPageLoad()
      }

      "must go from the ApplyingForReliefPage to ConnectedPersonsPage" in {
        val result = navigator.previousPage(ApplyingForReliefPage, NormalMode, emptyUserAnswers)
        result mustBe agentSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode)
      }

      "must go from the WhatReliefAreYouApplyingForPage to ApplyingForReliefPage" in {
        val result = navigator.previousPage(WhatReliefAreYouApplyingForPage, NormalMode, emptyUserAnswers)
        result mustBe agentSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
      }

      "must go from the SecuritiesTargetPage to WhatReliefAreYouApplyingForPage when relief is applied for" in {
        val answers = emptyUserAnswers.set(ApplyingForReliefPage, true).get
        val result = navigator.previousPage(SecuritiesTargetPage, NormalMode, answers)
        result mustBe agentSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode)
      }

      "must go from the SecuritiesTargetPage to ApplyingForReliefPage when relief is NOT applied for" in {
        val answers = emptyUserAnswers.set(ApplyingForReliefPage, false).get
        val result = navigator.previousPage(SecuritiesTargetPage, NormalMode, answers)
        result mustBe agentSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
      }

      "must go from the ChargingPointPage to SecuritiesTargetPage" in {
        val result = navigator.previousPage(ChargingPointPage, NormalMode, emptyUserAnswers)
        result mustBe agentSingleRoutes.SecuritiesTargetController.onPageLoad(NormalMode)
      }

      "must go from the TaxRatePage to ChargingPointPage" in {
        val result = navigator.previousPage(TaxRatePage, NormalMode, emptyUserAnswers)
        result mustBe agentSingleRoutes.ChargingPointController.onPageLoad(NormalMode)
      }

      "must go from the WhatTypeOfSecuritiesPage to TaxRatePage" in {
        val result = navigator.previousPage(WhatTypeOfSecuritiesPage, NormalMode, emptyUserAnswers)
        result mustBe agentSingleRoutes.TaxRateController.onPageLoad(NormalMode)
      }

      "must go from the OtherSecuritiesTypePage to WhatTypeOfSecuritiesPage" in {
        val result = navigator.previousPage(OtherSecuritiesTypePage, NormalMode, emptyUserAnswers)
        result mustBe agentSingleRoutes.WhatTypeOfSecuritiesController.onPageLoad(NormalMode)
      }

      "must go from the AmountPaidForSecuritiesPage to OtherSecuritiesTypePage" in {
        val result = navigator.previousPage(AmountPaidForSecuritiesPage, NormalMode, emptyUserAnswers)
        result mustBe agentSingleRoutes.OtherSecuritiesTypeController.onPageLoad(NormalMode)
      }

      "must go from the DetailsOfThisTransferPage to WhatTypeOfSecurities" in {
        val result = navigator.previousPage(DetailsOfThisTransferPage, NormalMode, emptyUserAnswers)
        result mustBe agentSingleRoutes.WhatTypeOfSecuritiesController.onPageLoad(NormalMode)
      }

      "must go from the TotalMarketValuePage to AmountPaidForSecuritiesPage" in {
        val result = navigator.previousPage(TotalMarketValuePage, NormalMode, emptyUserAnswers)
        result mustBe agentSingleRoutes.AmountPaidForSecuritiesController.onPageLoad(NormalMode)
      }
    }
  }
}