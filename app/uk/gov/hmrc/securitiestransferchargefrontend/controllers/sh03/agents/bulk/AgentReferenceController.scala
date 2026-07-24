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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.bulk

import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SubmissionIdClient
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.SaveAndReturnButton.isReturn
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{GroupIdentifier, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.forms.shared.AgentReferenceFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.shared.AgentReference
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Mode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.bulk.BulkAgentReferencePage
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.agents.bulk.AgentReferenceView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class AgentReferenceController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          @Named("agentsSh03") navigator: Navigator,
                                          stcAuthEnrolled: StcAuthEnrolledAction,
                                          getData: StcDataRetrievalAction,
                                          formProvider: AgentReferenceFormProvider,
                                          idClient: SubmissionIdClient,
                                          val controllerComponents: MessagesControllerComponents,
                                          view: AgentReferenceView
                                        )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {
  lazy val backLinkCall: Mode => Option[UserAnswers] => Call =
    mode => userAnswers => navigator.previousPage(BulkAgentReferencePage, mode, userAnswers)

  val form: Form[AgentReference] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (stcAuthEnrolled andThen getData) { implicit request =>

      val preparedForm = request.userAnswers.flatMap(_.get(BulkAgentReferencePage))
        .map(form.fill)
        .getOrElse(form)

      Ok(view(preparedForm, mode, backLinkCall(mode)(request.userAnswers)))
    }

  def onSubmit(mode: Mode): Action[AnyContent] = (stcAuthEnrolled andThen getData).async {
    implicit request =>


      val innerRequest = request.request
      val userId = UserId(innerRequest.internalId)
      val group = GroupIdentifier(innerRequest.groupIdentifier)

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode,backLinkCall(mode)(request.userAnswers)))),

        value =>
          for {
            submissionId <- idClient.nextSubmissionId()
            emptyAnswers = UserAnswers.empty(userId)(group)(submissionId)
            updatedAnswers <- Future.fromTry(emptyAnswers.set(BulkAgentReferencePage, value))
            nextPage       <- navigator.nextPage(BulkAgentReferencePage, mode, updatedAnswers, isReturn(request))
          } yield Redirect(nextPage)
      )
  }
}
