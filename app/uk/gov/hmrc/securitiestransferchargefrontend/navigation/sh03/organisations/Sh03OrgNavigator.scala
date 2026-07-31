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

package uk.gov.hmrc.securitiestransferchargefrontend.navigation.sh03.organisations

import com.google.inject.Singleton
import play.api.mvc.Call
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.organisations.routes as sh03OrgRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.organisations.single.routes as sh03OrgSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.shared.routes as sh03SharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.shared.single.routes as sh03SingleCyaRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes as stfSharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{SubmissionId, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, Mode, NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.{AbstractModeNavigator, PersistentNavigator}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.bulk.CannotSubmitFormErrorPage as Sh03CannotSubmitFormErrorPage
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.SubmissionsDashboardPage
import uk.gov.hmrc.securitiestransferchargefrontend.pages.{CheckYourAnswersPage, JourneyRecoveryPage, Page}
import uk.gov.hmrc.securitiestransferchargefrontend.services.AnswerPersistenceService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Sh03OrgNavigator @Inject()(
                                  override val answerPersistenceService: AnswerPersistenceService,
                                  appConfig: FrontendAppConfig
                                )(implicit ec: ExecutionContext) extends AbstractModeNavigator with PersistentNavigator {

  override lazy val dashboardPage: Call = stfSharedRoutes.SubmissionsDashboardController.onPageLoad()
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
    case BeforeYouStartPage => sh03SharedRoutes.BeforeYouStartController.onPageLoad()
    case HowToNotifyAboutShareBuybackPage => sh03OrgRoutes.HowToNotifyAboutShareBuybackController.onPageLoad()
    case CompanyDetailsPage => sh03OrgSingleRoutes.CompanyDetailsController.onPageLoad(NormalMode)
    case ReasonForPurchasePage => sh03OrgSingleRoutes.ReasonForPurchaseController.onPageLoad(NormalMode)
    case TreasurySharesPage => sh03OrgSingleRoutes.TreasurySharesController.onPageLoad(NormalMode)
    case ConnectedPersonsPage => sh03OrgSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode)
    case ApplyingForReliefPage => sh03OrgSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
    case WhatReliefAreYouApplyingForPage => sh03OrgSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode)
    case DetailsOfThisSharePurchasePage => sh03OrgSingleRoutes.DetailsOfThisSharePurchaseController.onPageLoad(NormalMode)
    case MaximumAmountPaidPage => sh03OrgSingleRoutes.MaximumAmountPaidController.onPageLoad(NormalMode)
    case MinimumAmountPaidPage => sh03OrgSingleRoutes.MinimumAmountPaidController.onPageLoad(NormalMode)
    case ChargingPointPage => sh03OrgSingleRoutes.ChargingPointController.onPageLoad(NormalMode)
    case RoleAtPurchasingCompanyPage => sh03OrgSingleRoutes.RoleAtPurchasingCompanyController.onPageLoad(NormalMode)
    case SubmissionsDashboardPage => dashboardPage
    case Sh03CannotSubmitFormErrorPage => sh03OrgSingleRoutes.CannotSubmitFormErrorController.onPageLoad()
    case CheckYourAnswersPage => sh03SingleCyaRoutes.CheckYourAnswersController.onPageLoad()
    case JourneyRecoveryPage => routes.JourneyRecoveryController.onPageLoad()
    case _ => routes.JourneyRecoveryController.onPageLoad()
  }

  override def restore(submissionId: SubmissionId, userId: UserId)(implicit hc: HeaderCarrier): Future[UserAnswers] = {
    answerPersistenceService.load(submissionId, userId)
  }
}