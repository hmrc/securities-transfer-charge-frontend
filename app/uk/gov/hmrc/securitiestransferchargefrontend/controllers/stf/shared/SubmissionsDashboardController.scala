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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared

import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.clients.{SaveAndReturnClient, SubmissionIdClient}
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.{StcAuthEnrolledAction, StcDataRetrievalAction}
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{GroupIdentifier, SubmissionId, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.audit.{AuditModel, AuditType}
import uk.gov.hmrc.securitiestransferchargefrontend.models.audit.JourneyStatus.StartSubmission
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.SubmissionsDashboardPage
import uk.gov.hmrc.securitiestransferchargefrontend.services.AuditService
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.shared.SubmissionsDashboardView
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.shared.routes as sh03SharedRoutes

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class SubmissionsDashboardController @Inject()(
                                                override val messagesApi: MessagesApi,
                                                val controllerComponents: MessagesControllerComponents,
                                                appConfig: FrontendAppConfig,
                                                stcAuthEnrolled: StcAuthEnrolledAction,
                                                getData: StcDataRetrievalAction,
                                                view: SubmissionsDashboardView,
                                                idClient: SubmissionIdClient,
                                                saveAndReturnClient: SaveAndReturnClient,
                                                @Named("individuals") individualsNavigator: Navigator,
                                                @Named("organisations") orgNavigator: Navigator,
                                                @Named("agents") agentNavigator: Navigator,
                                                auditService: AuditService)
                                              (implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad(): Action[AnyContent] =
    (stcAuthEnrolled andThen getData).async { implicit request =>

      val userId = UserId(request.request.internalId)
      val groupIdentifier = GroupIdentifier(request.request.groupIdentifier)
      listSubmissionIds(userId, groupIdentifier).map { submissionIds =>
        Ok(view(submissionIds))
      }
    }


  private def listSubmissionIds(userId: UserId, groupIdentifier: GroupIdentifier)(implicit headerCarrier: HeaderCarrier): Future[List[SubmissionId]] = {
    import appConfig.SaveAndReturnRetrievalType._
    appConfig.saveAndReturnRetrieval match {
      case UserOnly     => saveAndReturnClient.listByUser(userId)
      case UserAndGroup => saveAndReturnClient.listByGroup(groupIdentifier)
    }
  }

  def onSubmit(): Action[AnyContent] = (stcAuthEnrolled andThen getData).async {
    implicit request =>

      val innerRequest = request.request
      val userId = UserId(innerRequest.internalId)
      val group = GroupIdentifier(innerRequest.groupIdentifier)

      for {
        submissionId <- idClient.nextSubmissionId()
        emptyAnswers = UserAnswers.empty(userId)(group)(submissionId)

        call <- innerRequest.affinityGroup match {
          case AffinityGroup.Organisation =>
            orgNavigator.nextPage(SubmissionsDashboardPage, NormalMode, emptyAnswers)

          case AffinityGroup.Agent =>
            agentNavigator.nextPage(SubmissionsDashboardPage, NormalMode, emptyAnswers)

          case _ =>
            individualsNavigator.nextPage(SubmissionsDashboardPage, NormalMode, emptyAnswers)
        }
      } yield {
        auditService.audit(
          AuditModel(
            StartSubmission,
            innerRequest.subscriptionId,
            innerRequest.affinityGroup,
            innerRequest.credentialId,
            Some(submissionId),
            AuditType.Stf
          )
        )
        Redirect(call)
      }
  }

  def startSh03(): Action[AnyContent] = stcAuthEnrolled { implicit request =>
    Redirect(sh03SharedRoutes.BeforeYouStartController.onPageLoad())
  }
}
