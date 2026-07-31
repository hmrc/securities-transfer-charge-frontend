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

import com.google.inject.Singleton
import play.api.mvc.Call
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.fileUpload.routes as bulkSharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.agents.bulk.routes as agentBulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.agents.routes as agentRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.agents.single.routes as agentSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes as sharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.single.routes as stfSingleCyaRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{SubmissionId, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType.STF
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, Mode, NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.stf.agents
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.stf.agents.{BackwardsRoutes, ForwardRoutes}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.{AbstractModeNavigator, PersistentNavigator}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.{CheckYourAnswersPage, *}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.bulk.{FileUploadPage, TemplateInstructionsPage}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.{AgentReferencePage, HowToNotifyAboutSecuritiesTransferPage, SubmissionsDashboardPage}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.*
import uk.gov.hmrc.securitiestransferchargefrontend.services.AnswerPersistenceService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StfAgentNavigator @Inject()(appConfig: FrontendAppConfig,
                                  override val answerPersistenceService: AnswerPersistenceService)
                               (implicit ec: ExecutionContext) extends AbstractModeNavigator with PersistentNavigator:

  override lazy val dashboardPage: Call = sharedRoutes.SubmissionsDashboardController.onPageLoad()
  val defaultPage: Call = routes.JourneyRecoveryController.onPageLoad()
  val errorPages: List[Call] = List(defaultPage)

  val forwardRoutesHelper: ForwardRoutes = new ForwardRoutes(answerPersistenceService, appConfig, defaultPage, errorPages)
  val backwardsRoutesHelper: BackwardsRoutes = new BackwardsRoutes(defaultPage)

  override def forwardRoutes(page: Page)(implicit hc: HeaderCarrier, ec: ExecutionContext): UserAnswers => Future[Call] =
    forwardRoutesHelper.forwardRoutes(page)(hc)

  override def predecessorRoutes(page: Page): Option[UserAnswers] => Call =
    backwardsRoutesHelper.predecessorRoutes(page)

  def errorPage(forPage: Page): Call = forPage match {
    case _ => defaultPage
  }

  val checkRouteMap: Page => UserAnswers => Call = _ => _ => routes.CheckYourAnswersController.onPageLoad()

  override def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers, isReturn: Boolean): Page = {
    if (isReturn) {
      SubmissionsDashboardPage
    } else {
      mode match {
        case NormalMode => forwardRoutesHelper.forwardRoutesPage(page, userAnswers)
        case CheckMode => CheckYourAnswersPage
      }
    }
  }

  override protected def predecessorRoutesPage(page: Page, userAnswers: Option[UserAnswers]): Page =
    backwardsRoutesHelper.predecessorRoutesPage(page, userAnswers)

  override protected def pageToCall(page: Page): Call = page match {
    case SubmissionsDashboardPage => sharedRoutes.SubmissionsDashboardController.onPageLoad()
    case HowToNotifyAboutSecuritiesTransferPage => agentRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad()
    case TemplateInstructionsPage => agentBulkRoutes.TemplateInstructionsController.onPageLoad()
    case FileUploadPage => bulkSharedRoutes.FileUploadController.onPageLoad(STF)
    case AgentReferencePage => agentSingleRoutes.AgentReferenceController.onPageLoad(NormalMode)
    case NameOfBuyerPage => agentSingleRoutes.NameOfBuyerController.onPageLoad(NormalMode)
    case StfBuyersAddressPage => agentSingleRoutes.AddressController.onPageLoad()
    case NameOfSellerPage => agentSingleRoutes.NameOfSellerController.onPageLoad(NormalMode)
    case StfSellerAddressPage => agentSingleRoutes.StfSellerAddressController.onPageLoad()
    case ConnectedPersonsPage => agentSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode)
    case ApplyingForReliefPage => agentSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
    case WhatReliefAreYouApplyingForPage => agentSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode)
    case SecuritiesTargetPage => agentSingleRoutes.SecuritiesTargetController.onPageLoad(NormalMode)
    case ChargingPointPage => agentSingleRoutes.ChargingPointController.onPageLoad(NormalMode)
    case TaxRatePage => agentSingleRoutes.TaxRateController.onPageLoad(NormalMode)
    case PurchasingSharesPage => agentSingleRoutes.PurchasingSharesController.onPageLoad(NormalMode)
    case DetailsOfThisTransferPage => agentSingleRoutes.DetailsOfThisTransferController.onPageLoad(NormalMode)
    case OtherSecuritiesTypePage => agentSingleRoutes.OtherSecuritiesTypeController.onPageLoad(NormalMode)
    case AmountPaidForSecuritiesPage => agentSingleRoutes.AmountPaidForSecuritiesController.onPageLoad(NormalMode)
    case TotalMarketValuePage => agentSingleRoutes.TotalMarketValueController.onPageLoad(NormalMode)
    case CheckYourAnswersPage => stfSingleCyaRoutes.CheckYourAnswersController.onPageLoad()
    case _ => routes.JourneyRecoveryController.onPageLoad()
  }

  override def restore(submissionId: SubmissionId, userId: UserId)(implicit hc: HeaderCarrier): Future[UserAnswers] = {
    answerPersistenceService.load(submissionId, userId)
  }
