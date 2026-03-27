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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals

import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.UpscanInitiateConnector
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.upscan.*
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.UpscanJourneyRepository
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.FileUploadJSView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FileUploadController @Inject()(
                                      override val messagesApi: MessagesApi,
                                      stcAuthEnrolled: StcAuthEnrolledAction,
                                      val controllerComponents: MessagesControllerComponents,
                                      view: FileUploadJSView,
                                      upscanInitiateConnector: UpscanInitiateConnector,
                                      upscanJourneyRepository: UpscanJourneyRepository
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = stcAuthEnrolled.async { implicit request =>
    prepareUpload().map { response =>
      Ok(view(response))
    }
  }

  def remove(reference: String): Action[AnyContent] = stcAuthEnrolled.async { implicit request =>
    upscanJourneyRepository.delete(reference).map(_ => Redirect(routes.FileUploadController.onPageLoad()))
  }

  def onUploadError(): Action[AnyContent] = stcAuthEnrolled.async { implicit request =>

    val maybeError = UpscanUploadError.errorMessage(request)

    val removeOldDocument = request.getQueryString("key").map(upscanJourneyRepository.delete).getOrElse(Future.unit)

    for {
      _ <- removeOldDocument
      initiateResponse <- prepareUpload()
    } yield BadRequest(view(initiateResponse, maybeError))

  }

  def success(): Action[AnyContent] = Action {
    Ok("Upload successful")
  }

  def failure(): Action[AnyContent] = Action {
    Ok("Upload failed")
  }

  private def prepareUpload()(implicit request: Request[_]): Future[UpscanInitiateResponse] =
    for {
      initiateResponse <- upscanInitiateConnector.initiate()

      fileUpload = FileUpload(
        reference = initiateResponse.reference,
        status = UpscanJourneyStatus.Uploading
      )

      _ <- upscanJourneyRepository.insert(
        UpscanDocument(initiateResponse.reference, fileUpload)
      )

    } yield initiateResponse

  def mapFailureReason(reason: String): Action[AnyContent] =
    stcAuthEnrolled.async { implicit request =>
      val messages: Messages = messagesApi.preferred(request)
      val msg = FailureReason.fromString(reason).map {
        case FailureReason.Quarantine => messages("upscan.error.quarantine")
        case FailureReason.Unknown => messages("upscan.error.unknown")
        case FailureReason.NotCSV => messages("upscan.error.notCSV")
        case FailureReason.TooLarge => messages("upscan.error.tooLarge")
        case FailureReason.InvalidFileType => messages("upscan.error.invalidFileType")
        case FailureReason.InvalidArgument => messages("upscan.error.invalidArgument")
        case FailureReason.Rejected => messages("upscan.error.rejected")
      }


      for {
        initiateResponse <- prepareUpload()
      } yield BadRequest(view(initiateResponse, msg))
    }


}
