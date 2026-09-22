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

import play.api.Logging
import play.api.mvc.*
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.filters.RetrievalFilter
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.requests.StcAuthorisedRequest
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.{Redirects, routes}
import uk.gov.hmrc.securitiestransferchargefrontend.domain.CredentialId
import uk.gov.hmrc.securitiestransferchargefrontend.models.nrs.IdentityData

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait StcAuthEnrolledAction
  extends ActionBuilder[StcAuthorisedRequest, AnyContent]

@Singleton
final class StcAuthEnrolledActionImpl @Inject()(
                                                 override val authConnector: AuthConnector,
                                                 appConfig: FrontendAppConfig,
                                                 retrievalFilter: RetrievalFilter,
                                                 redirects: Redirects,
                                                 val parser: BodyParsers.Default
                                               )(implicit val executionContext: ExecutionContext)
  extends StcAuthEnrolledAction
    with AuthorisedFunctions
    with Logging {

  private val retrievals =
    Retrievals.internalId and
      Retrievals.externalId and
      Retrievals.agentCode and
      Retrievals.credentials and
      Retrievals.confidenceLevel and
      Retrievals.nino and
      Retrievals.saUtr and
      Retrievals.dateOfBirth and
      Retrievals.email and
      Retrievals.agentInformation and
      Retrievals.groupIdentifier and
      Retrievals.credentialRole and
      Retrievals.mdtpInformation and
      Retrievals.itmpName and
      Retrievals.itmpDateOfBirth and
      Retrievals.itmpAddress and
      Retrievals.affinityGroup and
      Retrievals.credentialStrength and
      Retrievals.loginTimes and
      Retrievals.groupIdentifier and
      Retrievals.allEnrolments
  
  private val getArn: AffinityGroup => Enrolments => Option[String] = {
    case AffinityGroup.Agent => es => getArnFromEnrolments(es)
    case _                   => _  => None
  }

  private val getArnFromEnrolments: Enrolments => Option[String] = es =>
    es
      .getEnrolment("HMRC-AS-AGENT")
      .flatMap(_.getIdentifier("AgentReferenceNumber"))
      .map(_.value)
  
  override def invokeBlock[A](
                               request: Request[A],
                               block: StcAuthorisedRequest[A] => Future[Result]
                             ): Future[Result] = {

    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised().retrieve(retrievals) {
      case maybeInternalId ~ maybeExternalId ~ maybeAgentCode ~ maybeCredentials ~ cl ~ maybeNino ~ maybeUtr ~ 
        maybeDoB ~ maybeEmail ~ agentInfo ~ maybeGroupId ~ maybeCredentialRole ~ maybeMdtpInfo ~ maybeItmpName ~
        maybeItmpDoB ~ maybeItmpAddress ~ maybeAffinityGroup ~ maybeCredentialStrength ~ loginTimes ~ 
        maybeGroupIdentifier ~ enrolments =>

        val identityData = IdentityData(
          maybeInternalId, maybeExternalId, maybeAgentCode, maybeCredentials, cl.level, maybeNino, maybeUtr, None,
            maybeDoB, maybeEmail, Some(agentInfo), maybeGroupId, maybeCredentialRole.map(_.toString), maybeMdtpInfo, maybeItmpName,
            maybeItmpDoB, maybeItmpAddress, maybeAffinityGroup.map(_.toString), maybeCredentialStrength, Some(loginTimes)
        )
        val maybeRequest =
          for {
            groupIdentifier <- retrievalFilter.isPresent(maybeGroupIdentifier)
            internalId      <- retrievalFilter.isPresent(maybeInternalId)
            affinityGroup   <- retrievalFilter.isPresent(maybeAffinityGroup)
            _               <- retrievalFilter.enrolledForStc(enrolments)
            subscriptionId  <- retrievalFilter.subscriptionIdPresent(enrolments)
            rawCredentialId <- retrievalFilter.providerIdPresentFilter(maybeCredentials)
            credentialId    =  CredentialId(rawCredentialId)
          } yield StcAuthorisedRequest(
            request,
            internalId,
            groupIdentifier,
            affinityGroup,
            subscriptionId,
            credentialId,
            identityData,
            getArn(affinityGroup)(enrolments)
          )

        maybeRequest.fold(identity, block)
    }.recover {

      case _: NoActiveSession =>
        redirects.redirectToLogin(appConfig.loginContinueUrl)

      case ae: AuthorisationException =>
        logger.warn(s"STC auth failed: ${ae.getMessage}")
        Redirect(routes.UnauthorisedController.onPageLoad())
    }
  }
}
