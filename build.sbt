import play.sbt.routes.RoutesKeys
import scoverage.ScoverageKeys
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

import scala.collection.immutable.Seq

lazy val appName: String = "securities-transfer-charge-frontend"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.3.7"

lazy val microservice = (project in file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(inConfig(Test)(testSettings): _*)
  .settings(ThisBuild / useSuperShell := false)
  .settings(
    name := appName,
    scalacOptions += "-Werror",
    RoutesKeys.routesImport ++= Seq(
      "uk.gov.hmrc.securitiestransferchargefrontend.models._",
      "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl"
    ),
    TwirlKeys.templateImports ++= Seq(
      "play.twirl.api.HtmlFormat",
      "play.twirl.api.HtmlFormat._",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
      "uk.gov.hmrc.hmrcfrontend.views.config._",
      "uk.gov.hmrc.securitiestransferchargefrontend.views.ViewUtils._",
      "uk.gov.hmrc.securitiestransferchargefrontend.models.Mode",
      "uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes._",
      "uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.govuk.all._"
    ),
    PlayKeys.playDefaultPort := 30036,
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*handlers.*;.*components.*;" +
      ".*Routes.*;.*viewmodels.govuk.*;",
    ScoverageKeys.coverageMinimumStmtTotal := 78,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    scalacOptions ++= Seq(
      "-feature",
      "-Wconf:src=routes/.*:silent",
      "-Wconf:src=views/.*:silent"
    ),
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true,
    pipelineStages := Seq(digest),
    Assets / pipelineStages := Seq(concat)
  )

lazy val testSettings: Seq[Def.Setting[_]] = Seq(
  fork := true,
  unmanagedSourceDirectories += baseDirectory.value / "test-utils"
)

lazy val it =
  (project in file("it"))
    .enablePlugins(PlayScala)
    .dependsOn(microservice % "test->test")

// Get rid of the warnings about flags being set repeatedly
Compile / scalacOptions := (Compile / scalacOptions).value.distinct
