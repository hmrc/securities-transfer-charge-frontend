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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers.pay

import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.PayApiConnector
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes as controllerRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{SubmissionId, SubscriptionId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.pay.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.{EtmpChargeDetail, SttTransfer}
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.EtmpSuccessResponseExtensions.*
import uk.gov.hmrc.securitiestransferchargefrontend.services.DashboardService

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class PaymentController @Inject()(
                                   val controllerComponents: MessagesControllerComponents,
                                   @Named("any") auth: StcAuthEnrolledAction,
                                   getData: StcDataRetrievalAction,
                                   dashboardService: DashboardService,
                                   payApiConnector: PayApiConnector,
                                   appConfig: FrontendAppConfig
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def startPayment(submissionId: String): Action[AnyContent] = (auth andThen getData).async { implicit request =>
    val subscriptionId = request.request.subscriptionId
    dashboardService.getSubmission(subscriptionId, SubmissionId(submissionId)).flatMap { sttSubmission =>
      sttSubmission.transfers.flatMap(basketItems(_, _ => true)) match {
        case items if items.nonEmpty => startJourney(subscriptionId, submissionId, items, appConfig.paymentBackUrl(submissionId))
        case _                       => nothingToPay(submissionId, "submission")
      }
    }
  }

  /** Pays every outstanding charge on one transfer ("Pay full balance" on the Transfer details page). */
  def startTransferPayment(submissionId: String, transferNumber: Int): Action[AnyContent] = auth.async { implicit request =>
    val subscriptionId = request.subscriptionId
    dashboardService.getSubmission(subscriptionId, SubmissionId(submissionId)).flatMap { sttSubmission =>
      sttSubmission.transferAt(transferNumber).map(basketItems(_, _ => true)) match {
        case Some(items) if items.nonEmpty =>
          startJourney(subscriptionId, submissionId, items, appConfig.paymentBackUrl(submissionId, transferNumber))
        case _ => nothingToPay(submissionId, s"transfer $transferNumber")
      }
    }
  }

  /** Pays a single charge on one transfer (a "Pay" link on the Transfer details page). */
  def startChargePayment(submissionId: String, transferNumber: Int, chargeRef: String): Action[AnyContent] =
    auth.async { implicit request =>
      val subscriptionId = request.subscriptionId
      dashboardService.getSubmission(subscriptionId, SubmissionId(submissionId)).flatMap { sttSubmission =>
        sttSubmission.transferAt(transferNumber).map(basketItems(_, _.chargeReference == chargeRef)) match {
          case Some(items) if items.nonEmpty =>
            startJourney(subscriptionId, submissionId, items, appConfig.paymentBackUrl(submissionId, transferNumber))
          case _ => nothingToPay(submissionId, s"charge $chargeRef on transfer $transferNumber")
        }
      }
    }

  // Only charges with something left to pay are sent to OPS.
  private def basketItems(transfer: SttTransfer, include: EtmpChargeDetail => Boolean): Seq[BasketItem] =
    transfer.charges.filter(charge => include(charge) && !charge.isPaid).map { charge =>
      BasketItem(
        buyerName = transfer.buyerNames,
        sellerName = transfer.sellerNames.getOrElse("Not provided"),
        chargeReference = charge.chargeReference,
        chargeType = mapToOPSChargeType(charge),
        amountInPence = charge.chargeAmountPending.setScale(2).underlying().movePointRight(2).longValueExact()
      )
    }

  // backUrl is the page the user paid from, so OPS's back link returns them there.
  private def startJourney(subscriptionId: SubscriptionId, submissionId: String, items: Seq[BasketItem], backUrl: String)
                          (implicit hc: HeaderCarrier): Future[Result] = {
    val paymentRequest = StartPaymentJourneyRequest(
      returnUrl = appConfig.paymentReturnUrl,
      backUrl = backUrl,
      customerId = subscriptionId.value,
      submissionId = submissionId,
      basketDetails = BasketDetails(items)
    )
    payApiConnector.startPaymentJourney(paymentRequest).map { response =>
      Redirect(response.nextUrl)
    }
  }

  private def nothingToPay(submissionId: String, what: String): Future[Result] = {
    logger.warn(s"Nothing to pay for $what in submission $submissionId")
    Future.successful(Redirect(controllerRoutes.JourneyRecoveryController.onPageLoad()))
  }

  private def mapToOPSChargeType(charge: EtmpChargeDetail): OPSChargeType = {
    if (charge.isLatePaymentPenalty || charge.isLateFilingPenalty) {
      OPSChargeType.LatePaymentPenalty
    } else if (charge.isLatePaymentInterest) {
      OPSChargeType.LatePaymentInterest
    } else {
      OPSChargeType.SecurityTransferCharge
    }
  }
}
