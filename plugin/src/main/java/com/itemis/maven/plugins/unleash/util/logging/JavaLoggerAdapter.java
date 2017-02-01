package com.itemis.maven.plugins.unleash.util.logging;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class JavaLoggerAdapter extends Logger {
  private com.itemis.maven.plugins.cdi.logging.Logger log;

  public JavaLoggerAdapter(String name, com.itemis.maven.plugins.cdi.logging.Logger delegate) {
    super(name, null);
    this.log = delegate;
  }

  @Override
  public void log(LogRecord record) {
    if (!isLoggable(record.getLevel())) {
      return;
    }

    String message = record.getMessage();
    if (message != null && record.getParameters() != null) {
      MessageFormat formatter = new MessageFormat(message);
      message = formatter.format(record.getParameters());
    }

    if (isDebug(record.getLevel())) {
      this.log.debug(message, record.getThrown());
    } else if (isInfo(record.getLevel())) {
      this.log.info(message, record.getThrown());
    } else if (isWarn(record.getLevel())) {
      this.log.warn(message, record.getThrown());
    } else if (isError(record.getLevel())) {
      this.log.error(message, record.getThrown());
    }
  }

  @Override
  public void log(Level level, String msg) {
    log(new LogRecord(level, msg));
  }

  // @Override
  // public void log(Level level, Supplier<String> msgSupplier) {
  // log(new LogRecord(level, msgSupplier.get()));
  // }

  @Override
  public void log(Level level, String msg, Object param1) {
    LogRecord record = new LogRecord(level, msg);
    record.setParameters(new Object[] { param1 });
    log(record);
  }

  @Override
  public void log(Level level, String msg, Object[] params) {
    LogRecord record = new LogRecord(level, msg);
    record.setParameters(params);
    log(record);
  }

  @Override
  public void log(Level level, String msg, Throwable thrown) {
    LogRecord record = new LogRecord(level, msg);
    record.setThrown(thrown);
    log(record);
  }

  // @Override
  // public void log(Level level, Throwable thrown, Supplier<String> msgSupplier) {
  // LogRecord record = new LogRecord(level, msgSupplier.get());
  // record.setThrown(thrown);
  // log(record);
  // }

  @Override
  public Level getLevel() {
    if (this.log.isDebugEnabled()) {
      return Level.ALL;
    }
    if (this.log.isInfoEnabled()) {
      return Level.INFO;
    }
    if (this.log.isWarnEnabled()) {
      return Level.WARNING;
    }
    if (this.log.isErrorEnabled()) {
      return Level.SEVERE;
    }
    return Level.OFF;
  }

  @Override
  public boolean isLoggable(Level level) {
    if (isDebug(level) && this.log.isDebugEnabled()) {
      return true;
    } else if (isInfo(level) && this.log.isInfoEnabled()) {
      return true;
    } else if (isWarn(level) && this.log.isWarnEnabled()) {
      return true;
    } else if (isError(level) && this.log.isErrorEnabled()) {
      return true;
    }
    return false;
  }

  private boolean isDebug(Level level) {
    return level.intValue() <= Level.CONFIG.intValue();
  }

  private boolean isInfo(Level level) {
    int value = level.intValue();
    return value <= Level.INFO.intValue() && value > Level.CONFIG.intValue();
  }

  private boolean isWarn(Level level) {
    int value = level.intValue();
    return value <= Level.WARNING.intValue() && value > Level.INFO.intValue();
  }

  private boolean isError(Level level) {
    int value = level.intValue();
    return value <= Level.SEVERE.intValue() && value > Level.WARNING.intValue();
  }
}
