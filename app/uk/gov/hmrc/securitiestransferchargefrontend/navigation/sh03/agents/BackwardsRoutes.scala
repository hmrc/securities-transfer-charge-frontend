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
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.routes as sh03AgentRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.single.routes as sh03AgentSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.ReasonForPurchase
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.NavigationHelper
import uk.gov.hmrc.securitiestransferchargefrontend.pages.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03._

class BackwardsRoutes(defaultPage: Call):

  val navHelper: NavigationHelper = new NavigationHelper(defaultPage)

  import navHelper.*

  def predecessorRoutes(page: Page): UserAnswers => Call = page match {
    case AgentReferencePage => _ => sh03AgentRoutes.HowToNotifyAboutShareBuybackController.onPageLoad()
    case CompanyDetailsPage => _ => sh03AgentSingleRoutes.AgentReferenceController.onPageLoad(NormalMode)
    case ReasonForPurchasePage => _ => sh03AgentSingleRoutes.CompanyDetailsController.onPageLoad(NormalMode)
    case TreasurySharesPage => _ => sh03AgentSingleRoutes.ReasonForPurchaseController.onPageLoad(NormalMode)
    case ConnectedPersonsPage => userAnswers => dataDependent(ReasonForPurchasePage, userAnswers) {
      case ReasonForPurchase.ForCancellation  => sh03AgentSingleRoutes.TreasurySharesController.onPageLoad(NormalMode)
      case ReasonForPurchase.ToPlaceIntoTreasury => sh03AgentSingleRoutes.ReasonForPurchaseController.onPageLoad(NormalMode)
    }
    case ApplyingForReliefPage => _ => sh03AgentSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode)
    case DetailsOfThisSharePurchasePage => _ => sh03AgentSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
    case MinimumAmountPaidPage => _ => routes.JourneyRecoveryController.onPageLoad()
    case ChargingPointPage => _ => sh03AgentSingleRoutes.MinimumAmountPaidController.onPageLoad(NormalMode)
    case MaximumAmountPaidPage => _ => routes.JourneyRecoveryController.onPageLoad()
    case _ => _ => defaultPage
  }