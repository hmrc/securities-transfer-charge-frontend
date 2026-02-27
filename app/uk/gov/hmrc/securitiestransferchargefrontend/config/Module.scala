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

package uk.gov.hmrc.securitiestransferchargefrontend.config

import com.google.inject.AbstractModule
import play.api.http.HttpErrorHandler
import uk.gov.hmrc.securitiestransferchargefrontend.clients.registration.{RegistrationClient, RegistrationClientImpl}
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.{AlfAddressConnector, AlfAddressConnectorImpl, AlfConfigLoader, AlfConfigLoaderImpl, SubscriptionConnector, SubscriptionConnectorImpl}
import uk.gov.hmrc.securitiestransferchargefrontend.clients.{SaveAndReturnClient, SaveAndReturnClientImpl, SubmissionIdClient, SubmissionIdClientImpl}
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.handlers.ErrorHandler
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.stf.individuals.StfNavigator
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.{SubscriptionDataRepository, SubscriptionDataRepositoryImpl}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.{Navigator, PersistentNavigator}
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.{SessionRepository, SessionRepositoryImpl}
import uk.gov.hmrc.securitiestransferchargefrontend.services.*

import java.time.{Clock, ZoneOffset}

class Module extends AbstractModule {

  override def configure(): Unit = {

    bind(classOf[DataRetrievalAction]).to(classOf[DataRetrievalActionImpl]).asEagerSingleton()
    bind(classOf[DataRequiredAction]).to(classOf[DataRequiredActionImpl]).asEagerSingleton()

    // For session based storage instead of cred based, change to SessionIdentifierAction
    bind(classOf[IdentifierAction]).to(classOf[AuthenticatedIdentifierAction]).asEagerSingleton()

    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone.withZone(ZoneOffset.UTC))
    bind(classOf[HttpErrorHandler]).to(classOf[ErrorHandler])
    bind(classOf[StcAuthEnrolledAction]).to(classOf[StcAuthEnrolledActionImpl]).asEagerSingleton()
    bind(classOf[StcDataRetrievalAction]).to(classOf[StcDataRetrievalActionImpl])
    bind(classOf[StcDataRequiredAction]).to(classOf[StcDataRequiredActionImpl])
    bind(classOf[SessionRepository]).to(classOf[SessionRepositoryImpl])
    bind(classOf[Navigator]).to(classOf[StfNavigator])
    bind(classOf[PersistentNavigator]).to(classOf[StfNavigator])
    bind(classOf[SubmissionIdClient]).to(classOf[SubmissionIdClientImpl])
    bind(classOf[SaveAndReturnClient]).to(classOf[SaveAndReturnClientImpl])
    bind(classOf[AlfAddressConnector]).to(classOf[AlfAddressConnectorImpl])

    bind(classOf[AlfConfigLoader]).to(classOf[AlfConfigLoaderImpl])

    bind(classOf[SubscriptionDataRepository]).to(classOf[SubscriptionDataRepositoryImpl])
    bind(classOf[SubscriptionConnector]).to(classOf[SubscriptionConnectorImpl])
    bind(classOf[RegistrationClient]).to(classOf[RegistrationClientImpl]).asEagerSingleton()
    bind(classOf[AnswerPersistenceService]).to(classOf[AnswerPersistenceServiceImpl])
  }
}
