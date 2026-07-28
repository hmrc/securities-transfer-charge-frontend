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

import java.time.LocalDate

@Singleton
class FrontendAppConfig @Inject() (configuration: Configuration, servicesConfig: ServicesConfig) {

  val host: String    = configuration.get[String]("host")
  val appName: String = configuration.get[String]("appName")
  val firstChargingPoint: LocalDate = LocalDate.parse(configuration.get[String]("first-charging-point"))
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
  val alfSellerContinueUrl: String = s"$continueUrlBase/stf/seller/address/return"

  val orgAlfConfigFileLocation: String = configuration.get[String]("alf.stf-org-config-file")
  val alfStfOrgContinueUrl: String = s"$continueUrlBase/stf/org/address/return"
  
  val agentAlfBuyerConfigFileLocation: String = configuration.get[String]("alf.stf-agent-buyer-config-file")
  val alfStfAgentContinueUrl: String = s"$continueUrlBase/stf/agent/address/return"

  val alfOrgSellerContinueUrl: String = s"$continueUrlBase/stf/org/seller/address/return"
  
  val alfAgentSellerContinueUrl: String = s"$continueUrlBase/stf/agent/seller/address/return"
  val agentSellerAlfConfigFileLocation: String = configuration.get[String]("alf.stf-agent-seller-config-file")

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

  val upscanTtl: Long = configuration.get[Int]("mongodb.upscanTimeToLiveInHours")
  val validationErrorTtl: Long = configuration.get[Int]("mongodb.validationErrorTimeToLiveInHours")
  val parsedStcRowsTtl: Long = configuration.get[Int]("mongodb.parsedStcRowsTimeToLiveInDays")
  val checksumTtl: Long = configuration.get[Int]("mongodb.checksumTimeToLiveInDays")

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

  enum SaveAndReturnRetrievalType {
    case UserOnly, UserAndGroup
  }

  val saveAndReturnRetrieval: SaveAndReturnRetrievalType =
    if (configuration.get[Boolean]("microservice.save-and-return-uses-user-id-only"))
      SaveAndReturnRetrievalType.UserOnly else SaveAndReturnRetrievalType.UserAndGroup

  lazy val connectedPersonsInformationUrl: String = configuration.get[String]("urls.external.connectedPersonsInformation")
  val stfBaseUrl: String = servicesConfig.baseUrl("securities-transfer-charge-frontend")

  val basePath = "/securities-transfer-charge"
  
  val upscanBaseUrl: String = servicesConfig.baseUrl("upscan-initiate")
  val upscanCallbackUrl: String =  s"$stfBaseUrl$basePath/upscan-callback"
  val upscanUploadSuccessfulUrl: String = s"$host$basePath/bulk-processing"
  val upscanUploadFailureUrl: String = s"$host$basePath/upload-template/problem"

  // File streamer
  val rowCacheSize: Int = configuration.get[Int]("file-upload.xlsx.row-cache-size")
  val bufferSizeBytes: Int = configuration.get[Int]("file-upload.xlsx.buffer-size-bytes")

  // Refresh
  val spinnerPageRefreshInterval: Int = configuration.get[Int]("page-refresh.spinner.interval")
  val spinnerPageRefreshTimeout: Int = configuration.get[Int]("page-refresh.spinner.timeout")
  
  val govUKUrl: String = configuration.get[String]("urls.external.govUK")
  val reliefsGuidanceUrl: String = configuration.get[String]("urls.external.reliefsGuidance")
}
