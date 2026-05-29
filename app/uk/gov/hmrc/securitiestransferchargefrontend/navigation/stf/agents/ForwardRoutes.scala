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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.agents.routes as agentRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.agents.single.routes as agentSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.agents.bulk.routes as agentBulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.HowToNotifyAboutSecuritiesTransfer.{MoreThanOneAtATime, OneAtATime}
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.PersistentNavigationHelper
import uk.gov.hmrc.securitiestransferchargefrontend.pages.Page
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.{HowToNotifyAboutSecuritiesTransferPage, SubmissionsDashboardPage}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.*
import uk.gov.hmrc.securitiestransferchargefrontend.services.AnswerPersistenceService
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.WhatTypeOfSecurities
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes


import scala.concurrent.{ExecutionContext, Future}

class ForwardRoutes(answerPersistenceService: AnswerPersistenceService,
                    appConfig: FrontendAppConfig,
                    defaultPage: Call,
                    errorPages: Seq[Call])
                   (implicit ec: ExecutionContext):

  val helper = new PersistentNavigationHelper(answerPersistenceService, defaultPage, errorPages)

  import helper.*

  private val firstDate = appConfig.firstChargingPoint

  def forwardRoutes(page: Page)(implicit hc: HeaderCarrier): UserAnswers => Future[Call] = page match {

    case SubmissionsDashboardPage => userAnswers => goTo(agentRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad(NormalMode), Some(userAnswers))
    case HowToNotifyAboutSecuritiesTransferPage => userAnswers => {
      dataDependent(HowToNotifyAboutSecuritiesTransferPage, userAnswers) {
        case OneAtATime => agentSingleRoutes.AgentReferenceController.onPageLoad(NormalMode)
        case MoreThanOneAtATime => agentBulkRoutes.TemplateInstructionsController.onPageLoad()
      }
    }
    case AgentReferencePage => userAnswers => dataRequired(AgentReferencePage, userAnswers, agentSingleRoutes.NameOfBuyerController.onPageLoad(NormalMode))
    case NameOfBuyerPage => userAnswers => dataRequired(NameOfBuyerPage, userAnswers, agentSingleRoutes.AddressController.onPageLoad())
    case StfBuyersAddressPage => userAnswers => dataRequired(StfBuyersAddressPage, userAnswers, agentSingleRoutes.NameOfSellerController.onPageLoad(NormalMode))
    case NameOfSellerPage => userAnswers => dataRequired(NameOfSellerPage, userAnswers, agentSingleRoutes.StfSellerAddressController.onPageLoad())
    case StfSellerAddressPage => userAnswers => dataRequired(StfSellerAddressPage, userAnswers, agentSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode))
    case ConnectedPersonsPage => userAnswers => dataRequired(ConnectedPersonsPage, userAnswers, agentSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode))
    case ApplyingForReliefPage => userAnswers => dataDependent(ApplyingForReliefPage, userAnswers) {
      case true => agentSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode)
      case false => agentSingleRoutes.SecuritiesTargetController.onPageLoad(NormalMode)
    }
    case WhatReliefAreYouApplyingForPage => userAnswers => dataRequired(WhatReliefAreYouApplyingForPage, userAnswers, agentSingleRoutes.SecuritiesTargetController.onPageLoad(NormalMode))
    case SecuritiesTargetPage => userAnswers => dataRequired(SecuritiesTargetPage, userAnswers, agentSingleRoutes.ChargingPointController.onPageLoad(NormalMode))
    case ChargingPointPage => userAnswers => dataDependent(ChargingPointPage, userAnswers) {enterDate =>
      if (enterDate.isBefore(firstDate)) defaultPage
      else agentSingleRoutes.TaxRateController.onPageLoad(NormalMode)
    }
    case TaxRatePage => userAnswers => dataRequired(TaxRatePage, userAnswers, agentSingleRoutes.WhatTypeOfSecuritiesController.onPageLoad(NormalMode))
    case OtherSecuritiesTypePage => userAnswers => dataRequired(OtherSecuritiesTypePage, userAnswers, agentSingleRoutes.AmountPaidForSecuritiesController.onPageLoad(NormalMode))
    case AmountPaidForSecuritiesPage => userAnswers =>
      dataDependent(ConnectedPersonsPage, userAnswers) { isConnected =>
        if (isConnected)
          agentSingleRoutes.TotalMarketValueController.onPageLoad(NormalMode)
        else
          routes.CheckYourAnswersController.onPageLoad()
      }
    case TotalMarketValuePage => userAnswers => dataRequired(TotalMarketValuePage, userAnswers, routes.CheckYourAnswersController.onPageLoad())
    case WhatTypeOfSecuritiesPage => userAnswers =>
      dataDependent(WhatTypeOfSecuritiesPage, userAnswers) {
        case WhatTypeOfSecurities.Shares => agentSingleRoutes.DetailsOfThisTransferController.onPageLoad(NormalMode)
        case WhatTypeOfSecurities.Other => agentSingleRoutes.OtherSecuritiesTypeController.onPageLoad(NormalMode)
      }
    case DetailsOfThisTransferPage => userAnswers => dataRequired(DetailsOfThisTransferPage, userAnswers, routes.CheckYourAnswersController.onPageLoad())  

    case _ => _ => Future.successful(defaultPage)
  }
