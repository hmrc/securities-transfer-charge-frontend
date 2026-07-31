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

package uk.gov.hmrc.securitiestransferchargefrontend.navigation.stf.organisations

import com.google.inject.Singleton
import play.api.mvc.Call
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.fileUpload.routes as bulkSharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations.bulk.routes as orgBulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations.routes as orgRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations.single.routes as orgSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes as sharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.single.routes as stfSingleCyaRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{SubmissionId, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType.STF
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, Mode, NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.stf.organisations.{BackwardsRoutes, ForwardRoutes}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.{AbstractModeNavigator, PersistentNavigator}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.bulk.{FileUploadPage, TemplateInstructionsPage}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.{CheckYourAnswersPage, *}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.{HowToNotifyAboutSecuritiesTransferPage, SubmissionsDashboardPage}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.*
import uk.gov.hmrc.securitiestransferchargefrontend.services.AnswerPersistenceService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StfOrgNavigator @Inject()(appConfig: FrontendAppConfig,
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
    case HowToNotifyAboutSecuritiesTransferPage => orgRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad()
    case TemplateInstructionsPage => orgBulkRoutes.TemplateInstructionsController.onPageLoad()
    case FileUploadPage => bulkSharedRoutes.FileUploadController.onPageLoad(STF)
    case ConfirmAddressPage => orgSingleRoutes.ConfirmAddressController.onPageLoad()
    case StfBuyersAddressPage => orgSingleRoutes.AddressController.onPageLoad()
    case NameOfSellerPage => orgSingleRoutes.NameOfSellerController.onPageLoad(NormalMode)
    case StfSellerAddressPage => orgSingleRoutes.StfSellerAddressController.onPageLoad()
    case ConnectedPersonsPage => orgSingleRoutes.ConnectedPersonsController.onPageLoad(NormalMode)
    case ApplyingForReliefPage => orgSingleRoutes.ApplyingForReliefController.onPageLoad(NormalMode)
    case WhatReliefAreYouApplyingForPage => orgSingleRoutes.WhatReliefAreYouApplyingForController.onPageLoad(NormalMode)
    case SecuritiesTargetPage => orgSingleRoutes.SecuritiesTargetController.onPageLoad(NormalMode)
    case ChargingPointPage => orgSingleRoutes.ChargingPointController.onPageLoad(NormalMode)
    case TaxRatePage => orgSingleRoutes.TaxRateController.onPageLoad(NormalMode)
    case WhatTypeOfSecuritiesPage => orgSingleRoutes.WhatTypeOfSecuritiesController.onPageLoad(NormalMode)
    case OtherSecuritiesTypePage => orgSingleRoutes.OtherSecuritiesTypeController.onPageLoad(NormalMode)
    case DetailsOfThisTransferPage => orgSingleRoutes.DetailsOfThisTransferController.onPageLoad(NormalMode)
    case AmountPaidForSecuritiesPage => orgSingleRoutes.AmountPaidForSecuritiesController.onPageLoad(NormalMode)
    case TotalMarketValuePage => orgSingleRoutes.TotalMarketValueController.onPageLoad(NormalMode)
    case CheckYourAnswersPage => stfSingleCyaRoutes.CheckYourAnswersController.onPageLoad()
    case _ => routes.JourneyRecoveryController.onPageLoad()
  }

  override def restore(submissionId: SubmissionId, userId: UserId)(implicit hc: HeaderCarrier): Future[UserAnswers] = {
    answerPersistenceService.load(submissionId, userId)
  }
