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
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.agents.bulk.routes as agentBulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes as sharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.fileUpload.routes as bulkSharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.HowToNotifyAboutSecuritiesTransfer.{MoreThanOneAtATime, OneAtATime}
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, JourneyType, Mode, NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.NavigationHelper
import uk.gov.hmrc.securitiestransferchargefrontend.pages.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.bulk.BulkCheckYourAnswersPage
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.{AgentReferencePage, CheckYourAnswersPage, HowToNotifyAboutSecuritiesTransferPage}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.*

class BackwardsRoutes(defaultPage: Call):

  val navHelper: NavigationHelper = new NavigationHelper(defaultPage)

  import navHelper.*

  def predecessorRoutes(page: Page, mode: Mode): Option[UserAnswers] => Call = mode match {
    case NormalMode => normalRoutes(page)
    case CheckMode => checkRoutes(page)
  }
  
  def normalRoutes(page: Page): Option[UserAnswers] => Call = page match {

    case HowToNotifyAboutSecuritiesTransferPage => _ => sharedRoutes.SubmissionsDashboardController.onPageLoad()
    case AgentReferencePage => _.fold(defaultPage) { userAnswers =>
      dataDependent(HowToNotifyAboutSecuritiesTransferPage, userAnswers) {
        case OneAtATime => agentRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad()
        case MoreThanOneAtATime => bulkSharedRoutes.FileUploadController.onPageLoad(JourneyType.STF)
      }
    }
    case NameOfBuyerPage => _ => agentSingleRoutes.AgentReferenceController.onPageLoad(NormalMode)
    case StfBuyersAddressPage => _ => agentSingleRoutes.NameOfBuyerController.onPageLoad(NormalMode)
    case NameOfSellerPage => _ => agentSingleRoutes.AddressController.onPageLoad(NormalMode)
    case StfSellerAddressPage => _ => agentSingleRoutes.NameOfSellerController.onPageLoad(NormalMode)
    case ConnectedPersonsPage => _ => agentSingleRoutes.StfSellerAddressController.onPageLoad(NormalMode)
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
    case CheckYourAnswersPage => _.fold(defaultPage) { userAnswers =>
      dataDependent(PurchasingSharesPage, userAnswers) { isPurchasingShares =>
        if (isPurchasingShares) {
          agentSingleRoutes.DetailsOfThisTransferController.onPageLoad(NormalMode)
        } else {
          dataDependent(ConnectedPersonsPage, userAnswers) { isConnectedPersons =>
            if (isConnectedPersons)
              agentSingleRoutes.TotalMarketValueController.onPageLoad(NormalMode)
            else
              agentSingleRoutes.AmountPaidForSecuritiesController.onPageLoad(NormalMode)
          }
        }
      }
    }
    case BulkCheckYourAnswersPage => _.fold(defaultPage) { userAnswers =>
      val fileUploadRef = getFileUploadRef(userAnswers)
      agentBulkRoutes.AgentReferenceController.onPageLoad(NormalMode, fileUploadRef)
    }

    case _ => _ => defaultPage
  }
  
  private def checkRoutes(page: Page): Option[UserAnswers] => Call = page match {
    case AgentReferencePage => _.fold(defaultPage) { userAnswers =>
      dataDependent(HowToNotifyAboutSecuritiesTransferPage, userAnswers) {
        case OneAtATime => agentSingleRoutes.CheckYourAnswersController.onPageLoad()
        case MoreThanOneAtATime => agentBulkRoutes.CheckYourAnswersController.onPageLoad()
      }
    }
    case _ => _ => agentSingleRoutes.CheckYourAnswersController.onPageLoad()
  }