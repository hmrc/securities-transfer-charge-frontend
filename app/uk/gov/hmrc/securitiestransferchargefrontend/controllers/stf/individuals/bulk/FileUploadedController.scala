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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.SubscriptionConnector
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.{StcAuthEnrolledAction, StcAuthorisedRequest}
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.bulk.routes as individualRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.{FileUpload, UpscanJourneyStatus}
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.UpscanJourneyRepository
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcUpscanProcessingService
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.individuals.bulk.FileUploadedView
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.FileParseError

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FileUploadedController @Inject()(
                                        val controllerComponents: MessagesControllerComponents,
                                        upscanJourneyRepository: UpscanJourneyRepository,
                                        stcAuthEnrolled: StcAuthEnrolledAction,
                                        stcUpscanProcessingService: StcUpscanProcessingService,
                                        subscriptionConnector: SubscriptionConnector,
                                        view: FileUploadedView
                                      )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport {

  def onPageLoad(reference: String): Action[AnyContent] =
    stcAuthEnrolled.async { implicit request =>
      upscanJourneyRepository.find(reference).flatMap {
        case Some(fileUpload) if fileUpload.status == UpscanJourneyStatus.Ready =>
          processReadyUpload(reference, fileUpload)

        case Some(fileUpload) =>
          Future.successful(Ok(view(fileUpload)))

        case None =>
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  private def processReadyUpload(reference: String, fileUpload: FileUpload)(implicit request: StcAuthorisedRequest[_]): Future[Result] =
    stcUpscanProcessingService.process(fileUpload).flatMap {
      case Left(_: FileParseError) =>
        Future.successful(
          Redirect(individualRoutes.FormattingErrorController.onPageLoad())
        )

      case Right(validationResponse) if validationResponse.tooManyBlockingErrors =>
        Future.successful(
          Redirect(individualRoutes.FormattingErrorController.onPageLoad())
        )

      case Right(validationResponse) if validationResponse.hasBlockingErrors =>
        Future.successful(
          Redirect(individualRoutes.UploadedFileErrorController.onPageLoad(reference))
        )

      case Right(_) =>
        subscriptionConnector.getAndStoreSubscription(request.subscriptionId)
          .map(_ => Ok(view(fileUpload)))
          .recover {
            case _ => Redirect(routes.JourneyRecoveryController.onPageLoad())
          }
    }
}