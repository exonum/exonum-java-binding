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

import com.google.common.base.Preconditions;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class NodeUserAgentResponseParser {
  private static final Pattern USER_INFO_REGEX =
      Pattern.compile("^exonum(?<exonum>[^/]+)/rustc (?<rust>[^/]+)/(?<os>.+)$");

  static NodeUserAgentResponse parseFrom(String input) {
    Matcher matcher = USER_INFO_REGEX.matcher(Preconditions.checkNotNull(input));

    if (!matcher.find()) {
      throw new IllegalArgumentException("Can't parse input string: " + input);
    }

    return new NodeUserAgentResponse(
        matcher.group("exonum").trim(),
        matcher.group("rust").trim(),
        matcher.group("os").trim()
    );
  }

}
