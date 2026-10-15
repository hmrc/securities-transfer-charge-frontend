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
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.bulk.routes as bulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.single.routes as singleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.individuals.HowToNotifyAboutSecuritiesTransferFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.HowToNotifyAboutSecuritiesTransfer
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Mode, NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.HowToNotifyAboutSecuritiesTransferPage
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.individuals.HowToNotifyAboutSecuritiesTransferView

import javax.inject.{Inject, Named}
import scala.concurrent.Future
import scala.language.postfixOps

class HowToNotifyAboutSecuritiesTransferController @Inject()(
                                                              override val messagesApi: MessagesApi,
                                                              @Named("individuals") navigator: Navigator,
                                                              stcAuthEnrolled: StcAuthEnrolledAction,
                                                              getData: StcDataRetrievalAction,
                                                              formProvider: HowToNotifyAboutSecuritiesTransferFormProvider,
                                                              val controllerComponents: MessagesControllerComponents,
                                                              view: HowToNotifyAboutSecuritiesTransferView,
                                                            ) extends FrontendBaseController with I18nSupport {

  val form: Form[HowToNotifyAboutSecuritiesTransfer] = formProvider()

  lazy val backLinkCall: Mode => Option[UserAnswers] => Call =
    mode => userAnswers => navigator.previousPage(HowToNotifyAboutSecuritiesTransferPage, mode, userAnswers)

  def onPageLoad(): Action[AnyContent] = (stcAuthEnrolled andThen getData) {
    implicit request =>
      val innerRequest = request.request
      val preparedForm = request.userAnswers.flatMap(_.get(HowToNotifyAboutSecuritiesTransferPage))
        .map(form.fill)
        .getOrElse(form)

      Ok(view(preparedForm, NormalMode, innerRequest.affinityGroupKey, backLinkCall(NormalMode)(request.userAnswers)))
  }

  def onSubmit(): Action[AnyContent] = (stcAuthEnrolled andThen getData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, NormalMode, request.request.affinityGroupKey, backLinkCall(NormalMode)(request.userAnswers)))),

        {
          case HowToNotifyAboutSecuritiesTransfer.OneAtATime => Future.successful(Redirect(singleRoutes.ConfirmAddressController.onPageLoad()))
          case HowToNotifyAboutSecuritiesTransfer.MoreThanOneAtATime => Future.successful(Redirect(bulkRoutes.TemplateInstructionsController.onPageLoad()))
        }
      )
  }
}
