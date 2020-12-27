import com.github.gv2011.util.ann.Artifact;

@Artifact(groupId = "com.github.gv2011.logback", artifactId = "logback-adapter")
module com.github.gv2011.logbackadapter{
  requires transitive com.github.gv2011.util;
  requires transitive org.slf4j;
  requires transitive ch.qos.logback.classic;
  requires transitive ch.qos.logback.core;

  requires java.logging;
  requires jul.to.slf4j;

  exports com.github.gv2011.logbackadapter to com.github.gv2011.util;

  provides ch.qos.logback.classic.spi.Configurator with com.github.gv2011.logbackadapter.LogbackConfigurator;
  provides com.github.gv2011.util.log.LogAdapter with com.github.gv2011.logbackadapter.LogbackLogAdapter;
}
