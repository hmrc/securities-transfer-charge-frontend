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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions

import com.google.inject.{Inject, Singleton}
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.filters.RetrievalFilter
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.{Redirects, routes}
import uk.gov.hmrc.securitiestransferchargefrontend.domain.CredentialId
import uk.gov.hmrc.securitiestransferchargefrontend.models.requests.StcIndividualAuthorisedRequest

import scala.concurrent.{ExecutionContext, Future}

trait StcIndividualAuthEnrolledAction
  extends ActionBuilder[StcIndividualAuthorisedRequest, AnyContent]

@Singleton
final class StcIndividualAuthEnrolledActionImpl @Inject()(
                                                           override val authConnector: AuthConnector,
                                                           appConfig: FrontendAppConfig,
                                                           retrievalFilter: RetrievalFilter,
                                                           redirects: Redirects,
                                                           val parser: BodyParsers.Default
                                                         )(implicit val executionContext: ExecutionContext)
  extends StcIndividualAuthEnrolledAction
    with AuthorisedFunctions
    with Logging {

  private val retrievals =
    Retrievals.internalId and
      Retrievals.groupIdentifier and
      Retrievals.allEnrolments and
      Retrievals.credentials and
      Retrievals.itmpName

  override def invokeBlock[A](
                               request: Request[A],
                               block: StcIndividualAuthorisedRequest[A] => Future[Result]
                             ): Future[Result] = {

    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised(AffinityGroup.Individual).retrieve(retrievals) {
      case maybeInternalId ~
        maybeGroupIdentifier ~
        enrolments ~
        maybeCredentials ~
        maybeItmpName =>

        val maybeRequest =
          for {
            internalId <- retrievalFilter.isPresent(maybeInternalId)
            groupIdentifier <- retrievalFilter.isPresent(maybeGroupIdentifier)
            _ <- retrievalFilter.enrolledForStc(enrolments)
            subscriptionId <- retrievalFilter.subscriptionIdPresent(enrolments)
            rawCredentialId <- retrievalFilter.providerIdPresentFilter(maybeCredentials)
            credentialId = CredentialId(rawCredentialId)
            name <- retrievalFilter.namePresentFilter(maybeItmpName)
          } yield StcIndividualAuthorisedRequest(
            request,
            internalId,
            groupIdentifier,
            subscriptionId,
            credentialId,
            name
          )

        maybeRequest.fold(identity, block)
    }.recover {

      case _: NoActiveSession =>
        redirects.redirectToLogin(appConfig.loginContinueUrl)

      case ua: UnsupportedAffinityGroup  =>
        logger.warn(ua.getMessage)
        Redirect(routes.JourneyRecoveryController.onPageLoad())

      case ae: AuthorisationException =>
        logger.warn(s"STC individual auth failed: ${ae.getMessage}")
        Redirect(routes.UnauthorisedController.onPageLoad())
    }
  }
}