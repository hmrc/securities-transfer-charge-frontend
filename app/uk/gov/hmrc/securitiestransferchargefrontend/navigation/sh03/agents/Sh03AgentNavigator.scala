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

import com.google.inject.Singleton
import play.api.mvc.Call
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.routes as sh03AgentRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.single.routes as sh03AgentSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.bulk.routes as sh03AgentBulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.shared.single.routes as sh03SingleCyaRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes as sharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.SubmissionsDashboardPage
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.BeforeYouStartPage
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{SubmissionId, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, Mode, NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.sh03.agents
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.{AbstractModeNavigator, PersistentNavigator}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.{CheckYourAnswersPage, *}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.bulk.*
import uk.gov.hmrc.securitiestransferchargefrontend.services.AnswerPersistenceService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Sh03AgentNavigator @Inject()(override val answerPersistenceService: AnswerPersistenceService, appConfig: FrontendAppConfig)
                                  (implicit ec: ExecutionContext) extends AbstractModeNavigator with PersistentNavigator:

  override lazy val dashboardPage: Call = sharedRoutes.SubmissionsDashboardController.onPageLoad()
  val defaultPage: Call = routes.JourneyRecoveryController.onPageLoad()
  val errorPages: List[Call] = List(defaultPage)

  val forwardRoutesHelper: ForwardRoutes = new ForwardRoutes(answerPersistenceService, defaultPage, errorPages, appConfig)
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
    case BeforeYouStartPage => uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.shared.routes.BeforeYouStartController.onPageLoad()
    case HowToNotifyAboutShareBuybackPage => sh03AgentRoutes.HowToNotifyAboutShareBuybackController.onPageLoad()
    case AgentReferencePage => sh03AgentSingleRoutes.AgentReferenceController.onPageLoad(NormalMode)
    case CompanyDetailsPage => sh03AgentSingleRoutes.CompanyDetailsController.onPageLoad(NormalMode)
    case ReasonForPurchasePage => sh03AgentSingleRoutes.ReasonForPurchaseController.onPageLoad(NormalMode)
    case TreasurySharesPage => sh03AgentSingleRoutes.TreasurySharesController.onPageLoad(NormalMode)
    case ConnectedPersonsPage => sh03AgentSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode)
    case ApplyingForReliefPage => sh03AgentSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
    case WhatReliefAreYouApplyingForPage => sh03AgentSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode)
    case DetailsOfThisSharePurchasePage => sh03AgentSingleRoutes.DetailsOfThisSharePurchaseController.onPageLoad(NormalMode)
    case MaximumAmountPaidPage => sh03AgentSingleRoutes.MaximumAmountPaidController.onPageLoad(NormalMode)
    case MinimumAmountPaidPage => sh03AgentSingleRoutes.MinimumAmountPaidController.onPageLoad(NormalMode)
    case ChargingPointPage => sh03AgentSingleRoutes.ChargingPointController.onPageLoad(NormalMode)
    case RoleAtPurchasingCompanyPage => sh03AgentSingleRoutes.RoleAtPurchasingCompanyController.onPageLoad(NormalMode)
    case BulkAgentReferencePage => sh03AgentBulkRoutes.AgentReferenceController.onPageLoad(NormalMode)
    case BulkCompanyDetailsPage => sh03AgentBulkRoutes.CompanyDetailsController.onPageLoad(NormalMode)
    case BulkRoleAtPurchasingCompanyPage => sh03AgentBulkRoutes.RoleAtPurchasingCompanyController.onPageLoad(NormalMode)
    case CannotSubmitFormErrorPage => sh03AgentSingleRoutes.CannotSubmitFormErrorController.onPageLoad()
    case CheckYourAnswersPage => sh03SingleCyaRoutes.CheckYourAnswersController.onPageLoad()
    case JourneyRecoveryPage => routes.JourneyRecoveryController.onPageLoad()
    case _ => sh03SingleCyaRoutes.CheckYourAnswersController.onPageLoad()
  }

  override def restore(submissionId: SubmissionId, userId: UserId)(implicit hc: HeaderCarrier): Future[UserAnswers] = {
    answerPersistenceService.load(submissionId, userId)
  }
