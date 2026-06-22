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

import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SubmissionIdClient
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{GroupIdentifier, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.agents.BeforeYouStartPage
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.agents.single.BeforeYouStartView

import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext

class BeforeYouStartController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          stcAuthEnrolled: StcAuthEnrolledAction,
                                          val controllerComponents: MessagesControllerComponents,
                                          view: BeforeYouStartView,
                                          @Named("agentsSh03") navigator: Navigator,
                                          submissionIdClient: SubmissionIdClient
                                        ) (implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad(): Action[AnyContent] = stcAuthEnrolled {
    implicit request =>
      Ok(view())
  }

  def onSubmit(): Action[AnyContent] = stcAuthEnrolled.async { implicit request =>
    val user = UserId(request.internalId)
    val group = GroupIdentifier(request.groupIdentifier)
    for {
      submission <- submissionIdClient.nextSubmissionId()
      emptyAnswers = UserAnswers.empty(user)(group)(submission)
      nextPage <- navigator.nextPage(BeforeYouStartPage, NormalMode, emptyAnswers)
      _ = logger.info(s"Next page is ${nextPage.url}")
    } yield Redirect(nextPage)
  }
}
