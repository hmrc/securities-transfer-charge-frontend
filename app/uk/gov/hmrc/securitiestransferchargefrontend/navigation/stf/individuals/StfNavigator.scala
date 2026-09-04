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

package uk.gov.hmrc.securitiestransferchargefrontend.navigation.stf.individuals

import play.api.mvc.{Call, Request}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.single.routes as individualSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.single.routes as stfSingleCyaRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes as sharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{SubmissionId, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, Mode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.Page
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.*
import uk.gov.hmrc.securitiestransferchargefrontend.services.AnswerPersistenceService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

  
class StfNavigator @Inject()(appConfig: FrontendAppConfig,
                             answerPersistenceService: AnswerPersistenceService)
                            (implicit ec: ExecutionContext) extends AbstractModeNavigator with PersistentNavigator {

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
    case _ => routes.JourneyRecoveryController.onPageLoad()
  }

  private def checkYourAnswersRoute: Call =
    stfSingleCyaRoutes.CheckYourAnswersController.onPageLoad()

  private def routeForOtherSecurities(userAnswers: UserAnswers): Call = {
    if (userAnswers.get(TotalMarketValuePage).isDefined) {
      checkYourAnswersRoute
    } else {
      individualSingleRoutes.TotalMarketValueController.onPageLoad(CheckMode)
    }
  }

  private def routeForShares(userAnswers: UserAnswers): Call = {
    val hasMarketValue = userAnswers.get(DetailsOfThisTransferPage)
      .exists(_.marketValue.isDefined)

    if (hasMarketValue) {
      checkYourAnswersRoute
    } else {
      individualSingleRoutes.DetailsOfThisTransferController.onPageLoad(CheckMode)
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
            case Some(true)  => routeForShares(userAnswers)          // Shares
            case None        => checkYourAnswersRoute
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

    override protected val startPage: GettablePage[?] = NameOfSellerPage

    override protected lazy val pageCallMap: PageCallBiMap =
      PageCallBiMapBuilder()
        .addMappingNoCheck(ConfirmAddressPage, individualSingleRoutes.ConfirmAddressController.onPageLoad)
        .addMappingNoCheck(StfBuyersAddressPage, individualSingleRoutes.AddressController.onPageLoad)
        .addMapping(NameOfSellerPage, individualSingleRoutes.NameOfSellerController.onPageLoad)
        .addMapping(StfSellerAddressPage, individualSingleRoutes.StfSellerAddressController.onPageLoad)
        .addMapping(ConnectedPersonsPage, individualSingleRoutes.ConnectedPersonsController.onPageLoad)
        .addMapping(ApplyingForReliefPage, individualSingleRoutes.ApplyingForReliefController.onPageLoad)
        .addMapping(WhatReliefAreYouApplyingForPage, individualSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad)
        .addMapping(SecuritiesTargetPage, individualSingleRoutes.SecuritiesTargetController.onPageLoad)
        .addMapping(ChargingPointPage, individualSingleRoutes.ChargingPointController.onPageLoad)
        .addMapping(TaxRatePage, individualSingleRoutes.TaxRateController.onPageLoad)
        .addMapping(PurchasingSharesPage, individualSingleRoutes.PurchasingSharesController.onPageLoad)
        .addMapping(DetailsOfThisTransferPage, individualSingleRoutes.DetailsOfThisTransferController.onPageLoad)
        .addMapping(OtherSecuritiesTypePage, individualSingleRoutes.OtherSecuritiesTypeController.onPageLoad)
        .addMapping(AmountPaidForSecuritiesPage, individualSingleRoutes.AmountPaidForSecuritiesController.onPageLoad)
        .addMapping(TotalMarketValuePage, individualSingleRoutes.TotalMarketValueController.onPageLoad)
        .addMappingNoCheck(CheckYourAnswersPage, stfSingleCyaRoutes.CheckYourAnswersController.onPageLoad)
        .build

    override protected def pageHasValidDataAtPath(userAnswers: UserAnswers, page: GettablePage[_]): Boolean = page match {
      case DetailsOfThisTransferPage if userAnswers.get(ConnectedPersonsPage).contains(true) =>
        userAnswers.get(DetailsOfThisTransferPage).exists(_.marketValue.isDefined)
      case SecuritiesTargetPage => // CRN is optional
        userAnswers.get(SecuritiesTargetPage).exists(_.businessName.nonEmpty)
      case _ => super.pageHasValidDataAtPath(userAnswers, page)
    }
  }
}
