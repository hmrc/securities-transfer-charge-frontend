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
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.NavigationHelper
import uk.gov.hmrc.securitiestransferchargefrontend.pages.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.HowToNotifyAboutSecuritiesTransferPage
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.{AgentReferencePage, NameOfSellerPage, StfBuyersAddressPage}

class BackwardsRoutes(defaultPage: Call):

  val navHelper: NavigationHelper = new NavigationHelper(defaultPage)

  def predecessorRoutes(page: Page): UserAnswers => Call = page match {

    case HowToNotifyAboutSecuritiesTransferPage => _ => sharedRoutes.SubmissionsDashboardController.onPageLoad()
    case AgentReferencePage => _ => agentRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad(NormalMode)
    case StfBuyersAddressPage => _ => agentSingleRoutes.AgentReferenceController.onPageLoad(NormalMode)
    case NameOfSellerPage => _ => agentSingleRoutes.AddressController.onPageLoad()
    case _ => _ => defaultPage

  }
