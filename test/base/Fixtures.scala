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

import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.AlfAddressConnector
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{SubmissionId, SubscriptionId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.{AlfAddress, AlfConfirmedAddress, ConfirmableAddress, Country, UploadedFileError}
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers

import scala.concurrent.{ExecutionContext, Future}

object Fixtures {

  val stcEnrolmentKey: String = "HMRC-STC-ORG"
  val stcIdentifierKey: String = "STCID"
  val testSubscriptionId: SubscriptionId = SubscriptionId("XAST1234567890")
  val testInternalId = "test-user-808"
  val testSubmissionId: SubmissionId = SubmissionId("STC-424242424")

  val affinityGroupIndividual: AffinityGroup.Individual.type = AffinityGroup.Individual


  implicit val hc: HeaderCarrier = HeaderCarrier()

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val emptyEnrolments: Enrolments =
    Enrolments(Set.empty)

  val stcEnrolment: Enrolment =
    Enrolment(
      key = stcEnrolmentKey,
      identifiers = Seq(
        EnrolmentIdentifier(stcIdentifierKey, testSubscriptionId.toString)
      ),
      state = "Activated"
    )

  val enrolledForStc: Enrolments =
    Enrolments(Set(stcEnrolment))
  
  val individualAffinity: AffinityGroup = AffinityGroup.Individual
  val organisationAffinity: AffinityGroup = AffinityGroup.Organisation
  
  val emptyUserAnswers: UserAnswers = UserAnswers.empty(testInternalId)(testSubmissionId)

  val fakeAlfAddress: AlfAddress = AlfAddress(
    List("1 high street", "bobbins on sea"), "ZZ1 1ZZ", Country("GB", "United Kingdom")
  )
  val fakeAlfConfirmedAddress: AlfConfirmedAddress = AlfConfirmedAddress(
    "foo", Some("bar"), fakeAlfAddress
  )

  val confirmableAddress: ConfirmableAddress = ConfirmableAddress(
    lines = List(
      "1 High Street",
      "Town"
    ),
    postcode = "ZZ1 1ZZ",
    country = Some(Country("United Kingdom", "GB"))
  )

  val uploadedFileErrors: Seq[UploadedFileError] = Seq(
    UploadedFileError(cell = "J6", error = "The seller's name cannot contain numbers"),
    UploadedFileError(cell = "J36", error = "You have selected that the buyer is a company, you need to enter the registered address"),
    UploadedFileError(cell = "K3", error = "Buyer's country can only contain letters, numbers and hyphens")
  )

  class FakeAuthConnectorSuccess(value: Any) extends AuthConnector {

    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
      Future.successful(value.asInstanceOf[A])
  }

  class FakeAuthConnectorFailing(ex: Throwable) extends AuthConnector {

    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
      Future.failed(ex)
  }

  class FakeAlfConnector extends AlfAddressConnector {

    override def alfRetrieveAddress(key: String)(implicit hc: HeaderCarrier): Future[AlfConfirmedAddress] = Future.successful(fakeAlfConfirmedAddress)

    override def initAlfJourneyRequest(configFileLocation: String, returnUrl: String)(implicit hc: HeaderCarrier): Future[Result] = Future.successful(Redirect("/alf"))
  }
}
