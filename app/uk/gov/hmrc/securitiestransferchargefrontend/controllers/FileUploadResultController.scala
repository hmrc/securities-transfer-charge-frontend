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

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.models.upscan.UpscanJourneyStatus
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.UpscanJourneyRepository
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcUpscanProcessingService
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.fileupload.StcUploadResultViewModel
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.FileUploadResultView
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.routes as individualRoutes

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FileUploadResultController @Inject()(
                                            val controllerComponents: MessagesControllerComponents,
                                            stcAuthEnrolled: StcAuthEnrolledAction,
                                            upscanJourneyRepository: UpscanJourneyRepository,
                                            stcUpscanProcessingService: StcUpscanProcessingService,
                                            view: FileUploadResultView
                                          )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(reference: String): Action[AnyContent] =
    stcAuthEnrolled.async { implicit request =>
      upscanJourneyRepository.find(reference).flatMap {
        case Some(fileUpload) if fileUpload.status == UpscanJourneyStatus.Ready =>
          stcUpscanProcessingService.process(fileUpload).map {
            case Right(validationResponse) =>
              Ok(view(StcUploadResultViewModel.from(validationResponse)))

            case Left(parseError) =>
              Redirect(individualRoutes.FormattingErrorController.onPageLoad())
                .flashing("uploadParseError" -> parseError.message)
          }

        case Some(_) =>
          Future.successful(Redirect(individualRoutes.FileUploadController.onPageLoad()))

        case None =>
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
    }
}