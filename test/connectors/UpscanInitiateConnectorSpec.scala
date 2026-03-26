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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.{UpscanInitiateConnector, UpscanInitiateConnectorImpl}
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.{UploadRequest, UpscanInitiateResponse}

import java.net.URL
import scala.concurrent.Future

class UpscanInitiateConnectorSpec extends SpecBase with MockitoSugar {

  private val mockHttp: HttpClientV2 = mock[HttpClientV2]
  private val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]
  private val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]

  private val testResponseModel = UpscanInitiateResponse(
    reference = "ref123",
    uploadRequest = UploadRequest(
      href = "http://upload",
      fields = Map("key" -> "value")
    )
  )

  private val testResponseJson: JsValue = Json.toJson(testResponseModel)

  when(mockRequestBuilder.withBody(any())(any(), any(), any()))
    .thenReturn(mockRequestBuilder)

  def setup(response: HttpResponse): UpscanInitiateConnector = {

    when(mockConfig.upscanBaseUrl).thenReturn("http://localhost:9570")
    when(mockConfig.upscanCallbackUrl).thenReturn("callback-url")
    when(mockConfig.upscanUploadSuccessfulUrl).thenReturn("success-url")
    when(mockConfig.upscanUploadFailureUrl).thenReturn("failure-url")

    when(mockHttp.post(any[URL])(any[HeaderCarrier]))
      .thenReturn(mockRequestBuilder)

    when(mockRequestBuilder.execute[HttpResponse])
      .thenReturn(Future.successful(response))

    new UpscanInitiateConnectorImpl(mockHttp, mockConfig)
  }

  "UpscanInitiateConnector" - {

    "initiate" - {

      "should succeed when response is 200 and valid JSON" in {

        val response = HttpResponse(OK, testResponseJson, Map.empty)
        val connector = setup(response)

        val result = connector.initiate()(HeaderCarrier())

        whenReady(result) { res =>
          res mustBe testResponseModel
        }
      }

      "should fail when there is an UpscanInitiateResponse parse failure" in {
        val invalidResponse = """{
                                |    "reference": "123455678990",
                                |    "uploadRequest": {
                                |        "href": "https://bucketName.s3.eu-west-2.amazonaws.com"
                                |    }
                                |}""".stripMargin

        val response = HttpResponse(OK, invalidResponse)
        val connector = setup(response)

        val result = connector.initiate()(HeaderCarrier())

        whenReady(result.failed) { ex =>
          ex mustBe a[RuntimeException]
          ex.getMessage must include("Failed to parse UpscanInitiateResponse")
        }
      }

      "should fail when response is a non-200" in {

        val response = HttpResponse(INTERNAL_SERVER_ERROR, "error")
        val connector = setup(response)

        val result = connector.initiate()(HeaderCarrier())

        whenReady(result.failed) { ex =>
          ex mustBe a[UpstreamErrorResponse]
          ex.getMessage must include("Upscan initiate failed")
        }
      }
    }
  }
}