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

package uk.gov.hmrc.securitiestransferchargefrontend.navigation.sh03.organisations

import play.api.mvc.Call
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.organisations.routes as sh03OrgRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.organisations.single.routes as sh03OrgSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.ReasonForPurchase
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.NavigationHelper
import uk.gov.hmrc.securitiestransferchargefrontend.pages.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.*

class BackwardsRoutes(defaultPage: Call):

  val navHelper: NavigationHelper = new NavigationHelper(defaultPage)

  import navHelper.*

  def predecessorRoutes(page: Page): UserAnswers => Call = page match {
    case CompanyDetailsPage => _ => sh03OrgRoutes.HowToNotifyAboutShareBuybackController.onPageLoad()
    case ReasonForPurchasePage => _ => sh03OrgSingleRoutes.CompanyDetailsController.onPageLoad(NormalMode)
    case TreasurySharesPage => _ => sh03OrgSingleRoutes.ReasonForPurchaseController.onPageLoad(NormalMode)
    case ConnectedPersonsPage => userAnswers =>
      dataDependent(ReasonForPurchasePage, userAnswers) {
        case ReasonForPurchase.ForCancellation => sh03OrgSingleRoutes.TreasurySharesController.onPageLoad(NormalMode)
        case ReasonForPurchase.ToPlaceIntoTreasury => sh03OrgSingleRoutes.ReasonForPurchaseController.onPageLoad(NormalMode)
      }
    case ApplyingForReliefPage => _ => sh03OrgSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode)
    case WhatReliefAreYouApplyingForPage => _ => sh03OrgSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode)

    case DetailsOfThisSharePurchasePage => userAnswers =>
      dataDependent(ApplyingForReliefPage, userAnswers) { applyingForRelief =>
        if (applyingForRelief)
          sh03OrgSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode)
        else
          sh03OrgSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
      }
    case MaximumAmountPaidPage => _ => sh03OrgSingleRoutes.DetailsOfThisSharePurchaseController.onPageLoad(NormalMode)

    case MinimumAmountPaidPage => _ => sh03OrgSingleRoutes.MaximumAmountPaidController.onPageLoad(NormalMode)
    case ChargingPointPage => userAnswers =>
      dataDependent(CompanyDetailsPage, userAnswers) { companyDetails =>
        if (companyDetails.isPlc)
          sh03OrgSingleRoutes.MinimumAmountPaidController.onPageLoad(NormalMode)
        else
          sh03OrgSingleRoutes.DetailsOfThisSharePurchaseController.onPageLoad(NormalMode)

      }
    case RoleAtPurchasingCompanyPage => _ => sh03OrgSingleRoutes.ChargingPointController.onPageLoad(NormalMode)
    case _ => _ => defaultPage
  }