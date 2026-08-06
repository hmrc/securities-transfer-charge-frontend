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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.fileUpload.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes.JourneyRecoveryController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.agents.bulk.routes as stfBulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.agents.bulk.routes as sh03BulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.organisations.bulk.routes as sh03OrgBulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.shared.bulk.routes as sh03CyaRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.bulk.routes as stfCyaRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.{JourneyType, NormalMode}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.{FileUpload, UpscanJourneyStatus}
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.UpscanJourneyRepository
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.processing.FileProcessingHelper.*
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.processing.{FileProcessingRefreshCounter, FileProcessingRefreshCounterFactory, ProcessingService}
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.fileUpload.{BulkUploadErrorView, FileProcessingView}

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

        val result = Redirect(routes.FileProcessingController.onTimeout(reference))
        Future.successful(counter.reset(result))

      } else {
        upscanJourneyRepository.find(reference).flatMap {
          case Some(fileUpload) => handleStatus(reference, fileUpload, counter, affinityKey)

          case None => Future.successful(Redirect(JourneyRecoveryController.onPageLoad()))
        }
      }
    }

  def onTimeout(reference: String): Action[AnyContent] =
    stcAuthEnrolled.async { implicit request =>
      upscanJourneyRepository.find(reference).map {
        case Some(fileUpload) =>
          Ok(timeoutView(fileUpload.journeyType))

        case None =>
          Redirect(JourneyRecoveryController.onPageLoad())
      }
    }

  private def spinnerPage(counter: FileProcessingRefreshCounter)(implicit request: StcAuthorisedRequest[_]) =
    counter.withIncrementedCounter(Ok(spinnerView(refreshInterval)))


  private def handleStatus(
                            reference: String,
                            fileUpload: FileUpload,
                            counter: FileProcessingRefreshCounter,
                            affinityKey: String
                          )(implicit request: StcAuthorisedRequest[_]): Future[Result] = {
    val journeyType = fileUpload.journeyType
    fileUpload.status match {

      case UpscanJourneyStatus.Ready if fileUpload.uploadDetails.exists(_.size == 0) =>
        Future.successful(Redirect(routes.BulkUploadFileEmptyController.onPageLoad(journeyType)))

      case UpscanJourneyStatus.Ready =>
        processingService.processReadyUpload(reference, fileUpload, affinityKey, journeyType)
        Future.successful(spinnerPage(counter))

      case UpscanJourneyStatus.Processing | UpscanJourneyStatus.Initiated => Future.successful(spinnerPage(counter))

      case UpscanJourneyStatus.RowLimitExceeded => Future.successful(Redirect(routes.BulkRowsErrorController.onPageLoad(journeyType)))

      case UpscanJourneyStatus.EmptyFile => Future.successful(Redirect(routes.BulkUploadFileEmptyController.onPageLoad(journeyType)))

      case UpscanJourneyStatus.TooManyErrors => Future.successful(Redirect(routes.FormattingErrorController.onPageLoad(journeyType)))

      case UpscanJourneyStatus.FormatingErrors => Future.successful(Redirect(routes.UploadedFileErrorController.onPageLoad(reference, journeyType)))

      case UpscanJourneyStatus.UpscanDownloadError => Future.successful(Redirect(routes.BulkUploadErrorController.onPageLoad(journeyType)))

      case UpscanJourneyStatus.InvalidTemplate => Future.successful(Redirect(routes.BulkUploadInvalidTemplateController.onPageLoad(journeyType)))

      case UpscanJourneyStatus.FileParseError => Future.successful(Redirect(JourneyRecoveryController.onPageLoad()))

      case UpscanJourneyStatus.Failed if isEncryptedFailure(fileUpload) => Future.successful(Redirect(routes.EncryptedFileErrorController.onPageLoad(journeyType)))

      case UpscanJourneyStatus.Failed if isVirusFailure(fileUpload) => Future.successful(Redirect(routes.BulkUploadVirusErrorController.onPageLoad(journeyType)))

      case UpscanJourneyStatus.Failed if isInvalidFileTypeFailure(fileUpload) => Future.successful(Redirect(routes.FileTypeErrorController.onPageLoad(journeyType)))

      case UpscanJourneyStatus.Failed => Future.successful(Redirect(routes.BulkUploadErrorController.onPageLoad(journeyType)))

      case UpscanJourneyStatus.Completed =>
        (request.affinityGroup, journeyType) match {
          case (AffinityGroup.Agent, JourneyType.STF) => Future.successful(Redirect(stfBulkRoutes.AgentReferenceController.onPageLoad(NormalMode)))

          case (AffinityGroup.Agent, JourneyType.SH03) => Future.successful(Redirect(sh03BulkRoutes.RoleAtPurchasingCompanyController.onPageLoad(NormalMode)))

          case (AffinityGroup.Organisation, JourneyType.SH03) => Future.successful(Redirect(sh03OrgBulkRoutes.RoleAtPurchasingCompanyController.onPageLoad(NormalMode)))

          case (_, JourneyType.SH03) => Future.successful(Redirect(sh03CyaRoutes.CheckYourAnswersController.onPageLoad()))
          case (_, JourneyType.STF) => Future.successful(Redirect(stfCyaRoutes.CheckYourAnswersController.onPageLoad()))
        }
    }
  }
}