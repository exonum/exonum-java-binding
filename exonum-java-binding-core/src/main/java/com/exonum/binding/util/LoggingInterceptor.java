package com.exonum.binding.util;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An interceptor, which logs any uncaught exceptions.
 */
public class LoggingInterceptor implements MethodInterceptor {

  private static final Logger logger = LogManager.getLogger(LoggingInterceptor.class);

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    try {
      return invocation.proceed();
    } catch (Throwable e) {
      logger.error("", e);
      throw e;
    }
  }
}
