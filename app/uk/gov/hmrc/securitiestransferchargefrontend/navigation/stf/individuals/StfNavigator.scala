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

import com.google.common.collect.{BiMap, HashBiMap}
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
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.{AbstractModeNavigator, PersistentNavigator, UserAnswersValidator}
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

    override protected val startPage: GettablePage[?] = ConfirmAddressPage

    override protected def pageCallMap(mode: Mode): BiMap[GettablePage[?], Call] = {
      val map = HashBiMap.create[GettablePage[?], Call]()
      
      // STF Individual single journey pages only
      map.put(ConfirmAddressPage, individualSingleRoutes.ConfirmAddressController.onPageLoad())
      map.put(StfBuyersAddressPage, individualSingleRoutes.AddressController.onPageLoad())
      map.put(NameOfSellerPage, individualSingleRoutes.NameOfSellerController.onPageLoad(mode))
      map.put(StfSellerAddressPage, individualSingleRoutes.StfSellerAddressController.onPageLoad())
      map.put(ConnectedPersonsPage, individualSingleRoutes.ConnectedPersonsController.onPageLoad(mode))
      map.put(ApplyingForReliefPage, individualSingleRoutes.ApplyingForReliefController.onPageLoad(mode))
      map.put(WhatReliefAreYouApplyingForPage, individualSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad(mode))
      map.put(SecuritiesTargetPage, individualSingleRoutes.SecuritiesTargetController.onPageLoad(mode))
      map.put(ChargingPointPage, individualSingleRoutes.ChargingPointController.onPageLoad(mode))
      map.put(TaxRatePage, individualSingleRoutes.TaxRateController.onPageLoad(mode))
      map.put(PurchasingSharesPage, individualSingleRoutes.PurchasingSharesController.onPageLoad(mode))
      map.put(DetailsOfThisTransferPage, individualSingleRoutes.DetailsOfThisTransferController.onPageLoad(mode))
      map.put(OtherSecuritiesTypePage, individualSingleRoutes.OtherSecuritiesTypeController.onPageLoad(mode))
      map.put(AmountPaidForSecuritiesPage, individualSingleRoutes.AmountPaidForSecuritiesController.onPageLoad(mode))
      map.put(TotalMarketValuePage, individualSingleRoutes.TotalMarketValueController.onPageLoad(mode))
      map.put(CheckYourAnswersPage, stfSingleCyaRoutes.CheckYourAnswersController.onPageLoad())

      map
    }

    override protected def pageHasValidDataAtPath(userAnswers: UserAnswers, page: GettablePage[_]): Boolean = page match {
      case DetailsOfThisTransferPage if userAnswers.get(ConnectedPersonsPage).contains(true) =>
        userAnswers.get(DetailsOfThisTransferPage).exists(_.marketValue.isDefined)
      case SecuritiesTargetPage => // CRN is optional
        userAnswers.get(SecuritiesTargetPage).exists(_.businessName.nonEmpty)
      case _ => super.pageHasValidDataAtPath(userAnswers, page)
    }
  }
}
