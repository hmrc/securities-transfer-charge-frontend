import play.sbt.routes.RoutesKeys
import scoverage.ScoverageKeys
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

lazy val appName: String = "securities-transfer-charge-frontend"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.3.7"

lazy val scoverageSettings = {

  val ScoverageExclusionPatterns = List(
    "app",
    "prod",
    "uk.gov.hmrc.securitiestransferchargefrontend.mappings",
    "uk.gov.hmrc.securitiestransferchargefrontend.config",
    "uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.govuk",
    "uk.gov.hmrc.securitiestransferchargefrontend.pages",
    "uk.gov.hmrc.securitiestransferchargefrontend.queries",
    "uk.gov.hmrc.securitiestransferchargefrontend.viewmodels",
    "uk.gov.hmrc.securitiestransferchargefrontend.models",
    "uk.gov.hmrc.securitiestransferchargefrontend.handlers",
    "uk.gov.hmrc.BuildInfo",
    "<empty>",
    "testOnlyDoNotUseInAppConf.*")

  Seq(
    ScoverageKeys.coverageExcludedPackages := ScoverageExclusionPatterns.mkString("", ";", ""),
    ScoverageKeys.coverageMinimumStmtTotal := 80,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val microservice = (project in file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(inConfig(Test)(testSettings): _*)
  .settings(ThisBuild / useSuperShell := false)
  .settings(scoverageSettings: _*)
  .settings(
    name := appName,
    scalacOptions += "-Werror",
    RoutesKeys.routesImport ++= Seq(
      "uk.gov.hmrc.securitiestransferchargefrontend.models._",
      "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl",
      "uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId"
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
    scalacOptions ++= Seq(
      "-feature",
      "-Wconf:src=routes/.*:silent",
      "-Wconf:src=views/.*:silent"
    ),
    libraryDependencies ++= AppDependencies(),
    dependencyOverrides ++= AppDependencies.overrides,
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

addCommandAlias("precommit", ";clean;coverage;test;it/test;coverageReport")
