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
import play.api.mvc.{Call, Request}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.agents.single.routes as agentSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes as sharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{SubmissionId, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, Mode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.stf.agents
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.stf.agents.{BackwardsRoutes, ForwardRoutes}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.*
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
    // TODO: The CYA error page needs updating to be the submission failure page once it is created.
    case CheckYourAnswersPage => routes.JourneyRecoveryController.onPageLoad()
    case _ => defaultPage
  }

  private def checkYourAnswersRoute: Call =
    agentSingleRoutes.CheckYourAnswersController.onPageLoad()

  private def routeForOtherSecurities(userAnswers: UserAnswers): Call = {
    if (userAnswers.get(TotalMarketValuePage).isDefined) {
      checkYourAnswersRoute
    } else {
      agentSingleRoutes.TotalMarketValueController.onPageLoad(CheckMode)
    }
  }

  private def routeForShares(userAnswers: UserAnswers): Call = {
    val hasMarketValue = userAnswers.get(DetailsOfThisTransferPage).exists(_.marketValue.isDefined)
    if (hasMarketValue) {
      checkYourAnswersRoute
    } else {
      agentSingleRoutes.DetailsOfThisTransferController.onPageLoad(CheckMode)
    }
  }

  val checkRouteMap: Page => UserAnswers => Call = page => userAnswers => {
    page match {
      case ConnectedPersonsPage =>
        val isConnectedPerson = userAnswers.get(ConnectedPersonsPage).getOrElse(false)

        if (!isConnectedPerson) {
          checkYourAnswersRoute
        } else {
          userAnswers.get(PurchasingSharesPage) match {
            case Some(false) => routeForOtherSecurities(userAnswers) // Other securities
            case Some(true) => routeForShares(userAnswers) // Shares
            case None => checkYourAnswersRoute
          }
        }

      case _ => checkYourAnswersRoute
    }
  }
  def restore(submissionId: SubmissionId, userId: UserId)(implicit request: Request[?]): Future[UserAnswers] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    answerPersistenceService.load(submissionId, userId)
  }

  val userAnswersValidator: UserAnswersValidator = new UserAnswersValidator(this) {

    override protected val startPage: GettablePage[?] = AgentReferencePage

    override protected lazy val pageCallMap: PageCallBiMap =
      PageCallBiMapBuilder()
        .addMapping(AgentReferencePage, agentSingleRoutes.AgentReferenceController.onPageLoad)
        .addMapping(NameOfBuyerPage, agentSingleRoutes.NameOfBuyerController.onPageLoad)
        .addMapping(StfBuyersAddressPage, agentSingleRoutes.AddressController.onPageLoad)
        .addMapping(NameOfSellerPage, agentSingleRoutes.NameOfSellerController.onPageLoad)
        .addMapping(StfSellerAddressPage, agentSingleRoutes.StfSellerAddressController.onPageLoad)
        .addMapping(ConnectedPersonsPage, agentSingleRoutes.ConnectedPersonsController.onPageLoad)
        .addMapping(ApplyingForReliefPage, agentSingleRoutes.ApplyingForReliefController.onPageLoad)
        .addMapping(WhatReliefAreYouApplyingForPage, agentSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad)
        .addMapping(SecuritiesTargetPage, agentSingleRoutes.SecuritiesTargetController.onPageLoad)
        .addMapping(ChargingPointPage, agentSingleRoutes.ChargingPointController.onPageLoad)
        .addMapping(TaxRatePage, agentSingleRoutes.TaxRateController.onPageLoad)
        .addMapping(PurchasingSharesPage, agentSingleRoutes.PurchasingSharesController.onPageLoad)
        .addMapping(DetailsOfThisTransferPage, agentSingleRoutes.DetailsOfThisTransferController.onPageLoad)
        .addMapping(OtherSecuritiesTypePage, agentSingleRoutes.OtherSecuritiesTypeController.onPageLoad)
        .addMapping(AmountPaidForSecuritiesPage, agentSingleRoutes.AmountPaidForSecuritiesController.onPageLoad)
        .addMapping(TotalMarketValuePage, agentSingleRoutes.TotalMarketValueController.onPageLoad)
        .addMappingNoCheck(CheckYourAnswersPage, agentSingleRoutes.CheckYourAnswersController.onPageLoad)
        .build

    override protected def pageHasValidDataAtPath(userAnswers: UserAnswers, page: GettablePage[_]): Boolean = page match {
      case AgentReferencePage => true // Optional data, so always valid
      case DetailsOfThisTransferPage if userAnswers.get(ConnectedPersonsPage).contains(true) =>
        userAnswers.get(DetailsOfThisTransferPage).exists(_.marketValue.isDefined)
      case SecuritiesTargetPage => // CRN is optional
        userAnswers.get(SecuritiesTargetPage).exists(_.businessName.nonEmpty)
      case _ => super.pageHasValidDataAtPath(userAnswers, page)

    }
  }
