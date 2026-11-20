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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers.dashboard

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SaveAndReturnClient
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.config.SaveAndReturnRetrievalType.{UserAndGroup, UserOnly}
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.requests.StcAuthorisedRequest
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{SubmissionId, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.handlers.ErrorHandler
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.dashboard.DraftViewModel
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.dashboard.DraftView

import java.time.Clock
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class DraftController @Inject()(
                                 override val messagesApi: MessagesApi,
                                 @Named("any") auth: StcAuthEnrolledAction,
                                 saveAndReturnClient: SaveAndReturnClient,
                                 val controllerComponents: MessagesControllerComponents,
                                 view: DraftView,
                                 clock: Clock,
                                 appConfig: FrontendAppConfig,
                                 errorHandler: ErrorHandler
                               )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(submissionId: String): Action[AnyContent] = auth.async { implicit request =>
    withOwnDraft(SubmissionId(submissionId)) { userAnswers =>
      val model = DraftViewModel.build(userAnswers, request.affinityGroupKey, clock, appConfig.draftTtlDays)
      Future.successful(Ok(view(model)))
    }
  }

  def onDeleteDraft(submissionId: String): Action[AnyContent] = auth.async {
    implicit request =>
      withOwnDraft(SubmissionId(submissionId)) { _ =>
        Future.successful(Ok("success"))
      }
  }
  
  private def withOwnDraft(submissionId: SubmissionId)(block: UserAnswers => Future[Result])(implicit request: StcAuthorisedRequest[AnyContent]): Future[Result] =
    saveAndReturnClient.retrieve(submissionId)
      .flatMap(userAnswers => if (ownsDraft(userAnswers)) block(userAnswers) else notFound)
      .recoverWith { case UpstreamErrorResponse.WithStatusCode(NOT_FOUND) => notFound }

  private def notFound(implicit request: StcAuthorisedRequest[AnyContent]): Future[Result] =
    errorHandler.notFoundTemplate.map(NotFound(_))

  private def ownsDraft(userAnswers: UserAnswers)(implicit request: StcAuthorisedRequest[AnyContent]): Boolean =
    appConfig.saveAndReturnRetrieval match {
      case UserOnly     => userAnswers.userId.value == request.internalId
      case UserAndGroup => userAnswers.groupIdentifier.value == request.groupIdentifier
    }
}

