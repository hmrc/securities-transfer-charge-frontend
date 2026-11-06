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

package base.stubs

import base.Fixtures
import base.Fixtures.*
import play.api.mvc.*
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.StcAuthEnrolledAction
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.requests.{AgentUniqueIdentifier, IndividualUniqueIdentifier, OrganisationUniqueIdentifier, StcAuthorisedRequest, UniqueIdentifier}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
abstract class StubStcAuthEnrolledAction @Inject() (
                                            playBodyParsers: PlayBodyParsers
                                          ) extends StcAuthEnrolledAction {

  protected val affinityGroup: AffinityGroup

  protected val uniqueIdentifier: UniqueIdentifier =
    IndividualUniqueIdentifier(Fixtures.testNino)

  override val parser: BodyParser[AnyContent] =
    playBodyParsers.defaultBodyParser

  override protected val executionContext: ExecutionContext =
    ExecutionContext.global

  override def invokeBlock[A](
                               request: Request[A],
                               block: StcAuthorisedRequest[A] => Future[Result]
                             ): Future[Result] =
    block(
      StcAuthorisedRequest(
        request,
        testInternalId.value,
        testGroupIdentifier.value,
        affinityGroup,
        testSubscriptionId,
        testCredentialId,
        testIdentityData,
        uniqueIdentifier
      )
    )
}

class IndividualStubStcAuthEnrolledAction @Inject() (playBodyParsers: PlayBodyParsers) extends StubStcAuthEnrolledAction(playBodyParsers) {

  protected val affinityGroup: AffinityGroup = AffinityGroup.Individual
  override val expectedAffinityGroup: Option[AffinityGroup] = Some(affinityGroup)
  override val uniqueIdentifier: UniqueIdentifier = IndividualUniqueIdentifier(Fixtures.testNino)
}

class OrganisationStubStcAuthEnrolledAction @Inject() (playBodyParsers: PlayBodyParsers) extends StubStcAuthEnrolledAction(playBodyParsers) {

  protected val affinityGroup: AffinityGroup = AffinityGroup.Organisation
  override val expectedAffinityGroup: Option[AffinityGroup] = Some(affinityGroup)
  override val uniqueIdentifier: UniqueIdentifier = OrganisationUniqueIdentifier(Some(Fixtures.testUtr))
}

class AgentStubStcAuthEnrolledAction @Inject() (playBodyParsers: PlayBodyParsers) extends StubStcAuthEnrolledAction(playBodyParsers) {

  protected val affinityGroup: AffinityGroup = AffinityGroup.Agent
  override val expectedAffinityGroup: Option[AffinityGroup] = Some(affinityGroup)
  override val uniqueIdentifier: UniqueIdentifier = AgentUniqueIdentifier(Some(Fixtures.testArn))

}

class AnyStubStcAuthEnrolledAction @Inject() (playBodyParsers: PlayBodyParsers, affinity: AffinityGroup) extends StubStcAuthEnrolledAction(playBodyParsers) {

  protected val affinityGroup: AffinityGroup = affinity
  override val expectedAffinityGroup: Option[AffinityGroup] = Some(affinity)
  override val uniqueIdentifier: UniqueIdentifier = affinityGroup match {
    case Agent => AgentUniqueIdentifier(Some(Fixtures.testArn))
    case Individual => IndividualUniqueIdentifier(Fixtures.testNino)
    case Organisation => OrganisationUniqueIdentifier(Some(Fixtures.testUtr))
  }
}
