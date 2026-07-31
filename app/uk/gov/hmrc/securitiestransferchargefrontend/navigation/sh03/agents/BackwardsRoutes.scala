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

package uk.gov.hmrc.securitiestransferchargefrontend.navigation.sh03.agents

import play.api.mvc.Call
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.routes as sh03AgentRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.single.routes as sh03AgentSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.bulk.routes as sh03AgentBulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.shared.routes as sharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.ReasonForPurchase
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.NavigationHelper
import uk.gov.hmrc.securitiestransferchargefrontend.pages.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.bulk.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.fileUpload.routes as fileUploadRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType.SH03
import uk.gov.hmrc.securitiestransferchargefrontend.pages.JourneyRecoveryPage
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.BeforeYouStartPage

class BackwardsRoutes(defaultPage: Call):

  val navHelper: NavigationHelper = new NavigationHelper(defaultPage)

  import navHelper.*

  def predecessorRoutesPage(page: Page, userAnswers: Option[UserAnswers]): Page = page match {
    case HowToNotifyAboutShareBuybackPage => BeforeYouStartPage
    case AgentReferencePage => HowToNotifyAboutShareBuybackPage
    case CompanyDetailsPage => AgentReferencePage
    case ReasonForPurchasePage => CompanyDetailsPage
    case TreasurySharesPage => ReasonForPurchasePage
    case ConnectedPersonsPage =>
      userAnswers.flatMap(_.get(ReasonForPurchasePage)) match {
        case Some(ReasonForPurchase.ForCancellation) => TreasurySharesPage
        case Some(ReasonForPurchase.ToPlaceIntoTreasury) => ReasonForPurchasePage
        case _ => JourneyRecoveryPage
      }
    case ApplyingForReliefPage => ConnectedPersonsPage
    case WhatReliefAreYouApplyingForPage => ApplyingForReliefPage
    case DetailsOfThisSharePurchasePage =>
      userAnswers.flatMap(_.get(ApplyingForReliefPage)) match {
        case Some(true) => WhatReliefAreYouApplyingForPage
        case Some(false) => ApplyingForReliefPage
        case _ => JourneyRecoveryPage
      }
    case MaximumAmountPaidPage => DetailsOfThisSharePurchasePage
    case MinimumAmountPaidPage => MaximumAmountPaidPage
    case ChargingPointPage =>
      userAnswers.flatMap(_.get(CompanyDetailsPage)) match {
        case Some(companyDetails) if companyDetails.isPlc => MinimumAmountPaidPage
        case Some(_) => DetailsOfThisSharePurchasePage
        case _ => JourneyRecoveryPage
      }
    case RoleAtPurchasingCompanyPage => ChargingPointPage
    case BulkAgentReferencePage => HowToNotifyAboutShareBuybackPage
    case BulkCompanyDetailsPage => BulkAgentReferencePage
    case BulkRoleAtPurchasingCompanyPage => JourneyRecoveryPage
    case CannotSubmitFormErrorPage => BulkRoleAtPurchasingCompanyPage
    case _ => JourneyRecoveryPage
  }

  def predecessorRoutes(page: Page): Option[UserAnswers] => Call = page match {
    case HowToNotifyAboutShareBuybackPage => _ => sharedRoutes.BeforeYouStartController.onPageLoad()
    case AgentReferencePage => _ => sh03AgentRoutes.HowToNotifyAboutShareBuybackController.onPageLoad()
    case CompanyDetailsPage => _ => sh03AgentSingleRoutes.AgentReferenceController.onPageLoad(NormalMode)
    case ReasonForPurchasePage => _ => sh03AgentSingleRoutes.CompanyDetailsController.onPageLoad(NormalMode)
    case TreasurySharesPage => _ => sh03AgentSingleRoutes.ReasonForPurchaseController.onPageLoad(NormalMode)
    case ConnectedPersonsPage => _.fold(defaultPage) { userAnswers =>
      dataDependent(ReasonForPurchasePage, userAnswers) {
        case ReasonForPurchase.ForCancellation => sh03AgentSingleRoutes.TreasurySharesController.onPageLoad(NormalMode)
        case ReasonForPurchase.ToPlaceIntoTreasury => sh03AgentSingleRoutes.ReasonForPurchaseController.onPageLoad(NormalMode)
      }
    }
    case ApplyingForReliefPage => _ => sh03AgentSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode)
    case WhatReliefAreYouApplyingForPage => _ => sh03AgentSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode)

    case DetailsOfThisSharePurchasePage => _.fold(defaultPage) { userAnswers =>
      dataDependent(ApplyingForReliefPage, userAnswers) { applyingForRelief =>
        if (applyingForRelief)
          sh03AgentSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode)
        else
          sh03AgentSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
      }
    }
    case MaximumAmountPaidPage => _ => sh03AgentSingleRoutes.DetailsOfThisSharePurchaseController.onPageLoad(NormalMode)

    case MinimumAmountPaidPage => _ => sh03AgentSingleRoutes.MaximumAmountPaidController.onPageLoad(NormalMode)
    case ChargingPointPage => _.fold(defaultPage) { userAnswers =>
      dataDependent(CompanyDetailsPage, userAnswers) { companyDetails =>
        if (companyDetails.isPlc)
          sh03AgentSingleRoutes.MinimumAmountPaidController.onPageLoad(NormalMode)
        else
          sh03AgentSingleRoutes.DetailsOfThisSharePurchaseController.onPageLoad(NormalMode)
      }
    }
    case RoleAtPurchasingCompanyPage => _ => sh03AgentSingleRoutes.ChargingPointController.onPageLoad(NormalMode)
    case BulkAgentReferencePage => _ => sh03AgentRoutes.HowToNotifyAboutShareBuybackController.onPageLoad()
    case BulkCompanyDetailsPage => _ => sh03AgentBulkRoutes.AgentReferenceController.onPageLoad(NormalMode)
    case BulkRoleAtPurchasingCompanyPage => _ => fileUploadRoutes.FileUploadController.onPageLoad(SH03)
    case CannotSubmitFormErrorPage => _ => sh03AgentBulkRoutes.RoleAtPurchasingCompanyController.onPageLoad(NormalMode)
    case _ => _ => defaultPage
  }