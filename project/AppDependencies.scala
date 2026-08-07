import sbt._

object AppDependencies {

  private val bootstrapVersion = "10.8.0"
  private val hmrcMongoVersion = "2.13.0"

  val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"           %% "play-frontend-hmrc-play-30"    % "13.9.0",
    "uk.gov.hmrc"           %% "bootstrap-frontend-play-30"    % bootstrapVersion,
    "uk.gov.hmrc.mongo"     %% "hmrc-mongo-play-30"            % hmrcMongoVersion,
    "org.apache.poi"        %  "poi-ooxml"                     % "5.5.1",
    "org.apache.commons"    %  "commons-csv"                   % "1.14.1",
    "com.github.pjfanning"  %  "excel-streaming-reader"        % "5.2.0",
    "com.google.guava"      %  "guava"                         % "33.6.0-jre"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30" % hmrcMongoVersion,
    "org.scalatestplus"       %% "scalacheck-1-17"         % "3.2.18.0"
  ).map(_ % Test)

  val overrides: Seq[ModuleID] = Seq(
    "com.google.guava" % "guava" % "33.6.0-jre"
  )

  def apply(): Seq[ModuleID] = compile ++ test
}
