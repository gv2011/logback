package com.github.gv2011.logbackadapter;

import static com.github.gv2011.util.Verify.notNull;
import static com.github.gv2011.util.Verify.verify;
import static com.github.gv2011.util.ex.Exceptions.call;
import static com.github.gv2011.util.ex.Exceptions.callWithCloseable;
import static com.github.gv2011.util.ex.Exceptions.format;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.github.gv2011.util.ann.Nullable;
import com.github.gv2011.util.bytes.ByteUtils;
import com.github.gv2011.util.bytes.Bytes;
import com.github.gv2011.util.bytes.Hash256;
import com.github.gv2011.util.filewatch.FileWatchService;
import com.github.gv2011.util.log.LogAdapter;
import com.github.gv2011.util.serviceloader.RecursiveServiceLoader;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.WarnStatus;

public class LogbackLogAdapter implements LogAdapter{

  private static final Path CONFIG_FILE = Paths.get("logback.xml").toAbsolutePath();

  private final Object lock = new Object();

  private boolean closing;

  private @Nullable Hash256 configHash = null;

  private @Nullable LoggerContext loggerContext = null;

  private @Nullable FileWatchService fileWatchService = null;

  @Override
  public void ensureInitialized() {
    synchronized(lock){
      final Logger logger = getLogger(LogbackLogAdapter.class);
      if(loggerContext==null){
        loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
      }
      if(configHash!=null){
        logger.info("Logback initialized.");
        fileWatchService = RecursiveServiceLoader.service(FileWatchService.class);
        fileWatchService.watch(CONFIG_FILE, notNull(configHash), this::reconfigure);
      }
      else logger.info("Logback initialized with external configuration.");
    }
  }

  @Override
  public void close() {
    synchronized(lock){
      closing = true;
      getLogger(LogbackLogAdapter.class).info("Stopping Logback.");
      ((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
    }
  }

  void configure(final LoggerContext loggerContext) {
    synchronized(lock){
      verify(!closing);
      try{
        this.loggerContext = notNull(loggerContext);
        if(!Files.exists(CONFIG_FILE)){
          final URL srcUrl = notNull(getClass().getResource("logback-default.xml"));
          call(()->{
            try(InputStream in = srcUrl.openStream()){
              Files.copy(in, CONFIG_FILE);
            }
          });
          loggerContext.getStatusManager().add(new WarnStatus(
            format("Logback configuration file {} did not exist, copied default from {}.", CONFIG_FILE, srcUrl),
            LogbackLogAdapter.class.getName()
          ));
        }
        loggerContext.getStatusManager().add(new InfoStatus(
          format("Configuring logback from file {}.", CONFIG_FILE),
          LogbackLogAdapter.class.getName()
        ));
        final Bytes configuration = ByteUtils.read(CONFIG_FILE);
        doConfigure(configuration);
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
      }
      catch(final Exception e){loggerContext.getStatusManager().add(
        new ErrorStatus("Could not configure logging.", LogbackLogAdapter.class.getName(), e)
      );}
      //StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);
    }
  }

  private void doConfigure(final Bytes configuration) {
    final Hash256 newHash = configuration.hash();
    verify(!newHash.equals(configHash));
    final JoranConfigurator configurator = new JoranConfigurator();
    configurator.setContext(notNull(loggerContext));
    callWithCloseable(configuration::openStream, s->{configurator.doConfigure(s);});
    configHash = newHash;
  }

  private boolean reconfigure(final Bytes config) {
    synchronized(lock){
      if(!closing){
        notNull(loggerContext);
        final Logger logger = getLogger(LogbackLogAdapter.class);
        final Hash256 newHash = config.hash();
        verify(!newHash.equals(configHash));
        logger.info("Reconfiguring logback.");
        loggerContext.reset();
        final JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(loggerContext);
        callWithCloseable(config::openStream, s->{configurator.doConfigure(s);});
        logger.info("Reconfigured logback.");
        configHash = newHash;
      }
      return !closing;
    }
  }

}
