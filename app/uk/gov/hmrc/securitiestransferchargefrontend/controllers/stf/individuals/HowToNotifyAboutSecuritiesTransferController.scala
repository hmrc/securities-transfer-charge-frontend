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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals

import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.individuals.HowToNotifyAboutSecuritiesTransferFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.{HowToNotifyAboutSecuritiesTransfer, Mode}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.individuals.HowToNotifyAboutSecuritiesTransferPage
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.individuals.HowToNotifyAboutSecuritiesTransferView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class HowToNotifyAboutSecuritiesTransferController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       navigator: Navigator,
                                       stcAuthEnrolled: StcAuthEnrolledAction,
                                       getData: StcDataRetrievalAction,
                                       requireData: StcDataRequiredAction,
                                       formProvider: HowToNotifyAboutSecuritiesTransferFormProvider,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: HowToNotifyAboutSecuritiesTransferView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[HowToNotifyAboutSecuritiesTransfer] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (stcAuthEnrolled andThen getData andThen requireData).async{
    implicit request =>
      val preparedForm = request.userAnswers.get(HowToNotifyAboutSecuritiesTransferPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Future.successful(Ok(view(preparedForm, mode)))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (stcAuthEnrolled andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        howToNotify =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(HowToNotifyAboutSecuritiesTransferPage, howToNotify))
            nextPage       <- navigator.nextPage(HowToNotifyAboutSecuritiesTransferPage, mode, updatedAnswers)
          } yield Redirect(nextPage)
      )
  }
}
