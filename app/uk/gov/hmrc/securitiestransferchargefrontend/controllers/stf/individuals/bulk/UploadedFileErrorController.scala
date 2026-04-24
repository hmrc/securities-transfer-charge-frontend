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

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.bulk.routes as individualRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.FileParseError
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.UpscanJourneyStatus
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.UpscanJourneyRepository
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcUpscanProcessingService
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.stf.fileupload.UploadedFileErrorMapper
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.individuals.bulk.UploadedFileErrorView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UploadedFileErrorController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             stcAuthEnrolled: StcAuthEnrolledAction,
                                             val controllerComponents: MessagesControllerComponents,
                                             upscanJourneyRepository: UpscanJourneyRepository,
                                             stcUpscanProcessingService: StcUpscanProcessingService,
                                             view: UploadedFileErrorView
                                           )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {


  // TODO: This controller currently re-processes the uploaded file to render the
  //  error page. Maybe we can persist the validation result or blocking errors against
  //  the upload reference then this controller can be streamlined to only render and simply load the
  //  errors instead of calling stcUpscanProcessingService.process() again
  def onPageLoad(reference: String): Action[AnyContent] =
    stcAuthEnrolled.async { implicit request =>
      upscanJourneyRepository.find(reference).flatMap {
        case None =>
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))

        case Some(fileUpload) if fileUpload.status != UpscanJourneyStatus.Ready =>
          Future.successful(Redirect(individualRoutes.FileUploadController.onPageLoad()))

        case Some(fileUpload) =>
          stcUpscanProcessingService.process(fileUpload).map {
            case Left(_: FileParseError) =>
              Redirect(individualRoutes.FormattingErrorController.onPageLoad())

            case Right(validationResponse) if validationResponse.tooManyBlockingErrors =>
              Redirect(individualRoutes.FormattingErrorController.onPageLoad())

            case Right(validationResponse) if validationResponse.hasBlockingErrors =>
              Ok(view(UploadedFileErrorMapper.from(validationResponse.blockingErrors)))

            case Right(_) =>
              Redirect(individualRoutes.FileUploadedController.onPageLoad(reference))
          }
      }
    }
}