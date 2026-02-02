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

import javax.inject.{Inject, Singleton}
import play.api.Logging
import play.api.mvc.*
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.auth.core.retrieve.~

import scala.concurrent.{ExecutionContext, Future}

trait StcAuthEnrolledAction
  extends ActionBuilder[StcAuthorisedRequest, AnyContent]

@Singleton
final class StcAuthEnrolledActionImpl @Inject()(
                                                 override val authConnector: AuthConnector,
                                                 appConfig: FrontendAppConfig,
                                                 val parser: BodyParsers.Default
                                               )(implicit val executionContext: ExecutionContext)
  extends StcAuthEnrolledAction
    with AuthorisedFunctions
    with Logging {

  private val retrievals =
    Retrievals.internalId and
      Retrievals.authorisedEnrolments and
      Retrievals.affinityGroup

  override def invokeBlock[A](
                               request: Request[A],
                               block: StcAuthorisedRequest[A] => Future[Result]
                             ): Future[Result] = {

    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    def extractStcId(enrolments: Enrolments): Option[String] =
      for {
        enrolment  <- enrolments.getEnrolment(appConfig.stcEnrolmentKey)
        identifier <- enrolment.getIdentifier(appConfig.stcIdentifierKey)
        if identifier.value.nonEmpty
      } yield identifier.value

    authorised(
      AuthProviders(GovernmentGateway) and
        ConfidenceLevel.L250 and
        Enrolment(appConfig.stcEnrolmentKey)
    )
      .retrieve(retrievals) {
        case maybeInternalId ~ enrolments ~ maybeAffinityGroup =>

          val internalId =
            maybeInternalId.getOrElse {
              logger.warn("STC auth: missing internalId from auth retrievals")
              throw new UnauthorizedException("Missing internalId")
            }

          val affinityGroup =
            maybeAffinityGroup.getOrElse {
              logger.warn("STC auth: missing affinityGroup from auth retrievals")
              throw new UnauthorizedException("Missing affinityGroup")
            }

          extractStcId(enrolments) match {
            case Some(stcId) =>
              block(
                StcAuthorisedRequest(
                  request = request,
                  internalId = internalId,
                  affinityGroup = affinityGroup,
                  stcId = stcId
                )
              )

            case None =>
              logger.warn(
                s"STC auth: user [$internalId] has active STC enrolment " +
                  s"but missing identifier [${appConfig.stcIdentifierKey}]"
              )
              Future.successful(
                Redirect(routes.UnauthorisedController.onPageLoad())
              )
          }
      }
      .recover {
        case _: NoActiveSession =>
          Redirect(
            appConfig.loginUrl,
            Map("continue" -> Seq(appConfig.loginContinueUrl))
          )

        case ae: AuthorisationException =>
          logger.info(
            s"STC auth: authorisation failure for request " +
              s"[${request.method} ${request.uri}]: ${ae.getMessage}"
          )
          Redirect(routes.UnauthorisedController.onPageLoad())
      }
  }
}