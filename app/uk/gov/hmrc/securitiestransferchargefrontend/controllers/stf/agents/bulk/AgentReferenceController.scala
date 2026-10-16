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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.agents.bulk

import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SubmissionIdClient
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.SaveAndReturnButton.isReturn
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{GroupIdentifier, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.forms.shared.AgentReferenceFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType.STF
import uk.gov.hmrc.securitiestransferchargefrontend.models.shared.AgentReference
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Mode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.bulk.BulkAgentReferencePage
import uk.gov.hmrc.securitiestransferchargefrontend.services.AnswerPersistenceService
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.agents.bulk.AgentReferenceView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class AgentReferenceController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          @Named("agents") navigator: Navigator,
                                          stcAuthEnrolled: StcAuthEnrolledAction,
                                          getData: StcDataRetrievalAction,
                                          requireData: StcDataRequiredAction,
                                          formProvider: AgentReferenceFormProvider,
                                          val controllerComponents: MessagesControllerComponents,
                                          view: AgentReferenceView,
                                          answerPersistenceService: AnswerPersistenceService,
                                          idClient: SubmissionIdClient
                                        )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[AgentReference] = formProvider()

  lazy val backLinkCall: Mode => UserAnswers => Call =
    mode => userAnswers => navigator.previousPage(BulkAgentReferencePage, mode, userAnswers)

  def onPageLoad(mode: Mode, fileUploadReference: String): Action[AnyContent] =
    (stcAuthEnrolled andThen getData).async { implicit request =>

      val userId = UserId(request.request.internalId)
      val group = GroupIdentifier(request.request.groupIdentifier)

      for {
        answers <- request.userAnswers.fold {
          idClient.nextSubmissionId().map { submissionId =>UserAnswers.empty(userId)(group)(submissionId)(STF)}
        }(Future.successful)

        updatedAnswers = answers.setFileUploadReference(fileUploadReference)

        _ <- answerPersistenceService.save(updatedAnswers)
      } yield {

        val preparedForm = updatedAnswers.get(BulkAgentReferencePage)
          .map(form.fill)
          .getOrElse(form)

        Ok(view(preparedForm, mode, backLinkCall(mode)(updatedAnswers)): Html)
      }
    }


  def onSubmit(mode: Mode): Action[AnyContent] = (stcAuthEnrolled andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode, backLinkCall(mode)(request.userAnswers)): Html)),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(BulkAgentReferencePage, value))
            nextPage <- navigator.nextPage(BulkAgentReferencePage, mode, updatedAnswers, isReturn(request))
          } yield Redirect(nextPage)
      )
  }
}
