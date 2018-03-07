import sbt._

object Version {
  val serviceLocatorZookeeper = "1.0.0-SNAPSHOT"
}

object Library {
  val serviceLocatorZookeeper   = "com.lightbend.lagom" %% "lagom-service-locator-zookeeper"  % Version.serviceLocatorZookeeper
}
