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

package connectors

import base.SpecBase
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.*
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.*
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.*
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR, NO_CONTENT, OK, SEE_OTHER}
import uk.gov.hmrc.securitiestransferchargefrontend.models.*

import java.net.URL
import scala.concurrent.*

class AlfAddressConnectorSpec extends SpecBase {
  
  val testAddress = AlfConfirmedAddress(
    "auditRef",
    Some("id"),
    AlfAddress(
      List("line1", "line2"),
      "postcode",
      Country("countryName", "countryCode")
    )
  )

  val testAddressJson: JsValue = Json.toJson(testAddress)
  
  val mockHttp: HttpClientV2 = mock[HttpClientV2]
  val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]
  val mockConfigLoader: AlfConfigLoader = mock[AlfConfigLoader]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]

  when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
  when(mockRequestBuilder.setHeader(any[(String, String)])).thenReturn(mockRequestBuilder)
  
  def initTestSetup(initResponse: HttpResponse): AlfAddressConnector = {
    when(mockConfig.alfInitUrl).thenReturn("http://localhost:1201/alf-init")
    when(mockConfigLoader.loadConfig(any[String], any[String])).thenReturn(Json.obj())
    when(mockHttp.post(any[URL])(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.execute[HttpResponse]).thenReturn(Future.successful(initResponse))
    new AlfAddressConnectorImpl(mockHttp, mockConfig, mockConfigLoader)
  }

  def retrieveTestSetup(response: HttpResponse): AlfAddressConnector = {
    when(mockConfig.alfRetrieveUrl).thenReturn("http://localhost:1201/alf-retrieve")
    when(mockHttp.get(any[URL])(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.execute[HttpResponse]).thenReturn(Future.successful(response))
    new AlfAddressConnectorImpl(mockHttp, mockConfig, mockConfigLoader)
  }

  "AlfAddressConnector" - {

    "init" - {

      "should fail if the response from ALF is not Accepted" in {
        val testResponse: HttpResponse = HttpResponse(INTERNAL_SERVER_ERROR, "")
        val connector: AlfAddressConnector = initTestSetup(testResponse)
        val result: Future[Result] = connector.initAlfJourneyRequest("configLocation", "returnUrl")(HeaderCarrier())
        whenReady(result.failed) { ex =>
          ex mustBe a[AlfAddressConnectorImpl#AlfException]
        }
      }
      "should fail is the response from ALF is Accepted but no Location header is returned" in {
        val testResponse: HttpResponse = HttpResponse(NO_CONTENT, "")
        val connector: AlfAddressConnector = initTestSetup(testResponse)
        val result: Future[Result] = connector.initAlfJourneyRequest("configLocation", "returnUrl")(HeaderCarrier())
        whenReady(result.failed) { ex =>
          ex mustBe a[AlfAddressConnectorImpl#AlfException]
        }
      }
      "should succeed is the response from ALF is not Accepted and a Location header is returned" in {
        val location = "http://localhost:1201/alf-journey"
        val testResponse: HttpResponse = HttpResponse(ACCEPTED, "", Map("Location" -> Seq(location)))
        val connector: AlfAddressConnector = initTestSetup(testResponse)
        val result: Future[Result] = connector.initAlfJourneyRequest("configLocation", "returnUrl")(HeaderCarrier())
        whenReady(result) { res =>
          res.header.status mustBe SEE_OTHER
          res.header.headers.get("Location") mustBe Some(location)
        }
      }
    }

    "retrieve" - {

      "should fail if the response from ALF is not OK" in {
        val testResponse: HttpResponse = HttpResponse(INTERNAL_SERVER_ERROR, testAddressJson, Map.empty)
        val connector: AlfAddressConnector = retrieveTestSetup(testResponse)
        val result: Future[AlfConfirmedAddress] = connector.alfRetrieveAddress("location")(HeaderCarrier())
        whenReady(result.failed) { ex =>
          ex mustBe a[AlfAddressConnectorImpl#AlfException]
        }

      }
      "should fail if the response from ALF is OK but the body cannot be parsed to an AlfConfirmedAddress" in {
        val testResponse: HttpResponse = HttpResponse(OK, "nonesense")
        val connector: AlfAddressConnector = retrieveTestSetup(testResponse)
        val result: Future[AlfConfirmedAddress] = connector.alfRetrieveAddress("location")(HeaderCarrier())
        whenReady(result.failed) { ex =>
          ex mustBe a[AlfAddressConnectorImpl#AlfException]
        }
      }
      "should succeed if the response from ALF is OK and the body can be parsed to an AlfConfirmedAddress" in {
        val testResponse: HttpResponse = HttpResponse(OK, testAddressJson, Map.empty)
        val connector: AlfAddressConnector = retrieveTestSetup(testResponse)
        val result: Future[AlfConfirmedAddress] = connector.alfRetrieveAddress("location")(HeaderCarrier())
        whenReady(result) { res =>
          res mustBe(testAddress)
        }
      }
      }
    }

  }
