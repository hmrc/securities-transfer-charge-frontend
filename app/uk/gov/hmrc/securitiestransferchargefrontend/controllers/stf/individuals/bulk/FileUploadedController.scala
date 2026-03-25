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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.bulk

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.StcAuthEnrolledAction
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.UpscanJourneyRepository
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.individuals.bulk.FileUploadedView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

//TODO Placeholder controller, update once the content had been confirmed by ucd
class FileUploadedController @Inject()(
                                        val controllerComponents: MessagesControllerComponents,
                                        upscanJourneyRepository: UpscanJourneyRepository,
                                        stcAuthEnrolled: StcAuthEnrolledAction,
                                        view: FileUploadedView
                                      )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport {

  def onPageLoad(key: String): Action[AnyContent] =
    stcAuthEnrolled.async { implicit request =>
      
      upscanJourneyRepository.find(key).map {
        case Some(fileUpload) =>
          Ok(view(fileUpload))
          
        case None =>
          Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
    }
}
