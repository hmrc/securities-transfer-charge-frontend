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

import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SaveAndReturnClient
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.config.SaveAndReturnRetrievalType.{UserAndGroup, UserOnly}
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.requests.StcAuthorisedRequest
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{SubmissionId, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.handlers.ErrorHandler
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.services.AnswerPersistenceService
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.dashboard.DraftViewModel
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.dashboard.DraftView
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.dashboard.routes as dashboardRoutes

import java.time.Clock
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class DraftController @Inject()(
                                 override val messagesApi: MessagesApi,
                                 @Named("any") auth: StcAuthEnrolledAction,
                                 saveAndReturnClient: SaveAndReturnClient,
                                 answerPersistenceService: AnswerPersistenceService,
                                 val controllerComponents: MessagesControllerComponents,
                                 view: DraftView,
                                 clock: Clock,
                                 appConfig: FrontendAppConfig,
                                 errorHandler: ErrorHandler
                               )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad(submissionId: String): Action[AnyContent] = auth.async { implicit request =>
    withOwnDraft(SubmissionId(submissionId)) { userAnswers =>
      val model = DraftViewModel.build(userAnswers, request.affinityGroupKey, clock, userAnswers.journeyType)
      Future.successful(Ok(view(model)))
    }
  }

  def onContinueSubmission(submissionId: String): Action[AnyContent] = auth.async {
    implicit request =>
      val subId = SubmissionId(submissionId)
      val userId = UserId(request.internalId)
      answerPersistenceService.load(subId, userId).map { userAnswers =>
        val nextPage = userAnswers.nextPage.getOrElse(routes.JourneyRecoveryController.onPageLoad())
        Redirect(nextPage)
      }
  }

  //Todo temp redirect to the dashboard page for now until we have the confirm delete page
  def onDeleteDraft(submissionId: String): Action[AnyContent] = auth.async {
    implicit request =>
      val subId = SubmissionId(submissionId)
      withOwnDraft(subId) { _ =>
        saveAndReturnClient.deleteDraft(subId).map { _ =>
          Redirect(dashboardRoutes.DashboardController.onPageLoad())
        }
      }
  }

  // Only lets a user act on a draft the dashboard would list for them; anything else is a 404,
  // so another user's draft can't be viewed or deleted by putting its ID in the URL.
  private def withOwnDraft(submissionId: SubmissionId)(block: UserAnswers => Future[Result])(implicit request: StcAuthorisedRequest[AnyContent]): Future[Result] =
    saveAndReturnClient.retrieve(submissionId).flatMap { userAnswers =>
      if (ownsDraft(userAnswers)) block(userAnswers)
      else {
        logger.warn(s"Draft $submissionId does not belong to the requesting user; returning 404")
        errorHandler.notFoundTemplate.map(NotFound(_))
      }
    }

  private def ownsDraft(userAnswers: UserAnswers)(implicit request: StcAuthorisedRequest[AnyContent]): Boolean =
    appConfig.saveAndReturnRetrieval match {
      case UserOnly     => userAnswers.userId.value == request.internalId
      case UserAndGroup => userAnswers.groupIdentifier.value == request.groupIdentifier
    }
}

