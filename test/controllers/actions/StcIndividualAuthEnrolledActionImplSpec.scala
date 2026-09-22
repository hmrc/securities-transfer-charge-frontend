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
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ItmpName, ~}
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.filters.RetrievalFilter
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.{
  StcIndividualAuthEnrolledAction,
  StcIndividualAuthEnrolledActionImpl
}
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.{Redirects, routes}

import scala.concurrent.Future

class StcIndividualAuthEnrolledActionImplSpec extends SpecBase {

  private type RetrievalType =
    Option[String] ~
      Option[String] ~
      Enrolments ~
      Option[Credentials] ~
      Option[ItmpName]

  private val enrolmentKey = "HMRC-STC-ORG"
  private val identifierKey = "STCID"

  private def buildRetrieval(
                              maybeInternalId: Option[String] =
                              Some(Fixtures.testInternalId.value),
                              maybeGroupIdentifier: Option[String] =
                              Some(Fixtures.testGroupIdentifier.value),
                              enrolments: Enrolments =
                              Fixtures.enrolledForStc,
                              maybeCredentials: Option[Credentials] =
                              Some(
                                Credentials(
                                  Fixtures.testCredentialId.value,
                                  "providerType"
                                )
                              ),
                              maybeItmpName: Option[ItmpName] =
                              Some(
                                ItmpName(
                                  Some("Test"),
                                  None,
                                  Some("Name")
                                )
                              )
                            ): RetrievalType =
    new~(
      new~(
        new~(
          new~(
            maybeInternalId,
            maybeGroupIdentifier
          ),
          enrolments
        ),
        maybeCredentials
      ),
      maybeItmpName
    )

  private def testSetup(application: Application, retrievals: RetrievalType)(
                         authConnector: AuthConnector =
                         new FakeAuthConnectorSuccess(retrievals)
                       ): StcIndividualAuthEnrolledAction = {

    val appConfig = application.injector.instanceOf[FrontendAppConfig]

    val bodyParsers = application.injector.instanceOf[BodyParsers.Default]

    val redirects = application.injector.instanceOf[Redirects]

    val retrievalFilter = application.injector.instanceOf[RetrievalFilter]

    new StcIndividualAuthEnrolledActionImpl(
      authConnector,
      appConfig,
      retrievalFilter,
      redirects,
      bodyParsers
    )
  }

  "StcIndividualAuthEnrolledActionImpl" - {

    "must build a StcIndividualAuthorisedRequest and invoke the block when fully authorised" in {

      val enrolments =
        Enrolments(
          Set(
            Enrolment(
              enrolmentKey,
              Seq(
                EnrolmentIdentifier(
                  identifierKey,
                  Fixtures.testSubscriptionId.toString
                )
              ),
              "Activated"
            )
          )
        )

      val application =
        applicationBuilder(affinityGroup = individualAffinity).build()

      running(application) {

        val action: StcIndividualAuthEnrolledAction = testSetup(application, buildRetrieval(enrolments = enrolments))()

        val result =
          action.invokeBlock(
            FakeRequest(),
            { req =>
              req.internalId mustBe Fixtures.testInternalId.value
              req.groupIdentifier mustBe Fixtures.testGroupIdentifier.value
              req.subscriptionId mustBe Fixtures.testSubscriptionId
              req.credentialId mustBe Fixtures.testCredentialId
              req.name mustBe "Test Name"

              Future.successful(Results.Ok)
            }
          )

        status(result) mustBe OK
      }
    }

    "must redirect to unauthorised page when authorisation fails" in {

      val application =
        applicationBuilder().build()

      running(application) {

        val authConnector = FakeAuthConnectorFailing(InsufficientEnrolments("STC enrolment missing"))

        val action: StcIndividualAuthEnrolledAction = testSetup(application, buildRetrieval())(authConnector)

        val result = action.invokeBlock(FakeRequest(), _ => Future.successful(Results.Ok))

        status(result) mustBe SEE_OTHER

        redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to unauthorised page when internalId is missing" in {

      val application =
        applicationBuilder().build()

      running(application) {

        val action: StcIndividualAuthEnrolledAction = testSetup(application, buildRetrieval(maybeInternalId = None))()

        val result = action.invokeBlock(FakeRequest(), _ => Future.successful(Results.Ok))

        status(result) mustBe SEE_OTHER

        redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to unauthorised page when group identifier is missing" in {

      val application =
        applicationBuilder().build()

      running(application) {

        val action: StcIndividualAuthEnrolledAction = testSetup(application, buildRetrieval(maybeGroupIdentifier = None))()

        val result = action.invokeBlock(FakeRequest(), _ => Future.successful(Results.Ok))

        status(result) mustBe SEE_OTHER

        redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must redirect to journey recovery when STC identifier is missing" in {

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

      val application =
        applicationBuilder().build()

      running(application) {

        val action: StcIndividualAuthEnrolledAction = testSetup(application, buildRetrieval(enrolments = enrolments)
          )()

        val result = action.invokeBlock(FakeRequest(), _ => Future.successful(Results.Ok))

        status(result) mustBe SEE_OTHER

        redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to unauthorised page when credentials are missing" in {

      val application =
        applicationBuilder().build()

      running(application) {

        val action: StcIndividualAuthEnrolledAction = testSetup(application, buildRetrieval(maybeCredentials = None))()

        val result = action.invokeBlock(FakeRequest(), _ => Future.successful(Results.Ok))

        status(result) mustBe SEE_OTHER

        redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
      }
    }

    "must use the default name when ITMP name is missing" in {

      val application = applicationBuilder().build()

      running(application) {

        val action: StcIndividualAuthEnrolledAction = testSetup(application, buildRetrieval(maybeItmpName = None))()

        val result = action.invokeBlock(FakeRequest(),
            { req =>
              req.name mustBe "Securities Transfer Tax"
              Future.successful(Results.Ok)
            }
          )

        status(result) mustBe OK
      }
    }

    "must redirect to login when there is no active session" in {

      val application =
        applicationBuilder().build()

      running(application) {

        val authConnector = FakeAuthConnectorFailing(SessionRecordNotFound())

        val action: StcIndividualAuthEnrolledAction = testSetup(application, buildRetrieval())(authConnector)

        val result = action.invokeBlock(FakeRequest(), _ => Future.successful(Results.Ok))

        status(result) mustBe SEE_OTHER
      }
    }

    "must redirect to journey recovery when affinity group is unsupported" in {

      val application =
        applicationBuilder().build()

      running(application) {

        val authConnector = FakeAuthConnectorFailing(UnsupportedAffinityGroup())

        val action: StcIndividualAuthEnrolledAction = testSetup(application, buildRetrieval())(authConnector)

        val result = action.invokeBlock(FakeRequest(), _ => Future.successful(Results.Ok))

        status(result) mustBe SEE_OTHER

        redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}