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
import play.api.mvc.{Call, Request}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.organisations.single.routes as sh03OrgSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes as sharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{SubmissionId, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Mode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.Page
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.*
import uk.gov.hmrc.securitiestransferchargefrontend.services.AnswerPersistenceService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Sh03OrgNavigator @Inject()(
                                  answerPersistenceService: AnswerPersistenceService,
                                  appConfig: FrontendAppConfig
                                )(implicit ec: ExecutionContext) extends AbstractModeNavigator with PersistentNavigator {

  override lazy val dashboardPage: Call = sharedRoutes.SubmissionsDashboardController.onPageLoad()
  val defaultPage: Call = routes.JourneyRecoveryController.onPageLoad()
  val errorPages: List[Call] = List(defaultPage)

  val forwardRoutes: ForwardRoutes = new ForwardRoutes(answerPersistenceService, defaultPage, errorPages, appConfig)
  val backwardsRoutes: BackwardsRoutes = new BackwardsRoutes(defaultPage)

  override def forwardRoutes(page: Page, mode: Mode)(implicit hc: HeaderCarrier): UserAnswers => Future[Call] =
    forwardRoutes.forwardRoutes(page, mode)(hc)

  override def predecessorRoutes(page: Page, mode: Mode): Option[UserAnswers] => Call =
    backwardsRoutes.predecessorRoutes(page, mode)

  def errorPage(forPage: Page): Call = forPage match {
    case _ => defaultPage
  }

  val checkRouteMap: Page => UserAnswers => Call = _ => _ => routes.CheckYourAnswersController.onPageLoad()

  def restore(submissionId: SubmissionId, userId: UserId)(implicit request: Request[?]): Future[UserAnswers] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    answerPersistenceService.load(submissionId, userId)
  }

  val userAnswersValidator: UserAnswersValidator = new UserAnswersValidator(this) {

    override protected val startPage: GettablePage[?] = CompanyDetailsPage

    override protected lazy val pageCallMap: PageCallBiMap =
      PageCallBiMapBuilder()
        .addMapping(CompanyDetailsPage, sh03OrgSingleRoutes.CompanyDetailsController.onPageLoad)
        .addMapping(ReasonForPurchasePage, sh03OrgSingleRoutes.ReasonForPurchaseController.onPageLoad)
        .addMapping(TreasurySharesPage, sh03OrgSingleRoutes.TreasurySharesController.onPageLoad)
        .addMapping(ConnectedPersonsPage, sh03OrgSingleRoutes.ConnectedPersonsController.onPageLoad)
        .addMapping(ApplyingForReliefPage, sh03OrgSingleRoutes.ApplyingForReliefController.onPageLoad)
        .addMapping(WhatReliefAreYouApplyingForPage, sh03OrgSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad)
        .addMapping(DetailsOfThisSharePurchasePage, sh03OrgSingleRoutes.DetailsOfThisSharePurchaseController.onPageLoad)
        .addMapping(MaximumAmountPaidPage, sh03OrgSingleRoutes.MaximumAmountPaidController.onPageLoad)
        .addMapping(MinimumAmountPaidPage, sh03OrgSingleRoutes.MinimumAmountPaidController.onPageLoad)
        .addMapping(ChargingPointPage, sh03OrgSingleRoutes.ChargingPointController.onPageLoad)
        .addMapping(RoleAtPurchasingCompanyPage, sh03OrgSingleRoutes.RoleAtPurchasingCompanyController.onPageLoad)
        .build

    override protected def pageHasValidDataAtPath(userAnswers: UserAnswers, page: GettablePage[_]): Boolean = page match {
      case DetailsOfThisSharePurchasePage if userAnswers.get(ConnectedPersonsPage).contains(true) =>
        userAnswers.get(DetailsOfThisSharePurchasePage).map(_.marketValue).isDefined
      case _ => super.pageHasValidDataAtPath(userAnswers, page)
    }

  }
}
