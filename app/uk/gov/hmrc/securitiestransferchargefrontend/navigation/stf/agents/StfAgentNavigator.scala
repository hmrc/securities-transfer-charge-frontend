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

import com.google.common.collect.{BiMap, HashBiMap}
import com.google.inject.Singleton
import play.api.mvc.{Call, Request}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.agents.single.routes as agentSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes as sharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.single.routes as stfSingleCyaRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{SubmissionId, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Mode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.stf.agents
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.stf.agents.{BackwardsRoutes, ForwardRoutes}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.{AbstractModeNavigator, PersistentNavigator, UserAnswersValidator}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.*
import uk.gov.hmrc.securitiestransferchargefrontend.services.AnswerPersistenceService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StfAgentNavigator @Inject()(appConfig: FrontendAppConfig,
                                  answerPersistenceService: AnswerPersistenceService)
                               (implicit ec: ExecutionContext) extends AbstractModeNavigator with PersistentNavigator:

  override lazy val dashboardPage: Call = sharedRoutes.SubmissionsDashboardController.onPageLoad()
  val defaultPage: Call = routes.JourneyRecoveryController.onPageLoad()
  val errorPages: List[Call] = List(defaultPage)

  val forwardRoutes: ForwardRoutes = new ForwardRoutes(answerPersistenceService, appConfig, defaultPage, errorPages)
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

    override protected val startPage: GettablePage[?] = AgentReferencePage

    override protected def pageCallMap(mode: Mode): BiMap[GettablePage[?], Call] = {
      val map = HashBiMap.create[GettablePage[?], Call]()
      
      // STF Agent single journey pages only
      map.put(AgentReferencePage, agentSingleRoutes.AgentReferenceController.onPageLoad(mode))
      map.put(NameOfBuyerPage, agentSingleRoutes.NameOfBuyerController.onPageLoad(mode))
      map.put(StfBuyersAddressPage, agentSingleRoutes.AddressController.onPageLoad())
      map.put(NameOfSellerPage, agentSingleRoutes.NameOfSellerController.onPageLoad(mode))
      map.put(StfSellerAddressPage, agentSingleRoutes.StfSellerAddressController.onPageLoad())
      map.put(ConnectedPersonsPage, agentSingleRoutes.ConnectedPersonsController.onPageLoad(mode))
      map.put(ApplyingForReliefPage, agentSingleRoutes.ApplyingForReliefController.onPageLoad(mode))
      map.put(WhatReliefAreYouApplyingForPage, agentSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad(mode))
      map.put(SecuritiesTargetPage, agentSingleRoutes.SecuritiesTargetController.onPageLoad(mode))
      map.put(ChargingPointPage, agentSingleRoutes.ChargingPointController.onPageLoad(mode))
      map.put(TaxRatePage, agentSingleRoutes.TaxRateController.onPageLoad(mode))
      map.put(PurchasingSharesPage, agentSingleRoutes.PurchasingSharesController.onPageLoad(mode))
      map.put(DetailsOfThisTransferPage, agentSingleRoutes.DetailsOfThisTransferController.onPageLoad(mode))
      map.put(OtherSecuritiesTypePage, agentSingleRoutes.OtherSecuritiesTypeController.onPageLoad(mode))
      map.put(AmountPaidForSecuritiesPage, agentSingleRoutes.AmountPaidForSecuritiesController.onPageLoad(mode))
      map.put(TotalMarketValuePage, agentSingleRoutes.TotalMarketValueController.onPageLoad(mode))
      map.put(CheckYourAnswersPage, stfSingleCyaRoutes.CheckYourAnswersController.onPageLoad())
      
      map
    }

    override protected def pageHasValidDataAtPath(userAnswers: UserAnswers, page: GettablePage[_]): Boolean = page match {
      case AgentReferencePage => true // Optional data, so always valid
      case DetailsOfThisTransferPage if userAnswers.get(ConnectedPersonsPage).contains(true) =>
        userAnswers.get(DetailsOfThisTransferPage).exists(_.marketValue.isDefined)
      case SecuritiesTargetPage => // CRN is optional
        userAnswers.get(SecuritiesTargetPage).exists(_.businessName.nonEmpty)
      case _ => super.pageHasValidDataAtPath(userAnswers, page)

    }
  }
