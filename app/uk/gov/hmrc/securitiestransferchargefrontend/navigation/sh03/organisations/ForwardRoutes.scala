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

import play.api.mvc.Call
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.organisations.single.routes as sh03OrgSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.HowToNotifyAboutShareBuyback.{MoreThanOneAtATime, OneAtATime}
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.{ReasonForPurchase, RoleAtPurchasingCompany}
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.PersistentNavigationHelper
import uk.gov.hmrc.securitiestransferchargefrontend.pages.Page
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.*
import uk.gov.hmrc.securitiestransferchargefrontend.services.AnswerPersistenceService

import scala.concurrent.{ExecutionContext, Future}

class ForwardRoutes(answerPersistenceService: AnswerPersistenceService,
                    defaultPage: Call,
                    errorPages: Seq[Call],
                    appConfig: FrontendAppConfig)
                   (implicit ec: ExecutionContext):

  val helper = new PersistentNavigationHelper(answerPersistenceService, defaultPage, errorPages)

  import helper.*

  private val firstDate = appConfig.firstChargingPoint

  def forwardRoutes(page: Page)(implicit hc: HeaderCarrier): UserAnswers => Future[Call] = page match {

    case HowToNotifyAboutShareBuybackPage => userAnswers => {
      dataDependent(HowToNotifyAboutShareBuybackPage, userAnswers) {
        case OneAtATime => sh03OrgSingleRoutes.CompanyDetailsController.onPageLoad(NormalMode)
        case MoreThanOneAtATime => defaultPage
      }
    }

    case CompanyDetailsPage => userAnswers =>
      dataRequired(CompanyDetailsPage, userAnswers, sh03OrgSingleRoutes.ReasonForPurchaseController.onPageLoad(NormalMode))

    case ReasonForPurchasePage => userAnswers =>
      dataDependent(ReasonForPurchasePage, userAnswers) {
        case ReasonForPurchase.ForCancellation => sh03OrgSingleRoutes.TreasurySharesController.onPageLoad(NormalMode)
        case ReasonForPurchase.ToPlaceIntoTreasury => sh03OrgSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode)
      }
    case TreasurySharesPage => userAnswers =>
      dataRequired(TreasurySharesPage, userAnswers, sh03OrgSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode))

    case ConnectedPersonsPage => userAnswers =>
      dataRequired(ConnectedPersonsPage, userAnswers, sh03OrgSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode))

    case ApplyingForReliefPage => userAnswers =>
      dataDependent(ApplyingForReliefPage, userAnswers) {
        case false => sh03OrgSingleRoutes.DetailsOfThisSharePurchaseController.onPageLoad(NormalMode)
        case true => sh03OrgSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode)
      }
    case WhatReliefAreYouApplyingForPage => userAnswers =>
      dataRequired(WhatReliefAreYouApplyingForPage, userAnswers, sh03OrgSingleRoutes.DetailsOfThisSharePurchaseController.onPageLoad(NormalMode))

    case DetailsOfThisSharePurchasePage => userAnswers =>
      dataDependent(CompanyDetailsPage, userAnswers) { companyDetails =>
        if (companyDetails.isPlc)
          sh03OrgSingleRoutes.MaximumAmountPaidController.onPageLoad(NormalMode)
        else
          sh03OrgSingleRoutes.ChargingPointController.onPageLoad(NormalMode)
      }

    case MaximumAmountPaidPage => userAnswers =>
      dataRequired(MaximumAmountPaidPage, userAnswers, sh03OrgSingleRoutes.MinimumAmountPaidController.onPageLoad(NormalMode))

    case MinimumAmountPaidPage => userAnswers =>
      dataRequired(MinimumAmountPaidPage, userAnswers, sh03OrgSingleRoutes.ChargingPointController.onPageLoad(NormalMode))

    case ChargingPointPage => userAnswers =>
      dataDependent(ChargingPointPage, userAnswers) { enterDate =>
        if (enterDate.isBefore(firstDate)) defaultPage
        else sh03OrgSingleRoutes.RoleAtPurchasingCompanyController.onPageLoad(NormalMode)
      }

    case RoleAtPurchasingCompanyPage => userAnswers =>
      dataDependent(RoleAtPurchasingCompanyPage, userAnswers) {
        roleAtPurchasingCompany =>
          if (roleAtPurchasingCompany.role == RoleAtPurchasingCompany.unsupportedRole)
            sh03OrgSingleRoutes.CannotSubmitFormErrorController.onPageLoad()
          else
            sh03OrgSingleRoutes.CheckYourAnswersController.onPageLoad()
      }
    case _ => _ => Future.successful(defaultPage)
  }
