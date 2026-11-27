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

package services

import base.SpecBase
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.when
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.models.Relief
import uk.gov.hmrc.securitiestransferchargefrontend.services.{BundledReliefs, BundledReliefsImpl}
import uk.gov.hmrc.securitiestransferchargefrontend.utils.ResourceLoader

class BundledReliefsSpec extends SpecBase with MockitoSugar {

  "BundledReliefs.parse" - {

    "must read 'name, percentage' lines, trimming spaces and ignoring blank lines" in {
      BundledReliefs.parse("Charities Relief, 100\n\n  Group Relief ,50  \n") mustBe
        Seq(Relief("Charities Relief", 100), Relief("Group Relief", 50))
    }

    "must read names that contain a slash" in {
      BundledReliefs.parse("Insolvency/Bankruptcy Relief, 100") mustBe Seq(Relief("Insolvency/Bankruptcy Relief", 100))
    }

    "must skip lines that are not 'name, percentage'" in {
      BundledReliefs.parse("No percentage\nBad, abc\n, 100\nGood, 25") mustBe Seq(Relief("Good", 25))
    }

    "must return nothing for an empty file" in {
      BundledReliefs.parse("") mustBe Seq.empty
    }
  }

  "BundledReliefsImpl" - {

    "must load the file named in config through the ResourceLoader" in {
      val app       = applicationBuilder().build()
      val appConfig = app.injector.instanceOf[FrontendAppConfig]
      val loader    = app.injector.instanceOf[ResourceLoader]

      new BundledReliefsImpl(loader, appConfig).reliefs.map(_.name) must contain allOf (
        "Charities Relief",
        "PISCES",
        "UK Listing Relief"
      )
    }

    "must bundle the 15 reliefs, each at 100, in the file shipped with the service" in {
      val app       = applicationBuilder().build()
      val appConfig = app.injector.instanceOf[FrontendAppConfig]
      val loader    = app.injector.instanceOf[ResourceLoader]

      val reliefs = new BundledReliefsImpl(loader, appConfig).reliefs

      reliefs.size mustBe 15
      reliefs.map(_.rate).toSet mustBe Set(100)
      reliefs.map(_.name).distinct.size mustBe 15
    }

    "must give no reliefs, rather than fail, when the file cannot be read" in {
      val loader    = mock[ResourceLoader]
      val appConfig = mock[FrontendAppConfig]
      when(appConfig.bundledReliefsFile).thenReturn("missing.txt")
      when(loader.loadString("missing.txt")).thenThrow(new java.util.MissingResourceException("missing", "x", "missing.txt"))

      new BundledReliefsImpl(loader, appConfig).reliefs mustBe Seq.empty
    }
  }
}
