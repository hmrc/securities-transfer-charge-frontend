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
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.StcAuthEnrolledAction
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.PersistentNavigator
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.stf.individuals.StfNavigator

import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext

class SaveAndReturnController @Inject()( override val messagesApi: MessagesApi,
                                         val controllerComponents: MessagesControllerComponents,
                                         stcAuthEnrolled: StcAuthEnrolledAction,
                                         @Named("individuals") navigator: PersistentNavigator)
                                       ( implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport:

  def restore(submissionId: String): Action[AnyContent] =
    stcAuthEnrolled.async { implicit request =>
      for {
        userAnswers <- navigator.restore(SubmissionId(submissionId), request.internalId)
        nextPage     = userAnswers.nextPage.getOrElse(StfNavigator.startPage)
      } yield Redirect(nextPage)
    }
