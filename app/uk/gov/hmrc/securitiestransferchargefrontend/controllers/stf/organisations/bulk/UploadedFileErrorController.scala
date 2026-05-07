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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations.bulk

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.UploadedFileError
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.organisations.bulk.UploadedFileErrorView

import javax.inject.Inject

class UploadedFileErrorController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             stcAuthEnrolled: StcAuthEnrolledAction,
                                             val controllerComponents: MessagesControllerComponents,
                                             view: UploadedFileErrorView,
                                           ) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = stcAuthEnrolled {
    implicit request =>
      Ok(view(stubUploadedFileErrors))
  }

  private def stubUploadedFileErrors: Seq[UploadedFileError] = {
    Seq(
      UploadedFileError(
        cell = "J6",
        error = "The seller's name cannot contain numbers"
      ),
      UploadedFileError(
        cell = "J36",
        error = "You have selected that the buyer is a company, you need to enter the registered address"
      ),
      UploadedFileError(
        cell = "K3",
        error = "Buyer's country can only contain letters, numbers and hyphens"
      )
    )
  }
}