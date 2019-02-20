/*
 * Copyright 2019 The Exonum Team
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
 *
 */

package com.exonum.client;

/**
 * Contains Exonum API URLs.
 */
final class ExonumUrls {
  private static final String EXPLORER_PATHS_PREFIX = "/api/explorer/v1";
  private static final String SYS_PATHS_PREFIX = "/api/system/v1";
  static final String SUBMIT_TRANSACTION = EXPLORER_PATHS_PREFIX + "/transactions";
  static final String MEMORY_POOL = SYS_PATHS_PREFIX + "/mempool";
  static final String HEALTH_CHECK = SYS_PATHS_PREFIX + "/healthcheck";
  static final String USER_AGENT = SYS_PATHS_PREFIX + "/user_agent";

}
