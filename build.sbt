import sbt.Resolver.bintrayRepo
import com.typesafe.sbt.packager.docker._
import sbt.Keys.{javaOptions, resolvers}


organization in ThisBuild := "net.busynot"
version in ThisBuild := "1.0-SNAPSHOT"
lazy val buildVersion = "1.0-SNAPSHOT"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.11.8"

resolvers += "Local Maven" at Path.userHome.asFile.toURI.toURL + ".m2/repository"


lazy val `moovis` = (project in file("."))
  .aggregate(`listener-api`,`listener-impl`,`epticapuller-api`,`epticapuller-impl`,`uccxpuller-api`,`uccxpuller-impl`)

lazy val `listener-api` = (project in file("listener-api"))
  .settings(common: _*)
  .settings(
    libraryDependencies ++= Seq(
      lagomJavadslApi,
      lagomJavadslJackson,
      lombok
    )
  )

lazy val `listener-impl` = (project in file("listener-impl"))
  .enablePlugins(LagomJava,SbtAspectj,JavaAgent,JavaAppPackaging)
  .settings(common: _*)
  .settings(
    resolvers += bintrayRepo("hajile", "maven"),
    libraryDependencies ++= Seq(
      lagomJavadslPubSub,
      "org.influxdb" % "influxdb-java" % "2.8",
      lombok
    )
  )
  .settings(
    libraryDependencies ++= levelDbDeps
  )
  .settings(
    version := buildVersion,
    javaOptions in run += "-Dorg.aspectj.tracing.factory=default",
    javaAgents += "org.aspectj" % "aspectjweaver" % "1.8.10" % "compile;test;runtime;dist",
    javaOptions in Universal += "-Dorg.aspectj.tracing.factory=default"
  )
  .settings(
    version := buildVersion,
    version in Docker := buildVersion,
    dockerBaseImage := sys.props.getOrElse("baseImage", "openjdk:8-jre-alpine"),
    dockerRepository := Some(BuildTarget.dockerRepository),
    dockerUpdateLatest := true,
    dockerEntrypoint ++= """ -J-Xss256k -J-Xms256M -J-Xmx256M -Dhttp.address="$(eval "echo $LISTENER_BIND_IP")" -Dhttp.port="$(eval "echo $LISTENER_BIND_PORT")" -Dakka.remote.netty.tcp.hostname="$(eval "echo $AKKA_REMOTING_HOST")" -Dakka.remote.netty.tcp.bind-hostname="$(eval "echo $AKKA_REMOTING_BIND_HOST")" -Dakka.remote.netty.tcp.port="$(eval "echo $AKKA_REMOTING_PORT")" -Dakka.remote.netty.tcp.bind-port="$(eval "echo $AKKA_REMOTING_BIND_PORT")" $(IFS=','; I=0; for NODE in $(eval "echo $AKKA_SEED_NODES"); do echo "-Dakka.cluster.seed-nodes.$I=akka.tcp://application@$NODE"; I=$(expr $I + 1); done)""".split(" ").toSeq,
    dockerCommands :=
      dockerCommands.value.flatMap {
        case ExecCmd("ENTRYPOINT", args @ _*) => Seq(Cmd("ENTRYPOINT", args.mkString(" ")))
        case c @ Cmd("FROM", _) => Seq(c)
        case v => Seq(v)
      }
  )
  .settings(BuildTarget.additionalSettings)
  .settings(lagomForkedTestSettings: _*)
  .dependsOn(`listener-api`)

lazy val `uccxpuller-api` = (project in file("uccxpuller-api"))
  .settings(common: _*)
  .settings(
    libraryDependencies ++= Seq(
      lagomJavadslApi,
      lagomJavadslJackson,
      lombok
    )
  )

lazy val `uccxpuller-impl` = (project in file("uccxpuller-impl"))
  .enablePlugins(LagomJava,SbtAspectj,JavaAgent,JavaAppPackaging)
  .settings(common: _*)
  .settings(
    resolvers += bintrayRepo("hajile", "maven"),
    libraryDependencies ++= Seq(
      lagomJavadslPubSub,
      "org.influxdb" % "influxdb-java" % "2.8",
      lombok
    ),
    libraryDependencies ++= informix
  )
  .settings(
    libraryDependencies ++= levelDbDeps
  )
  .settings(
    version := buildVersion,
    javaOptions in run += "-Dorg.aspectj.tracing.factory=default",
    javaAgents += "org.aspectj" % "aspectjweaver" % "1.8.10" % "compile;test;runtime;dist",
    javaOptions in Universal += "-Dorg.aspectj.tracing.factory=default"
  )
  .settings(
    version := buildVersion,
    version in Docker := buildVersion,
    dockerBaseImage := sys.props.getOrElse("baseImage", "openjdk:8-jre-alpine"),
    dockerRepository := Some(BuildTarget.dockerRepository),
    dockerUpdateLatest := true,
    dockerEntrypoint ++= """ -J-Xss256k -J-Xms256M -J-Xmx256M -Dhttp.address="$(eval "echo $UCCXPULLER_BIND_IP")" -Dhttp.port="$(eval "echo $UCCXPULLER_BIND_PORT")" -Dakka.remote.netty.tcp.hostname="$(eval "echo $AKKA_REMOTING_HOST")" -Dakka.remote.netty.tcp.bind-hostname="$(eval "echo $AKKA_REMOTING_BIND_HOST")" -Dakka.remote.netty.tcp.port="$(eval "echo $AKKA_REMOTING_PORT")" -Dakka.remote.netty.tcp.bind-port="$(eval "echo $AKKA_REMOTING_BIND_PORT")" $(IFS=','; I=0; for NODE in $(eval "echo $AKKA_SEED_NODES"); do echo "-Dakka.cluster.seed-nodes.$I=akka.tcp://application@$NODE"; I=$(expr $I + 1); done)""".split(" ").toSeq,
    dockerCommands :=
      dockerCommands.value.flatMap {
        case ExecCmd("ENTRYPOINT", args @ _*) => Seq(Cmd("ENTRYPOINT", args.mkString(" ")))
        case c @ Cmd("FROM", _) => Seq(c)
        case v => Seq(v)
      }
  )
  .settings(BuildTarget.additionalSettings)
  .settings(lagomForkedTestSettings: _*)
  .dependsOn(`uccxpuller-api`)
  
