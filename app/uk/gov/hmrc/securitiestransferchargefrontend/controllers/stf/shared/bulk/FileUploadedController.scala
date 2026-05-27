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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.bulk

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.{SubscriptionConnector, UpscanDownloadException}
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.{StcAuthEnrolledAction, StcAuthorisedRequest}
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes.JourneyRecoveryController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.bulk.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.FileParseError
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.{FileUpload, UpscanJourneyStatus}
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.{UpscanJourneyRepository, ValidationErrorRepository}
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcUpscanProcessingService
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.shared.bulk.FileUploadedView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FileUploadedController @Inject()(
                                        val controllerComponents: MessagesControllerComponents,
                                        upscanJourneyRepository: UpscanJourneyRepository,
                                        stcAuthEnrolled: StcAuthEnrolledAction,
                                        stcUpscanProcessingService: StcUpscanProcessingService,
                                        subscriptionConnector: SubscriptionConnector,
                                        validationErrorRepository: ValidationErrorRepository,
                                        view: FileUploadedView
                                      )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport {

  def onPageLoad(reference: String): Action[AnyContent] =
    stcAuthEnrolled.async { implicit request =>

      val affinityKey = request.affinityGroupKey

      upscanJourneyRepository.find(reference).flatMap {
        case Some(fileUpload) if fileUpload.status == UpscanJourneyStatus.Ready =>
          processReadyUpload(reference, fileUpload,affinityKey)

        case Some(fileUpload) if fileUpload.status == UpscanJourneyStatus.Failed &&
          fileUpload.failureReason.contains("QUARANTINE") &&
          fileUpload.message.exists(_.contains("EncryptedDoc")) =>
          Future.successful(Redirect(routes.EncryptedFileErrorController.onPageLoad()))


        case Some(fileUpload) if fileUpload.status == UpscanJourneyStatus.Failed &&
          fileUpload.failureReason.contains("QUARANTINE") &&
          fileUpload.message.exists(_.toLowerCase().contains("virus")) =>
          Future.successful(Redirect(routes.BulkUploadVirusErrorController.onPageLoad()))

        case Some(fileUpload) =>
          Future.successful(Ok(view(fileUpload)))

        case None =>
          Future.successful(Redirect(JourneyRecoveryController.onPageLoad()))
      }
    }

  private def processReadyUpload(reference: String, fileUpload: FileUpload, affinityKey:String)(implicit request: StcAuthorisedRequest[_]): Future[Result] =
    stcUpscanProcessingService.process(fileUpload,affinityKey).flatMap {

      case Left(_: FileParseError.RowLimitExceeded) =>
        Future.successful(
          Redirect(routes.BulkRowsErrorController.onPageLoad())
        )

      case Left(FileParseError.EmptyFile) =>
        Future.successful(
          Redirect(routes.BulkUploadFileEmptyController.onPageLoad())
        )

      case Right(validationResponse) if validationResponse.tooManyBlockingErrors =>
        Future.successful(
          Redirect(routes.FormattingErrorController.onPageLoad())
        )

      case Right(validationResponse) if validationResponse.hasBlockingErrors =>
        validationErrorRepository
          .save(reference, validationResponse.blockingErrors)
          .map { _ =>
            Redirect(
              routes.UploadedFileErrorController.onPageLoad(reference)
            )
          }


      case Right(_) =>
        subscriptionConnector.getAndStoreSubscription(request.subscriptionId)
          .map(_ => Ok(view(fileUpload)))
          .recover {
            case _ => Redirect(JourneyRecoveryController.onPageLoad())
          }
      case _ => Future.successful(Redirect(JourneyRecoveryController.onPageLoad()))
    }.recover {
      case _: UpscanDownloadException =>
        Redirect(routes.BulkUploadErrorController.onPageLoad())
    }
}