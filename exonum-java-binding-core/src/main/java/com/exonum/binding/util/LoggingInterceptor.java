/*
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.util;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An interceptor, which logs any uncaught exceptions.
 */
public final class LoggingInterceptor implements MethodInterceptor {

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
