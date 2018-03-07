import sbt._

object BuildTarget {
  private sealed trait DeploymentRuntime
  private case object Docker extends DeploymentRuntime

  private val deploymentRuntime: DeploymentRuntime = sys.props.get("buildTarget") match {
    case Some(v) if v.toLowerCase == "docker" =>
      Docker

    case Some(v) =>
      sys.error(s"The build target $v is not supported. Available: 'docker'")

    case None =>
      Docker
  }

  val additionalSettings = deploymentRuntime match {
    case Docker =>
      Seq(
        Keys.libraryDependencies ++= Seq(
          Library.serviceLocatorZookeeper
        ),
        Keys.resolvers ++= Seq(
          Resolver.mavenLocal,
          Resolver.defaultLocal
        ),
        Keys.unmanagedResourceDirectories in Compile += Keys.sourceDirectory.value / "main" / "docker"
      )
  }

  val dockerRepository: String = deploymentRuntime match {
    case Docker => "moovis"
  }
}
