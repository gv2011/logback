package com.github.gv2011.logbackadapter;

import com.github.gv2011.util.log.LogAdapter;
import com.github.gv2011.util.serviceloader.RecursiveServiceLoader;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.core.spi.ContextAwareBase;

public final class LogbackConfigurator extends ContextAwareBase implements Configurator{

  @Override
  public void configure(final LoggerContext loggerContext) {
    ((LogbackLogAdapter) RecursiveServiceLoader.service(LogAdapter.class)).configure(loggerContext);
  }

}
