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

package uk.gov.hmrc.securitiestransferchargefrontend.navigation.stf.organisations

import play.api.mvc.Call
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations.single.routes as orgSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations.routes as orgRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes as sharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.NavigationHelper
import uk.gov.hmrc.securitiestransferchargefrontend.pages.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.HowToNotifyAboutSecuritiesTransferPage
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.{AmountPaidForSecuritiesPage, ApplyingForReliefPage, ChargingPointPage, ConfirmAddressPage, ConnectedPersonsPage, DetailsOfThisTransferPage, NameOfSellerPage, OtherSecuritiesTypePage, SecuritiesTargetPage, StfBuyersAddressPage, TaxRatePage, TotalMarketValuePage, WhatReliefAreYouApplyingForPage, WhatTypeOfSecuritiesPage}

class BackwardsRoutes(defaultPage: Call):

  val navHelper: NavigationHelper = new NavigationHelper(defaultPage)
  import navHelper.*

  def predecessorRoutes(page: Page): UserAnswers => Call = page match {

    case HowToNotifyAboutSecuritiesTransferPage => _ => sharedRoutes.SubmissionsDashboardController.onPageLoad()
    case ConfirmAddressPage => _ => orgRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad(NormalMode)
    case StfBuyersAddressPage => _ =>orgRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad(NormalMode)
    case NameOfSellerPage => _ => orgSingleRoutes.ConfirmAddressController.onPageLoad()
    case ConnectedPersonsPage => _ => orgSingleRoutes.StfSellerAddressController.onPageLoad()
    case ApplyingForReliefPage  => _ => orgSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode)
    case WhatReliefAreYouApplyingForPage => _ => orgSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
    case SecuritiesTargetPage => userAnswers => dataDependent(ApplyingForReliefPage, userAnswers) {
      case true => orgSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode)
      case false => orgSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
    }
    case ChargingPointPage => _ => orgSingleRoutes.SecuritiesTargetController.onPageLoad(NormalMode)
    case TaxRatePage => _ => orgSingleRoutes.ChargingPointController.onPageLoad(NormalMode)
    case OtherSecuritiesTypePage => _ => orgSingleRoutes.WhatTypeOfSecuritiesController.onPageLoad(NormalMode)
    case WhatTypeOfSecuritiesPage => _ => orgSingleRoutes.TaxRateController.onPageLoad(NormalMode)
    case DetailsOfThisTransferPage  => _ => orgSingleRoutes.WhatTypeOfSecuritiesController.onPageLoad(NormalMode)
    case AmountPaidForSecuritiesPage => _ => orgSingleRoutes.OtherSecuritiesTypeController.onPageLoad(NormalMode)
    case TotalMarketValuePage => _ => orgSingleRoutes.AmountPaidForSecuritiesController.onPageLoad(NormalMode)
    case _ => _ => defaultPage

  }
