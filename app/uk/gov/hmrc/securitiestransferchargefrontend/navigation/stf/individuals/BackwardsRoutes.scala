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

package uk.gov.hmrc.securitiestransferchargefrontend.navigation.stf.individuals

import play.api.mvc.Call
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.routes as individualsRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.NavigationHelper
import uk.gov.hmrc.securitiestransferchargefrontend.pages.*

object BackwardsRoutes:

  val navHelper: NavigationHelper = new NavigationHelper(StfNavigator.defaultPage)
  import navHelper.*
  
  def predecessorRoutes(page: Page): UserAnswers => Call = page match {
    case HowToNotifyAboutSecuritiesTransferPage => _ => routes.SubmissionsDashboardController.onPageLoad()
    case ConfirmAddressPage => _ => individualsRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad(NormalMode)
    case StfBuyersAddressPage => _ => individualsRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad(NormalMode)
    case NameOfSellerPage => _ => individualsRoutes.ConfirmAddressController.onPageLoad()
    case SellerAddressPage => _ => individualsRoutes.NameOfSellerController.onPageLoad(NormalMode)
    case ConnectedPersonsPage => _ => individualsRoutes.StfSellerAddressController.onPageLoad()
    case ApplyingForReliefPage => _ => individualsRoutes.ConnectedPersonsController.onPageLoad(NormalMode)
    case WhatReliefAreYouApplyingForPage => _ => individualsRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
    case SecuritiesTargetPage => userAnswers => dataDependent(ApplyingForReliefPage, userAnswers) {
      case true => individualsRoutes.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode)
      case false => individualsRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
    }
    case ChargingPointPage => _ => individualsRoutes.SecuritiesTargetController.onPageLoad(NormalMode)
    case TaxRatePage => _ => individualsRoutes.ChargingPointController.onPageLoad(NormalMode)
    case WhatTypeOfSecuritiesPage => _ => individualsRoutes.TaxRateController.onPageLoad(NormalMode)
    case DetailsOfThisTransferPage => _ => individualsRoutes.WhatTypeOfSecuritiesController.onPageLoad(NormalMode)
    case OtherSecuritiesTypePage => _ => individualsRoutes.WhatTypeOfSecuritiesController.onPageLoad(NormalMode)
    case AmountPaidForSecuritiesPage => _ => individualsRoutes.OtherSecuritiesTypeController.onPageLoad(NormalMode)
    case TotalMarketValuePage => _ => individualsRoutes.AmountPaidForSecuritiesController.onPageLoad(NormalMode)
    case _ => _ => StfNavigator.defaultPage
  }
      

