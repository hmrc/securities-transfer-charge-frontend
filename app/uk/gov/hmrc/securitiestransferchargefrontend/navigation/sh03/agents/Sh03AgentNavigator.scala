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

import com.google.common.collect.{BiMap, HashBiMap}
import com.google.inject.Singleton
import play.api.mvc.{Call, Request}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.single.routes as sh03AgentSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes as sharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{SubmissionId, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.sh03.agents
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.{AbstractModeNavigator, PersistentNavigator, UserAnswersValidator}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.*
import uk.gov.hmrc.securitiestransferchargefrontend.services.AnswerPersistenceService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Sh03AgentNavigator @Inject()(answerPersistenceService: AnswerPersistenceService, appConfig: FrontendAppConfig)
                                  (implicit ec: ExecutionContext) extends AbstractModeNavigator with PersistentNavigator:

  override lazy val dashboardPage: Call = sharedRoutes.SubmissionsDashboardController.onPageLoad()
  val defaultPage: Call = routes.JourneyRecoveryController.onPageLoad()
  val errorPages: List[Call] = List(defaultPage)

  val forwardRoutes: ForwardRoutes = new ForwardRoutes(answerPersistenceService, defaultPage, errorPages,appConfig)
  val backwardsRoutes: BackwardsRoutes = new BackwardsRoutes(defaultPage)

  override def forwardRoutes(page: Page)(implicit hc: HeaderCarrier): UserAnswers => Future[Call] =
    forwardRoutes.forwardRoutes(page)(hc)

  override def predecessorRoutes(page: Page): Option[UserAnswers] => Call =
    backwardsRoutes.predecessorRoutes(page)

  def errorPage(forPage: Page): Call = forPage match {
    case _ => defaultPage
  }

  val checkRouteMap: Page => UserAnswers => Call = _ => _ => routes.CheckYourAnswersController.onPageLoad()

  def restore(submissionId: SubmissionId, userId: UserId)(implicit request: Request[?]): Future[UserAnswers] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    answerPersistenceService.load(submissionId, userId)
  }

  val userAnswersValidator: UserAnswersValidator = new UserAnswersValidator(this) {

    override protected val startPage: GettablePage[?] = AgentReferencePage

    override protected val pageCallMap: BiMap[GettablePage[?], Call] = {
      val map = HashBiMap.create[GettablePage[?], Call]()
      
      // SH03 Agent single journey pages only
      map.put(AgentReferencePage, sh03AgentSingleRoutes.AgentReferenceController.onPageLoad(NormalMode))
      map.put(CompanyDetailsPage, sh03AgentSingleRoutes.CompanyDetailsController.onPageLoad(NormalMode))
      map.put(ReasonForPurchasePage, sh03AgentSingleRoutes.ReasonForPurchaseController.onPageLoad(NormalMode))
      map.put(TreasurySharesPage, sh03AgentSingleRoutes.TreasurySharesController.onPageLoad(NormalMode))
      map.put(ConnectedPersonsPage, sh03AgentSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode))
      map.put(ApplyingForReliefPage, sh03AgentSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode))
      map.put(WhatReliefAreYouApplyingForPage, sh03AgentSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode))
      map.put(DetailsOfThisSharePurchasePage, sh03AgentSingleRoutes.DetailsOfThisSharePurchaseController.onPageLoad(NormalMode))
      map.put(MaximumAmountPaidPage, sh03AgentSingleRoutes.MaximumAmountPaidController.onPageLoad(NormalMode))
      map.put(MinimumAmountPaidPage, sh03AgentSingleRoutes.MinimumAmountPaidController.onPageLoad(NormalMode))
      map.put(ChargingPointPage, sh03AgentSingleRoutes.ChargingPointController.onPageLoad(NormalMode))
      map.put(RoleAtPurchasingCompanyPage, sh03AgentSingleRoutes.RoleAtPurchasingCompanyController.onPageLoad(NormalMode))
      
      map
    }

    override protected def pageHasValidDataAtPath(userAnswers: UserAnswers, page: GettablePage[_]): Boolean = page match {
      case AgentReferencePage => true // Optional data, so always valid
      case DetailsOfThisSharePurchasePage if userAnswers.get(ConnectedPersonsPage).contains(true) =>
        userAnswers.get(DetailsOfThisSharePurchasePage).map(_.marketValue).isDefined
      case _ => super.pageHasValidDataAtPath(userAnswers, page)
    }
  }
