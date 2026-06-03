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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.filters

import play.api.mvc.Result
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.Redirects
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubscriptionId

import javax.inject.Inject
import scala.concurrent.Future

type RetrievalFilterResult[A] = Either[Future[Result], A]
type RetrievalFilterFunction[A, B] = A => RetrievalFilterResult[B]


class RetrievalFilter @Inject()(
                                 appConfig: FrontendAppConfig,
                                 redirects: Redirects
                               ) {

  import redirects.*

  def isPresent[A]: RetrievalFilterFunction[Option[A], A] =
    case Some(a) => Right(a)
    case None => Left(redirectToUnauthorisedF)

  val enrolledForStc: RetrievalFilterFunction[Enrolments, Unit] = enrolments =>
    enrolments
      .getEnrolment(appConfig.stcEnrolmentKey)
      .filter(_.isActivated)
      .map(_ => Right(()))
      .getOrElse(Left(redirectToRegistrationF))

  val subscriptionIdPresent: RetrievalFilterFunction[Enrolments, SubscriptionId] = enrolments =>
    enrolments
      .getEnrolment(appConfig.stcEnrolmentKey)
      .flatMap(_.getIdentifier(appConfig.stcIdentifierKey))
      .map(id => SubscriptionId(id.value))
      .map(Right(_))
      .getOrElse(Left(redirectToJourneyRecoveryF))

  val providerIdPresentFilter: RetrievalFilterFunction[Option[Credentials], String] = {
    case Some(Credentials(providerId, _)) => Right(providerId)
    case _ => Left(redirectToUnauthorisedF)
  }

}
