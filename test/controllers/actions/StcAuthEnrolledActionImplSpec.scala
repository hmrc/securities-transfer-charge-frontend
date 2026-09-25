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

package controllers.actions

import base.Fixtures.{FakeAuthConnectorFailing, FakeAuthConnectorSuccess}
import base.{Fixtures, SpecBase}
import play.api.Application
import play.api.mvc.*
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.*
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.filters.RetrievalFilter
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.{StcAuthEnrolledAction, StcAuthEnrolledActionImpl}
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.{Redirects, routes}

import java.time.{Instant, LocalDate}
import scala.concurrent.Future

class StcAuthEnrolledActionImplSpec extends SpecBase {

  type RetrievalType =
    Option[String] ~ Option[String] ~ Option[String] ~ Option[Credentials] ~ ConfidenceLevel ~ Option[String] ~ Option[String] ~ Option[LocalDate] ~ Option[String] ~ AgentInformation ~ Option[String] ~ Option[CredentialRole] ~ Option[MdtpInformation] ~ Option[ItmpName] ~ Option[LocalDate] ~ Option[ItmpAddress] ~ Option[AffinityGroup] ~ Option[String] ~ LoginTimes ~ Option[String] ~ Enrolments


  private val enrolmentKey = "HMRC-STC-ORG"
  private val identifierKey = "STCID"

  val externalId: Option[String] = Fixtures.testExternalId
  val agentCode: Option[String] = None
  val confidenceLevel: ConfidenceLevel = ConfidenceLevel.L500
  val nino: Option[String] = None
  val saUtr: Option[String] = None
  val dateOfBirth: Option[LocalDate] = None
  val email: Option[String] = None
  val agentInformation = AgentInformation(None, None, None)
  val credentialRole: Option[CredentialRole] = None
  val mdtpInformation: Option[MdtpInformation] = None
  val itmpName: Option[ItmpName] = None
  val itmpDateOfBirth: Option[LocalDate] = None
  val itmpAddress: Option[ItmpAddress] = None
  val credentialStrength: Option[String] = None
  val loginTimes = LoginTimes(Instant.now(), None)

  def buildRetrieval(
                      maybeInternalId: Option[String] = Some(Fixtures.testInternalId.value),
                      maybeGroupIdentifier: Option[String] = Some(Fixtures.testGroupIdentifier.value),
                      enrolments: Enrolments = Fixtures.enrolledForStc,
                      maybeAffinityGroup: Option[AffinityGroup] = Some(AffinityGroup.Organisation),
                      maybeCredentials: Option[Credentials] = Some(Credentials(Fixtures.testCredentialId.value, "providerType"))
                    ): RetrievalType =

    new~(
      new~(
        new~(
          new~(
            new~(
              new~(
                new~(
                  new~(
                    new~(
                      new~(
                        new~(
                          new~(
                            new~(
                              new~(
                                new~(
                                  new~(
                                    new~(
                                      new~(
                                        new~(
                                          new~(maybeInternalId, externalId),
                                          agentCode),
                                        maybeCredentials),
                                      confidenceLevel),
                                    nino),
                                  saUtr),
                                dateOfBirth),
                              email),
                            agentInformation),
                          maybeGroupIdentifier),
                        credentialRole),
                      mdtpInformation),
                    itmpName),
                  itmpDateOfBirth),
                itmpAddress),
              maybeAffinityGroup),
            credentialStrength),
          loginTimes),
        maybeGroupIdentifier),
      enrolments)

  def testSetup(
                 application: Application,
                 retrievals: RetrievalType
               )(
                 authConnector: AuthConnector =
                 new FakeAuthConnectorSuccess(retrievals)
               ): StcAuthEnrolledAction = {

    val appConfig = application.injector.instanceOf[FrontendAppConfig]
    val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
    val redirects = application.injector.instanceOf[Redirects]
    val retrievalFilter = application.injector.instanceOf[RetrievalFilter]

    new StcAuthEnrolledActionImpl(
      authConnector,
      appConfig,
      retrievalFilter,
      redirects,
      bodyParsers
    )
  }


  "StcAuthEnrolledActionImpl" - {

    "must build a StcAuthorisedRequest and invoke the block when fully authorised" in {

      val enrolments =
        Enrolments(
          Set(
            Enrolment(
              enrolmentKey,
              Seq(EnrolmentIdentifier(identifierKey, Fixtures.testSubscriptionId.toString)),
              "Activated"
            )
          )
        )

      val application = applicationBuilder().build()

      running(application) {

        val action: StcAuthEnrolledAction =
          testSetup(application, buildRetrieval(enrolments = enrolments))()

        val result =
          action.invokeBlock(FakeRequest(), { req =>
            req.internalId mustBe Fixtures.testInternalId.value
            req.affinityGroup mustBe AffinityGroup.Organisation
            req.subscriptionId mustBe Fixtures.testSubscriptionId
            req.credentialId mustBe Fixtures.testCredentialId
            Future.successful(Results.Ok)
          })

        status(result) mustBe OK

      }
    }

    "must redirect to unauthorised page when STC enrolment is missing" in {

      val application = applicationBuilder().build()

      running(application) {

        val authConnector =
          FakeAuthConnectorFailing(
            new InsufficientEnrolments("STC enrolment missing")
          )

        val action =
          testSetup(application, buildRetrieval())(authConnector)

        val result =
          action.invokeBlock(FakeRequest(), _ => Future.successful(Results.Ok))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value must include(
          routes.UnauthorisedController.onPageLoad().url
        )
      }
    }

    "must redirect to unauthorised page when internalId is missing" in {

      val application = applicationBuilder().build()

      running(application) {

        val action =
          testSetup(application, buildRetrieval(maybeInternalId = None))()

        val result =
          action.invokeBlock(FakeRequest(), _ => Future.successful(Results.Ok))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to journey recovery when STC enrolment is activated but identifier is missing" in {

      val enrolments =
        Enrolments(
          Set(
            Enrolment(
              enrolmentKey,
              Seq.empty,
              "Activated"
            )
          )
        )

      val application = applicationBuilder().build()

      running(application) {

        val action =
          testSetup(application, buildRetrieval(enrolments = enrolments))()

        val result =
          action.invokeBlock(FakeRequest(), _ => Future.successful(Results.Ok))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
