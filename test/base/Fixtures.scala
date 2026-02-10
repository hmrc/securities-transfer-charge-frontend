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

package base

import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.StcAuthorisedRequest

import scala.concurrent.{ExecutionContext, Future}

object Fixtures {

  val stcEnrolmentKey: String = "HMRC-STC-ORG"
  val stcIdentifierKey: String = "STCID"
  val testSubscriptionId: String = "XAST1234567890"
  val testInternalId = "test-user-808"
  val testSubmissionId: SubmissionId = SubmissionId("STC-424242424")

  val stcId: String = "STC1234567890"
  val affinityGroupIndividual: AffinityGroup.Individual.type = AffinityGroup.Individual


  implicit val hc: HeaderCarrier = HeaderCarrier()

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  
  val emptyEnrolments: Enrolments =
    Enrolments(Set.empty)

  val stcEnrolment: Enrolment =
    Enrolment(
      key = stcEnrolmentKey,
      identifiers = Seq(
        EnrolmentIdentifier(stcIdentifierKey, stcId)
      ),
      state = "Activated"
    )

  val enrolledForStc: Enrolments =
    Enrolments(Set(stcEnrolment))
  
  val individualAffinity: AffinityGroup = AffinityGroup.Individual
  val organisationAffinity: AffinityGroup = AffinityGroup.Organisation

  def fakeStcAuthRequest[A](request: Request[A],
                            internalId: String = testInternalId,
                            affinityGroup: AffinityGroup = affinityGroupIndividual,
                            stcId: String = stcId): StcAuthorisedRequest[A] = StcAuthorisedRequest[A](request,internalId,affinityGroup,stcId)

  val emptyUserAnswers: UserAnswers = UserAnswers.empty(testInternalId)(testSubmissionId)

  class FakeAuthConnectorSuccess(value: Any) extends AuthConnector {

    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
      Future.successful(value.asInstanceOf[A])
  }

  class FakeAuthConnectorFailing(ex: Throwable) extends AuthConnector {

    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
      Future.failed(ex)
  }
}
