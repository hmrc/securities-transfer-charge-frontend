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

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.SubscriptionConnector
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes.JourneyRecoveryController
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.FileParseError
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.{FileUpload, UpscanJourneyStatus}
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.{UpscanJourneyRepository, ValidationErrorRepository}
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcUpscanProcessingService
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.shared.bulk.FileProcessingView

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FileProcessingController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          stcAuthEnrolled: StcAuthEnrolledAction,
                                          upscanJourneyRepository: UpscanJourneyRepository,
                                          stcUpscanProcessingService: StcUpscanProcessingService,
                                          validationErrorRepository: ValidationErrorRepository,
                                          subscriptionConnector: SubscriptionConnector,
                                          view: FileProcessingView,
                                          appConfig: FrontendAppConfig,
                                          val controllerComponents: MessagesControllerComponents
                                        )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {


  def onPageLoad(reference: String): Action[AnyContent] =
    stcAuthEnrolled.async { implicit request =>

      val now = Instant.now()

      val startedAt = request.session.get("file-processing-start").map(Instant.parse).getOrElse(now)

      val timedOut = startedAt.plus(appConfig.fileProcessingTimeout).isBefore(now)

      if (timedOut) {

        Future.successful(
          Redirect(JourneyRecoveryController.onPageLoad())
            .removingFromSession("file-processing-start")
        )

      } else {

        upscanJourneyRepository.find(reference).flatMap {

          case None =>
            Future.successful(
              Redirect(JourneyRecoveryController.onPageLoad())
                .removingFromSession("file-processing-start")
            )

          case Some(fileUpload) =>
            fileUpload.status match {
              case UpscanJourneyStatus.Failed if fileUpload.failureReason.contains("QUARANTINE") &&
                fileUpload.message.exists(_.contains("EncryptedDoc")) =>
                Future.successful(Redirect(routes.EncryptedFileErrorController.onPageLoad()))

              case UpscanJourneyStatus.Failed if fileUpload.failureReason.contains("QUARANTINE") &&
                fileUpload.message.exists(_.toLowerCase().contains("virus")) =>
                Future.successful(Redirect(routes.BulkUploadVirusErrorController.onPageLoad()))

              case UpscanJourneyStatus.Ready =>
                processReadyUpload(reference, fileUpload)
                  .map(_.removingFromSession("file-processing-start"))

              case UpscanJourneyStatus.Processing =>
                Future.successful(
                  Ok(view())
                    .addingToSession(
                      "file-processing-start" -> startedAt.toString
                    )
                )
              case _ =>
                Future.successful(
                  Ok(view())
                    .addingToSession(
                      "file-processing-start" -> startedAt.toString
                    )
                )
            }
        }
      }
    }

  private def processReadyUpload(
                                  reference: String,
                                  fileUpload: FileUpload
                                )(implicit request: StcAuthorisedRequest[_]): Future[Result] = {

    upscanJourneyRepository.updateStatus(reference, UpscanJourneyStatus.Processing).flatMap { _ =>

      stcUpscanProcessingService.process(fileUpload).flatMap {

        case Left(FileParseError.EmptyFile) =>
          Future.successful(
            Redirect(routes.BulkUploadFileEmptyController.onPageLoad())
          )

        case Left(_: FileParseError) =>

          Future.successful(
            Redirect(routes.FormattingErrorController.onPageLoad())
          )

        case Right(validationResponse)
          if validationResponse.tooManyBlockingErrors =>

          Future.successful(
            Redirect(routes.FormattingErrorController.onPageLoad())
          )

        case Right(validationResponse)
          if validationResponse.hasBlockingErrors =>

          validationErrorRepository
            .save(reference, validationResponse.blockingErrors)
            .map { _ =>
              Redirect(
                routes.UploadedFileErrorController.onPageLoad(reference)
              )
            }

        case Right(_) =>

          subscriptionConnector
            .getAndStoreSubscription(request.subscriptionId)
            .map { _ =>
              Redirect(
                routes.FileUploadedController.onPageLoad(reference)
              )
            }
            .recover {
              case _ =>
                Redirect(JourneyRecoveryController.onPageLoad())
            }
      }
    }
  }
}