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
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.{ConfirmAddressPage, ConnectedPersonsPage, NameOfSellerPage, Page, StfBuyersAddressPage}
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations.routes as orgRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.pages.*
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.NavigationHelper

class BackwardsRoutes(defaultPage: Call):

  val navHelper: NavigationHelper = new NavigationHelper(defaultPage)
  import navHelper.*

  def predecessorRoutes(page: Page): UserAnswers => Call = page match {

    case HowToNotifyAboutSecuritiesTransferPage => _ => routes.SubmissionsDashboardController.onPageLoad()
    case ConfirmAddressPage => _ => orgRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad(NormalMode)
    case StfBuyersAddressPage => _ =>orgRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad(NormalMode)
    case NameOfSellerPage => _ => orgRoutes.ConfirmAddressController.onPageLoad()
    case ConnectedPersonsPage => _ => orgRoutes.StfSellerAddressController.onPageLoad()
    case ApplyingForReliefPage  => _ => orgRoutes.ConnectedPersonsController.onPageLoad(NormalMode)
    case SecuritiesTargetPage => userAnswers => dataDependent(ApplyingForReliefPage, userAnswers) {
      case true => routes.SubmissionsDashboardController.onPageLoad()
      case false => orgRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
    }
    case TaxRatePage => _ => routes.SubmissionsDashboardController.onPageLoad()

    case _ => _ => defaultPage

  }
