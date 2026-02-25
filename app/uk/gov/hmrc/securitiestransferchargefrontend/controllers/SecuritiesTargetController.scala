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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers

import play.api.data.Form
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.forms.SecuritiesTargetFormProvider

import javax.inject.Inject
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Mode, SecuritiesTarget}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.SecuritiesTargetPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.models.Mode.submitErrorModeFilter
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.SecuritiesTargetView

import scala.concurrent.{ExecutionContext, Future}

class SecuritiesTargetController @Inject()(
                                      override val messagesApi: MessagesApi,
                                      navigator: Navigator,
                                      stcAuthEnrolled: StcAuthEnrolledAction,
                                      getData: StcDataRetrievalAction,
                                      requireData: StcDataRequiredAction,
                                      formProvider: SecuritiesTargetFormProvider,
                                      val controllerComponents: MessagesControllerComponents,
                                      view: SecuritiesTargetView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[SecuritiesTarget] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (stcAuthEnrolled andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(SecuritiesTargetPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode): Html)
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (stcAuthEnrolled andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, submitErrorModeFilter(mode)): Html)),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(SecuritiesTargetPage, value))
            nextPage       <- navigator.nextPage(SecuritiesTargetPage, mode, updatedAnswers)
          } yield Redirect(nextPage)
      )
  }
}
