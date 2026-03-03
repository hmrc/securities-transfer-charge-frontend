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

import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class FrontendAppConfig @Inject() (configuration: Configuration, servicesConfig: ServicesConfig) {

  val host: String    = configuration.get[String]("host")
  val appName: String = configuration.get[String]("appName")

  private val contactHost = configuration.get[String]("contact-frontend.host")
  private val contactFormServiceIdentifier = "securities-transfer-charge-frontend"

  def feedbackUrl(implicit request: RequestHeader): String =
    s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier&backUrl=${host + request.uri}"

  val stcEnrolmentKey = "HMRC-STC-ORG"
  val stcIdentifierKey = "STCID"
  
  val registrationFrontendUrl: String = configuration.get[String]("urls.registrationFrontendUrl")

  val loginUrl: String         = configuration.get[String]("urls.login")
  val loginContinueUrl: String = configuration.get[String]("urls.loginContinue")
  val signOutUrl: String       = configuration.get[String]("urls.signOut")
  val continueUrlBase: String = configuration.get[String]("urls.continue-url-base")
  
  // Address Lookup
  private val addressLookupBaseUrl: String =
    servicesConfig.baseUrl("address-lookup-frontend")

  val alfInitUrl: String =
    s"$addressLookupBaseUrl/api/init"

  val alfRetrieveUrl: String =
    s"$addressLookupBaseUrl/api/confirmed"

  val alfStfBuyersContinueUrl: String =
    s"$continueUrlBase/stf/address/return"

  val buyersAlfConfigFileLocation: String = configuration.get[String]("alf.stf-buyers-config-file")
  val alfSellerContinueUrl: String =
    s"$continueUrlBase/stf/seller/address/return"

  val orgAlfConfigFileLocation: String = configuration.get[String]("alf.stf-org-config-file")
  val alfStfOrgContinueUrl: String = s"$continueUrlBase/stf/org/address/return"  

  val sellerAlfConfigFileLocation: String = configuration.get[String]("alf.stf-seller-config-file")
  val reliefsFileLocation: String = configuration.get[String]("reliefs.reliefs-config-file")

  private val exitSurveyBaseUrl: String = configuration.get[Service]("microservice.services.feedback-frontend").baseUrl
  val exitSurveyUrl: String             = s"$exitSurveyBaseUrl/feedback/securities-transfer-charge-frontend"

  val languageTranslationEnabled: Boolean =
    configuration.get[Boolean]("features.welsh-translation")

  def languageMap: Map[String, Lang] = Map(
    "en" -> Lang("en"),
    "cy" -> Lang("cy")
  )

  val timeout: Int   = configuration.get[Int]("timeout-dialog.timeout")
  val countdown: Int = configuration.get[Int]("timeout-dialog.countdown")

  val cacheTtl: Long = configuration.get[Int]("mongodb.timeToLiveInSeconds")

  private val saveAndReturnService =
    configuration.get[Service]("microservice.services.securities-transfer-charge-save-and-return")

  lazy val saveAndReturnBaseUrl: String =
    saveAndReturnService.baseUrl

  lazy val saveAndReturnBasePath: String =
    configuration.get[String](
      "microservice.services.securities-transfer-charge-save-and-return.path"
    )

  lazy val saveAndReturnUrl: String =
    s"$saveAndReturnBaseUrl$saveAndReturnBasePath"


  lazy val connectedPersonsInformationUrl: String = configuration.get[String]("urls.external.connectedPersonsInformation")
  
}
