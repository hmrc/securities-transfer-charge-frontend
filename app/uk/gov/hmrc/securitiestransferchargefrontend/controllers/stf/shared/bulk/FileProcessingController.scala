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
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes.JourneyRecoveryController
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.{FileUpload, UpscanJourneyStatus}
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.UpscanJourneyRepository
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.processing.FileProcessingHelper.*
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.processing.{FileProcessingRefreshCounter, FileProcessingRefreshCounterFactory, ProcessingService}
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.shared.bulk.{BulkUploadErrorView, FileProcessingView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class FileProcessingController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          stcAuthEnrolled: StcAuthEnrolledAction,
                                          spinnerView: FileProcessingView,
                                          timeoutView: BulkUploadErrorView,
                                          createCounter: FileProcessingRefreshCounterFactory,
                                          upscanJourneyRepository: UpscanJourneyRepository,
                                          val controllerComponents: MessagesControllerComponents,
                                          appConfig: FrontendAppConfig,
                                          processingService: ProcessingService
                                        )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  private val refreshInterval = appConfig.spinnerPageRefreshInterval

  def onPageLoad(reference: String): Action[AnyContent] =
    stcAuthEnrolled.async { implicit request =>

      val affinityKey = request.affinityGroupKey

      val counter = createCounter(request)

      if (counter.isTimedOut) {
        
        val result = Redirect(routes.FileProcessingController.onTimeout())
        Future.successful(counter.reset(result))

      } else {
        upscanJourneyRepository.find(reference).flatMap {
          case Some(fileUpload) => handleStatus(reference, fileUpload, counter, affinityKey)

          case None => Future.successful(Redirect(JourneyRecoveryController.onPageLoad()))
        }
      }
    }

  def onTimeout(): Action[AnyContent] =
    stcAuthEnrolled { implicit request =>
      Ok(timeoutView())
    }

  private def spinnerPage(counter: FileProcessingRefreshCounter)(implicit request: StcAuthorisedRequest[_]) =
    counter.withIncrementedCounter(Ok(spinnerView(refreshInterval)))


  private def handleStatus(
                            reference: String,
                            fileUpload: FileUpload,
                            counter: FileProcessingRefreshCounter,
                            affinityKey: String
                          )(implicit request: StcAuthorisedRequest[_]): Future[Result] =
    fileUpload.status match {

      case UpscanJourneyStatus.Ready => processingService.processReadyUpload(reference, fileUpload, affinityKey)
        Future.successful(spinnerPage(counter))

      case UpscanJourneyStatus.Processing | UpscanJourneyStatus.Initiated => Future.successful(spinnerPage(counter))
      
      case UpscanJourneyStatus.RowLimitExceeded => Future.successful(Redirect(routes.BulkRowsErrorController.onPageLoad()))

      case UpscanJourneyStatus.EmptyFile => Future.successful(Redirect(routes.BulkUploadFileEmptyController.onPageLoad()))

      case UpscanJourneyStatus.TooManyErrors => Future.successful(Redirect(routes.FormattingErrorController.onPageLoad()))

      case UpscanJourneyStatus.FormatingErrors => Future.successful(Redirect(routes.UploadedFileErrorController.onPageLoad(reference)))

      case UpscanJourneyStatus.UpscanDownloadError => Future.successful(Redirect(routes.BulkUploadErrorController.onPageLoad()))

      case UpscanJourneyStatus.InvalidTemplate => Future.successful(Redirect(routes.BulkUploadInvalidTemplateController.onPageLoad()))

      case UpscanJourneyStatus.FileParseError => Future.successful(Redirect(JourneyRecoveryController.onPageLoad()))

      case UpscanJourneyStatus.Failed if isEncryptedFailure(fileUpload) => Future.successful(Redirect(routes.EncryptedFileErrorController.onPageLoad()))

      case UpscanJourneyStatus.Failed if isVirusFailure(fileUpload) => Future.successful(Redirect(routes.BulkUploadVirusErrorController.onPageLoad()))

      case UpscanJourneyStatus.Failed  => Future.successful(Redirect(routes.BulkUploadErrorController.onPageLoad()))

      case UpscanJourneyStatus.Completed => Future.successful(Redirect(JourneyRecoveryController.onPageLoad()))
      
    }
}