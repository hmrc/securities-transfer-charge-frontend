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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations

import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SubmissionIdClient
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.SaveAndReturnButton.isReturn
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{GroupIdentifier, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.organisations.HowToNotifyAboutSecuritiesTransferFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.HowToNotifyAboutSecuritiesTransfer
import uk.gov.hmrc.securitiestransferchargefrontend.models.{JourneyType, Mode, NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.HowToNotifyAboutSecuritiesTransferPage
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.organisations.HowToNotifyAboutSecuritiesTransferView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class HowToNotifyAboutSecuritiesTransferController @Inject()(
                                                              override val messagesApi: MessagesApi,
                                                              @Named("organisations") orgNavigator: Navigator,
                                                              stcAuthEnrolled: StcAuthEnrolledAction,
                                                              getData: StcDataRetrievalAction,
                                                              formProvider: HowToNotifyAboutSecuritiesTransferFormProvider,
                                                              val controllerComponents: MessagesControllerComponents,
                                                              view: HowToNotifyAboutSecuritiesTransferView,
                                                              idClient: SubmissionIdClient
                                                            )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[HowToNotifyAboutSecuritiesTransfer] = formProvider()

  lazy val backLinkCall: Mode => Option[UserAnswers] => Call =
    mode => userAnswers => orgNavigator.previousPage(HowToNotifyAboutSecuritiesTransferPage, mode, userAnswers)

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

      val innerRequest = request.request
      val userId = UserId(innerRequest.internalId)
      val group = GroupIdentifier(innerRequest.groupIdentifier)

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, NormalMode, innerRequest.affinityGroupKey, backLinkCall(NormalMode)(request.userAnswers)))),

        howToNotify =>
          for {
            answers <- request.userAnswers.fold {
              idClient.nextSubmissionId().map(submissionId => UserAnswers.empty(userId)(group)(submissionId)(JourneyType.STF))
            }(Future.successful)
            updatedAnswers <- Future.fromTry(answers.set(HowToNotifyAboutSecuritiesTransferPage, howToNotify))
            nextPage <- orgNavigator.nextPage(HowToNotifyAboutSecuritiesTransferPage, NormalMode, updatedAnswers, isReturn(request))
          } yield Redirect(nextPage)
      )
  }
}
