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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.single

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.SaveAndReturnButton.isReturn
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.individuals.ChargingPointFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Mode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.individuals.ChargingPointPage
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.individuals.single.ChargingPointView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class ChargingPointController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        @Named("individuals") navigator: Navigator,
                                        stcAuthEnrolled: StcAuthEnrolledAction,
                                        getData: StcDataRetrievalAction,
                                        requireData: StcDataRequiredAction,
                                        formProvider: ChargingPointFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: ChargingPointView
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  lazy val backLinkCall: Mode => UserAnswers => Call =
    mode => userAnswers => navigator.previousPage(ChargingPointPage, mode, userAnswers)

  def onPageLoad(mode: Mode): Action[AnyContent] = (stcAuthEnrolled andThen getData andThen requireData) {
    implicit request =>

      val form = formProvider()

      val preparedForm = request.userAnswers.get(ChargingPointPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode, backLinkCall(mode)(request.userAnswers)))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (stcAuthEnrolled andThen getData andThen requireData).async {
    implicit request =>

      val form = formProvider()

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode, backLinkCall(mode)(request.userAnswers)))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(ChargingPointPage, value))
            nextPage       <- navigator.nextPage(ChargingPointPage, mode, updatedAnswers, isReturn(request))
          } yield Redirect(nextPage)
      )
  }
}
