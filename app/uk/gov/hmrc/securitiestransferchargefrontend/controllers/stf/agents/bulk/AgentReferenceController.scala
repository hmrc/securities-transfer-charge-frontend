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
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.SaveAndReturnButton.isReturn
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.agents.AgentReferenceFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Mode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.AgentReference
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.AgentReferencePage
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
                                          view: AgentReferenceView
                                        )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[AgentReference] = formProvider()

  lazy val backLinkCall: Mode => UserAnswers => Call =
    mode => userAnswers => navigator.previousPage(AgentReferencePage, mode, userAnswers)

  def onPageLoad(mode: Mode): Action[AnyContent] = (stcAuthEnrolled andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(AgentReferencePage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode, backLinkCall(mode)(request.userAnswers)): Html)
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (stcAuthEnrolled andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode, backLinkCall(mode)(request.userAnswers)): Html)),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(AgentReferencePage, value))
            nextPage       <- navigator.nextPage(AgentReferencePage, mode, updatedAnswers, isReturn(request))
          } yield Redirect(nextPage)
      )
  }
}
