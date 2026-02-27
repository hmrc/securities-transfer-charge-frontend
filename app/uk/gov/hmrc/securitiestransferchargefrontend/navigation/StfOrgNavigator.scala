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

package uk.gov.hmrc.securitiestransferchargefrontend.navigation


import com.google.inject.Singleton
import play.api.mvc.{Call, Request}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations.routes as orgRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, Mode, NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.*
import uk.gov.hmrc.securitiestransferchargefrontend.services.AnswerPersistenceService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StfOrgNavigator @Inject()(answerPersistenceService: AnswerPersistenceService)
                               (implicit ec: ExecutionContext) extends AbstractNavigator(answerPersistenceService) {

  private def normalRoutes(page: Page)(implicit hc: HeaderCarrier): UserAnswers => Future[Call] = page match {
    case SubmissionsDashboardPage => userAnswers => goTo(orgRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad(NormalMode), Some(userAnswers))
    case ConfirmAddressPage => userAnswers => dataRequired(ConfirmAddressPage, userAnswers, routes.JourneyRecoveryController.onPageLoad())
    case _ => _ => Navigator.defaultPageF

  }

  override def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers)(implicit request: Request[_]): Future[Call] = {
    mode match {
      case NormalMode =>
        implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
        normalRoutes(page)(hc)(userAnswers)
      case CheckMode => Future.successful(checkRouteMap(page)(userAnswers))
    }
  }

  val checkRouteMap: Page => UserAnswers => Call = (_ => _ => routes.CheckYourAnswersController.onPageLoad())


  override val errorPage: Page => Call = (_ => routes.JourneyRecoveryController.onPageLoad())
}