lazy val `epticapuller-api` = (project in file("epticapuller-api"))
  .settings(common: _*)
  .settings(
    libraryDependencies ++= Seq(
      lagomJavadslApi,
      lagomJavadslJackson,
      lombok
    )
  )

lazy val `epticapuller-impl` = (project in file("epticapuller-impl"))
  .enablePlugins(LagomJava,SbtAspectj,JavaAgent,JavaAppPackaging)
  .settings(common: _*)
  .settings(
    resolvers += bintrayRepo("hajile", "maven"),
    libraryDependencies ++= Seq(
      lagomJavadslPubSub,
      "org.influxdb" % "influxdb-java" % "2.8",
      lombok
    )
  )
  .settings(
    version := buildVersion,
    javaOptions in run += "-Dorg.aspectj.tracing.factory=default",
    javaAgents += "org.aspectj" % "aspectjweaver" % "1.8.10" % "compile;test;runtime;dist",
    javaOptions in Universal += "-Dorg.aspectj.tracing.factory=default"
  )
  .settings(
    version := buildVersion,
    version in Docker := buildVersion,
    dockerBaseImage := sys.props.getOrElse("baseImage", "openjdk:8-jre-alpine"),
    dockerRepository := Some(BuildTarget.dockerRepository),
    dockerUpdateLatest := true,
    dockerEntrypoint ++= """ -J-Xss256k -J-Xms256M -J-Xmx256M -Dhttp.address="$(eval "echo $EPTICAPULLER_BIND_IP")" -Dhttp.port="$(eval "echo $EPTICAPULLER_BIND_PORT")" -Dakka.remote.netty.tcp.hostname="$(eval "echo $AKKA_REMOTING_HOST")" -Dakka.remote.netty.tcp.bind-hostname="$(eval "echo $AKKA_REMOTING_BIND_HOST")" -Dakka.remote.netty.tcp.port="$(eval "echo $AKKA_REMOTING_PORT")" -Dakka.remote.netty.tcp.bind-port="$(eval "echo $AKKA_REMOTING_BIND_PORT")" $(IFS=','; I=0; for NODE in $(eval "echo $AKKA_SEED_NODES"); do echo "-Dakka.cluster.seed-nodes.$I=akka.tcp://application@$NODE"; I=$(expr $I + 1); done)""".split(" ").toSeq,
    dockerCommands :=
      dockerCommands.value.flatMap {
        case ExecCmd("ENTRYPOINT", args @ _*) => Seq(Cmd("ENTRYPOINT", args.mkString(" ")))
        case c @ Cmd("FROM", _) => Seq(c)
        case v => Seq(v)
      }
  )
  .settings(BuildTarget.additionalSettings)
  .settings(lagomForkedTestSettings: _*)
  .dependsOn(`epticapuller-api`,`epticasupervision-api`)
  
lazy val `epticasupervision-api` = (project in file("epticasupervision-api"))
  .settings(common: _*)
  .settings(
    libraryDependencies ++= Seq(
      lagomJavadslApi
    )
  )

val lombok = "org.projectlombok" % "lombok" % "1.16.10"
val slf4j = "org.slf4j" % "slf4j-api" % "1.7.25"
val lagomZookeeperServiceLocator = "com.lightbend.lagom" % "lagom-service-locator-zookeeper" % "1.0.0-SNAPSHOT"

lazy val levelDbDeps = Seq(
  "org.iq80.leveldb" % "leveldb" % "0.7",
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
  "com.typesafe.akka" %% "akka-persistence" % "2.4.20"
)

lazy val informix = Seq(
  "com.ibm.informix" % "jdbc" % "4.10.10.0"
)


lagomCassandraEnabled in ThisBuild := false
lagomUnmanagedServices in ThisBuild := Map("epticasupervision" -> "http://eptica")

def common = Seq(
  javacOptions in compile += "-parameters"
)