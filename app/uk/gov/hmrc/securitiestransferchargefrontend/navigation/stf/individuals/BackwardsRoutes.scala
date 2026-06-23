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
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.single.routes as individualSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.routes as individualRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes as sharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.NavigationHelper
import uk.gov.hmrc.securitiestransferchargefrontend.pages.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.HowToNotifyAboutSecuritiesTransferPage
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.{AmountPaidForSecuritiesPage, ApplyingForReliefPage, ChargingPointPage, ConfirmAddressPage, ConnectedPersonsPage, DetailsOfThisTransferPage, NameOfSellerPage, OtherSecuritiesTypePage, SecuritiesTargetPage, StfBuyersAddressPage, StfSellerAddressPage, TaxRatePage, TotalMarketValuePage, WhatReliefAreYouApplyingForPage, WhatTypeOfSecuritiesPage}

class BackwardsRoutes(defaultPage: Call):

  val navHelper: NavigationHelper = new NavigationHelper(defaultPage)
  import navHelper.*
  
  def predecessorRoutes(page: Page): UserAnswers => Call = page match {
    case HowToNotifyAboutSecuritiesTransferPage => _ => sharedRoutes.SubmissionsDashboardController.onPageLoad()
    case ConfirmAddressPage => _ => individualRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad()
    case StfBuyersAddressPage => _ => individualRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad()
    case NameOfSellerPage => _ => individualSingleRoutes.ConfirmAddressController.onPageLoad()
    case StfSellerAddressPage => _ => individualSingleRoutes.NameOfSellerController.onPageLoad(NormalMode)
    case ConnectedPersonsPage => _ => individualSingleRoutes.StfSellerAddressController.onPageLoad()
    case ApplyingForReliefPage => _ => individualSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode)
    case WhatReliefAreYouApplyingForPage => _ => individualSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
    case SecuritiesTargetPage => userAnswers => dataDependent(ApplyingForReliefPage, userAnswers) {
      case true => individualSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode)
      case false => individualSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
    }
    case ChargingPointPage => _ => individualSingleRoutes.SecuritiesTargetController.onPageLoad(NormalMode)
    case TaxRatePage => _ => individualSingleRoutes.ChargingPointController.onPageLoad(NormalMode)
    case WhatTypeOfSecuritiesPage => _ => individualSingleRoutes.TaxRateController.onPageLoad(NormalMode)
    case DetailsOfThisTransferPage => _ => individualSingleRoutes.WhatTypeOfSecuritiesController.onPageLoad(NormalMode)
    case OtherSecuritiesTypePage => _ => individualSingleRoutes.WhatTypeOfSecuritiesController.onPageLoad(NormalMode)
    case AmountPaidForSecuritiesPage => _ => individualSingleRoutes.OtherSecuritiesTypeController.onPageLoad(NormalMode)
    case TotalMarketValuePage => _ => individualSingleRoutes.AmountPaidForSecuritiesController.onPageLoad(NormalMode)
    case _ => _ => defaultPage
  }
