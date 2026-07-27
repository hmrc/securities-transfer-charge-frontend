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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers.fileUpload

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.UpscanInitiateConnector
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.fileUpload.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes.JourneyRecoveryController
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.*
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.UpscanJourneyRepository
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.processing.FileProcessingRefreshCounterFactory
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.fileUpload.FileUploadView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FileUploadController @Inject()(
                                      override val messagesApi: MessagesApi,
                                      stcAuthEnrolled: StcAuthEnrolledAction,
                                      val controllerComponents: MessagesControllerComponents,
                                      view: FileUploadView,
                                      upscanInitiateConnector: UpscanInitiateConnector,
                                      upscanJourneyRepository: UpscanJourneyRepository,
                                      fileProcessingRefreshCounterFactory: FileProcessingRefreshCounterFactory,

                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(journeyType: JourneyType): Action[AnyContent] = stcAuthEnrolled.async { implicit request =>
    prepareUpload(journeyType).map { response =>
      val uploadView = view(response.uploadRequest,journeyType = journeyType)
      val counter = fileProcessingRefreshCounterFactory(request)
      counter.reset(Ok(uploadView))
    }
  }

  def onUploadError(): Action[AnyContent] =
    stcAuthEnrolled.async { implicit request =>
      request.getQueryString("key") match {
        case Some(reference) =>
          upscanJourneyRepository.find(reference).flatMap {
            case Some(fileUpload) =>
              upscanJourneyRepository.delete(reference).map { _ =>
                Redirect(routes.BulkUploadErrorController.onPageLoad(fileUpload.journeyType))
              }
            case None =>
              Future.successful(Redirect(JourneyRecoveryController.onPageLoad()))
          }
        case None =>
          Future.successful(Redirect(JourneyRecoveryController.onPageLoad()))
      }
    }

  private def prepareUpload(journeyType: JourneyType)(implicit request: Request[_]): Future[UpscanInitiateResponse] =
    for {
      initiateResponse <- upscanInitiateConnector.initiate()

      fileUpload = FileUpload(
        reference = initiateResponse.reference,
        status = UpscanJourneyStatus.Initiated,
        journeyType = journeyType
      )

      _ <- upscanJourneyRepository.insert(
        UpscanDocument(initiateResponse.reference, fileUpload)
      )

    } yield initiateResponse

}