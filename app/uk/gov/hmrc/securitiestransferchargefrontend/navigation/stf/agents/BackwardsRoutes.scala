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

package uk.gov.hmrc.securitiestransferchargefrontend.navigation.stf.agents

import play.api.mvc.Call
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.agents.routes as agentRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.agents.single.routes as agentSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes as sharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.fileUpload.routes as bulkSharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.HowToNotifyAboutSecuritiesTransfer.{MoreThanOneAtATime, OneAtATime}
import uk.gov.hmrc.securitiestransferchargefrontend.models.{JourneyType, NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.NavigationHelper
import uk.gov.hmrc.securitiestransferchargefrontend.pages.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.{AgentReferencePage, HowToNotifyAboutSecuritiesTransferPage, SubmissionsDashboardPage}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.JourneyRecoveryPage
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.bulk.FileUploadPage

class BackwardsRoutes(defaultPage: Call):

  val navHelper: NavigationHelper = new NavigationHelper(defaultPage)

  import navHelper.*

  def predecessorRoutesPage(page: Page, userAnswers: Option[UserAnswers]): Page = page match {
    case HowToNotifyAboutSecuritiesTransferPage => SubmissionsDashboardPage
    case AgentReferencePage =>
      userAnswers.flatMap(_.get(HowToNotifyAboutSecuritiesTransferPage)) match {
        case Some(OneAtATime) => HowToNotifyAboutSecuritiesTransferPage
        case Some(MoreThanOneAtATime) => FileUploadPage
        case _ => JourneyRecoveryPage
      }
    case NameOfBuyerPage => AgentReferencePage
    case StfBuyersAddressPage => NameOfBuyerPage
    case NameOfSellerPage => StfBuyersAddressPage
    case StfSellerAddressPage => NameOfSellerPage
    case ConnectedPersonsPage => StfSellerAddressPage
    case ApplyingForReliefPage => ConnectedPersonsPage
    case WhatReliefAreYouApplyingForPage => ApplyingForReliefPage
    case SecuritiesTargetPage =>
      userAnswers.flatMap(_.get(ApplyingForReliefPage)) match {
        case Some(true) => WhatReliefAreYouApplyingForPage
        case Some(false) => ApplyingForReliefPage
        case _ => JourneyRecoveryPage
      }
    case ChargingPointPage => SecuritiesTargetPage
    case TaxRatePage => ChargingPointPage
    case PurchasingSharesPage => TaxRatePage
    case DetailsOfThisTransferPage => PurchasingSharesPage
    case OtherSecuritiesTypePage => PurchasingSharesPage
    case AmountPaidForSecuritiesPage => OtherSecuritiesTypePage
    case TotalMarketValuePage => AmountPaidForSecuritiesPage
    case _ => JourneyRecoveryPage
  }

  def predecessorRoutes(page: Page): Option[UserAnswers] => Call = page match {

    case HowToNotifyAboutSecuritiesTransferPage => _ => sharedRoutes.SubmissionsDashboardController.onPageLoad()
    case AgentReferencePage => _.fold(defaultPage) { userAnswers =>
      dataDependent(HowToNotifyAboutSecuritiesTransferPage, userAnswers) {
        case OneAtATime => agentRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad()
        case MoreThanOneAtATime => bulkSharedRoutes.FileUploadController.onPageLoad(JourneyType.STF)
      }
    }
    case NameOfBuyerPage => _ => agentSingleRoutes.AgentReferenceController.onPageLoad(NormalMode)
    case StfBuyersAddressPage => _ => agentSingleRoutes.NameOfBuyerController.onPageLoad(NormalMode)
    case NameOfSellerPage => _ => agentSingleRoutes.AddressController.onPageLoad()
    case StfSellerAddressPage => _ => agentSingleRoutes.NameOfSellerController.onPageLoad(NormalMode)
    case ConnectedPersonsPage => _ => agentSingleRoutes.StfSellerAddressController.onPageLoad()
    case ApplyingForReliefPage => _ => agentSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode)
    case WhatReliefAreYouApplyingForPage => _ => agentSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
    case SecuritiesTargetPage => _.fold(defaultPage) { userAnswers =>
      dataDependent(ApplyingForReliefPage, userAnswers) {
        case true => agentSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode)
        case false => agentSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
      }
    }
    case ChargingPointPage => _ => agentSingleRoutes.SecuritiesTargetController.onPageLoad(NormalMode)
    case TaxRatePage => _ => agentSingleRoutes.ChargingPointController.onPageLoad(NormalMode)
    case PurchasingSharesPage => _ => agentSingleRoutes.TaxRateController.onPageLoad(NormalMode)
    case OtherSecuritiesTypePage => _ => agentSingleRoutes.PurchasingSharesController.onPageLoad(NormalMode)
    case AmountPaidForSecuritiesPage => _ => agentSingleRoutes.OtherSecuritiesTypeController.onPageLoad(NormalMode)
    case DetailsOfThisTransferPage => _ => agentSingleRoutes.PurchasingSharesController.onPageLoad(NormalMode)
    case TotalMarketValuePage => _ => agentSingleRoutes.AmountPaidForSecuritiesController.onPageLoad(NormalMode)
    case _ => _ => defaultPage
  }