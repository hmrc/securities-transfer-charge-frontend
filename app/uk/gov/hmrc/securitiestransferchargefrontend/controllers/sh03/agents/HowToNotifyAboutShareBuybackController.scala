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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents

import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SubmissionIdClient
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{GroupIdentifier, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.shared.HowToNotifyAboutShareBuybackFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.HowToNotifyAboutShareBuyback
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Mode, NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.HowToNotifyAboutShareBuybackPage
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.agents.HowToNotifyAboutShareBuybackView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class HowToNotifyAboutShareBuybackController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       @Named("agentsSh03") navigator: Navigator,
                                       idClient: SubmissionIdClient,
                                       stcAuthEnrolled: StcAuthEnrolledAction,
                                       getData: StcDataRetrievalAction,
                                       formProvider: HowToNotifyAboutShareBuybackFormProvider,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: HowToNotifyAboutShareBuybackView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  lazy val backLinkCall: Mode => Option[UserAnswers] => Call =
    mode => userAnswers => navigator.previousPage(HowToNotifyAboutShareBuybackPage, mode, userAnswers)

  def onPageLoad(): Action[AnyContent] = (stcAuthEnrolled andThen getData) {implicit request =>

    val innerRequest = request.request
    val form = formProvider(innerRequest.affinityGroupKey)

    val preparedForm = request.userAnswers match {
      case Some(userAnswers) => userAnswers.get(HowToNotifyAboutShareBuybackPage) match {
          case Some(answer) => form.fill(answer)
          case None         => form
        }

      case None => form
    }

    Ok(view(preparedForm, NormalMode, innerRequest.affinityGroupKey, backLinkCall(NormalMode)(request.userAnswers)))
  }


  def onSubmit(): Action[AnyContent] = (stcAuthEnrolled andThen getData).async {
    implicit request =>

      val innerRequest = request.request
      val userId = UserId(innerRequest.internalId)
      val group = GroupIdentifier(innerRequest.groupIdentifier)
      val form: Form[HowToNotifyAboutShareBuyback] = formProvider(innerRequest.affinityGroupKey)

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, NormalMode, innerRequest.affinityGroupKey, backLinkCall(NormalMode)(request.userAnswers)))),

        howToNotify =>
          for {
            submissionId <- idClient.nextSubmissionId()
            emptyAnswers = UserAnswers.empty(userId)(group)(submissionId)
            updatedAnswers <- Future.fromTry(emptyAnswers.set(HowToNotifyAboutShareBuybackPage, howToNotify))
            nextPage <- navigator.nextPage(HowToNotifyAboutShareBuybackPage, NormalMode, updatedAnswers)
          } yield Redirect(nextPage)
      )
  }
}
