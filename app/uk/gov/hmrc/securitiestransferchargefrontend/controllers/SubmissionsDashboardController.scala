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

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SubmissionIdClient
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.{StcAuthEnrolledAction, StcDataRetrievalAction}
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.SubmissionsDashboardPage
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.SubmissionsDashboardView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SubmissionsDashboardController @Inject()(
                                                override val messagesApi: MessagesApi,
                                                val controllerComponents: MessagesControllerComponents,
                                                stcAuthEnrolled: StcAuthEnrolledAction,
                                                getData: StcDataRetrievalAction,
                                                view: SubmissionsDashboardView,
                                                idClient: SubmissionIdClient,
                                                navigator: Navigator)
                                              (implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (stcAuthEnrolled andThen getData) {
    implicit request =>
      Ok(view())
  }

  def onSubmit(): Action[AnyContent] = (stcAuthEnrolled andThen getData).async {
    implicit request =>
      val userId = request.request.internalId
      for {
        submissionId  <- idClient.nextSubmissionId()
        emptyAnswers  =  UserAnswers.empty(userId)(submissionId)
        nextPage      <- navigator.nextPage(SubmissionsDashboardPage, NormalMode, emptyAnswers)
      } yield Redirect(nextPage)
  }
}
