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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers.dashboard

import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.GetTransactionResult
import uk.gov.hmrc.securitiestransferchargefrontend.services.{DashboardService, EtmpSubmissionService}
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.dashboard.TransferDetailsViewModel
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.dashboard.TransferDetailsView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class TransferDetailsController @Inject()(
                                           override val messagesApi: MessagesApi,
                                           @Named("any") auth: StcAuthEnrolledAction,
                                           getData: StcDataRetrievalAction,
                                           val controllerComponents: MessagesControllerComponents,
                                           view: TransferDetailsView,
                                           dashboardService: DashboardService,
                                           etmpSubmissionService: EtmpSubmissionService
                                         )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad(submissionId: String, transferNumber: Int): Action[AnyContent] = (auth andThen getData).async {
    implicit request =>
      val subscriptionId = request.request.subscriptionId
      lazy val journeyRecovery = Redirect(routes.JourneyRecoveryController.onPageLoad())

      dashboardService.getSubmission(subscriptionId, SubmissionId(submissionId)).flatMap { submission =>
        submission.transferAt(transferNumber) match {
          case None =>
            logger.warn(s"Submission $submissionId has no transfer number $transferNumber")
            Future.successful(journeyRecovery)
          case Some(transfer) =>
            etmpSubmissionService.getTransaction(subscriptionId, transfer.utrn).map {
              case GetTransactionResult.Found(transaction) =>
                Ok(view(TransferDetailsViewModel.build(submission, transferNumber, transfer, transaction)))
              case notFound =>
                logger.warn(s"Could not get transfer $transferNumber in submission $submissionId: $notFound")
                journeyRecovery
            }
        }
      }
  }
}
