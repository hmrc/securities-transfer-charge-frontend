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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.single

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.SaveAndReturnButton.isReturn
import uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.shared.ApplyingForReliefFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Mode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.ApplyingForReliefPage
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.agents.single.ApplyingForReliefView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class ApplyingForReliefController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         @Named("agentsSh03") navigator: Navigator,
                                         stcAuthEnrolled: StcAuthEnrolledAction,
                                         getData: StcDataRetrievalAction,
                                         requireData: StcDataRequiredAction,
                                         formProvider: ApplyingForReliefFormProvider,
                                         val controllerComponents: MessagesControllerComponents,
                                         view: ApplyingForReliefView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {
  
  lazy val backLinkCall: Mode => UserAnswers => Call =
    mode => userAnswers => navigator.previousPageCall(ApplyingForReliefPage, mode, userAnswers)

  def onPageLoad(mode: Mode): Action[AnyContent] = (stcAuthEnrolled andThen getData andThen requireData) {
    implicit request =>

      val innerRequest = request.request
      val affinityKey = innerRequest.affinityGroupKey

      val form = formProvider(affinityKey)

      val preparedForm = request.userAnswers.get(ApplyingForReliefPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode, backLinkCall(mode)(request.userAnswers)))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (stcAuthEnrolled andThen getData andThen requireData).async {
    implicit request =>

      val innerRequest = request.request
      val affinityKey = innerRequest.affinityGroupKey

      val form = formProvider(affinityKey)

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode, backLinkCall(mode)(request.userAnswers)))),

        applyingForRelief =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(ApplyingForReliefPage, applyingForRelief))
            nextPage <- navigator.nextPageCall(ApplyingForReliefPage, mode, updatedAnswers, isReturn(request))
          } yield Redirect(nextPage)
      )
  }
}
