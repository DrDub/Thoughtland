
val commonSettings = Seq(
  version := "0.0.2-SNAPSHOT",
  scalaVersion := "2.11.4",
  organization := "net.duboue",
  homepage := Some(new URL("http://thoughtland.duboue.net")),
  description := "Verbalizing n-dimensional objects.",
  startYear := Some(2013),
  licenses := Seq("AGPLv3+" -> new URL("http://www.gnu.org/licenses/agpl-3.0.html")),
  javacOptions ++= Seq(
    "-encoding", "UTF-8",
    "-source", "1.7",
    "-target", "1.7",
    "-Xlint:unchecked",
    "-Xlint:deprecation"),
  scalacOptions ++= List(
    "-encoding", "UTF-8",
    "-feature",
    "-unchecked",
    "-deprecation",
    "-Xlint",
    "-language:_",
    "-target:jvm-1.7",
    "-Xlog-reflective-calls"))

pomExtra :=
    <scm>
      <url>git@github.com:DrDub/thoughtland.git</url>
      <connection>scm:git:git@github.com:DrDub/thoughtland.git</connection>
    </scm>

val MAHOUT_VERSION = "0.8"
val HADOOP_VERSION = "1.2.1"


val junit = "junit" % "junit" % "4.8.1" % "test"
val specs2Core       = "org.specs2"      %% "specs2-core"       % "2.4.13"   % "test"
val specs2ScalaCheck = "org.specs2"      %% "specs2-scalacheck" % "2.4.13"   % "test"
val scalatest = "org.scalatest" %% "scalatest" % "2.2.4" % "test"
val guava = "com.google.guava" % "guava" % "18.0" 
val mahoutCore = "org.apache.mahout" % "mahout-core" % MAHOUT_VERSION
val mahoutIntegration = ("org.apache.mahout" % "mahout-integration" % MAHOUT_VERSION)
  .exclude("org.apache.cassandra", "cassandra-all")
  .exclude("me.prettyprint", "hector-core")
  .exclude("org.mongodb", "mongo-java-driver")
  .exclude("org.mongodb", "bson")
  .exclude("org.apache.lucene", "lucene-benchmark")
  .exclude("org.apache.solr", "solr-commons-csv")

val mahoutExamples = ("org.apache.mahout" % "mahout-examples" % MAHOUT_VERSION) 
  .exclude("org.apache.lucene", "lucene-benchmark")
  .exclude("org.mongodb", "mongo-java-driver")
  .exclude("org.mongodb", "bson")

val hadoopCore = "org.apache.hadoop" % "hadoop-core" % HADOOP_VERSION 

val wekaStable = "nz.ac.waikato.cms.weka" % "weka-stable" % "3.6.8" 
//val simplenlg = "simplenlg" % "simplenlg" % "4.3.0" 
val simplenlgdeps = Seq ( "org.json" % "json" % "20090211" )

//val openschema = "net.sf.openschema" % "openschema" % "0.0.3-SNAPSHOT" 
val openschemadeps = Seq( "org.apache.jena" % "jena-core" % "2.7.4" )

val jettyProject = "org.eclipse.jetty" % "jetty-project" % "9.0.0.RC1" 
val scalatra = "org.scalatra" %% "scalatra" % "2.3.0" 
val javaxServletApi = "javax.servlet" % "javax.servlet-api" % "3.0.1" 
val jettyServer = "org.eclipse.jetty" % "jetty-server" % "9.0.0.RC1" 
val jettyServlet = "org.eclipse.jetty" % "jetty-servlet" % "9.0.0.RC1" 
val jettyWebapp = "org.eclipse.jetty" % "jetty-webapp" % "9.0.0.RC1" 
val jettyJsp = "org.eclipse.jetty" % "jetty-jsp" % "9.0.0.RC1" 
val jettyAnnotations = "org.eclipse.jetty" % "jetty-annotations" % "9.0.0.RC1" 
val jettyPlus = "org.eclipse.jetty" % "jetty-plus" % "9.0.0.RC1" 
//val jlibsCore = "jlibs" % "jlibs-core" % "1.0" 
val jgrapht = "net.sf.jgrapht" % "jgrapht" % "0.8.3" 

lazy val root = project.in(file("."))
  .settings(commonSettings: _*)
  .settings(
    name := "thoughtland",
    resolvers += Resolver.url("ecl-releases", url("http://www.coli.uni-saarland.de/projects/ecl/maven2/releases")),
    /*resolvers += Resolver.url("jlibs-repository", url("https://raw.githubusercontent.com/santhosh-tekuri/maven-repository/")), */
    libraryDependencies ++= Seq(
      junit, specs2Core, specs2ScalaCheck,
    scalatest, guava, mahoutCore, mahoutIntegration, hadoopCore,
      mahoutExamples, wekaStable, /*simplenlg, openschema, */ jettyProject, scalatra, 
      javaxServletApi, jettyServer, jettyServlet, jettyWebapp, jettyJsp, jettyAnnotations, 
      jettyPlus, /* jlibsCore, */ jgrapht
    ) ++ openschemadeps
    /*libraryDependecies ++= simplenlgdeps, */
  )
//mainClass in assembly := Some("net.duboue.thoughtland.Main")

