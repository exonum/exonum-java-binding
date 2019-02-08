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
 */

package com.exonum.binding.fakes.mocks;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Helper class to make testing callbacks from native code easier. It allows simple tracking of
 * interaction with a specific method of a mocked object and then return it (by calling
 * getInteractions() method from native side) as a JSON string.
 */
@SuppressWarnings("unused") // Native API
public class MockInteraction {
  private static final Gson GSON = new Gson();

  private final String[] argumentNames;
  private final List<JsonElement> interactions = new ArrayList<>();

  /**
   * Creates new instance of {@link MockInteraction} configured with expected names of arguments for
   * a mocked method.
   *
   * @param argumentNames names of expected arguments for a mocked method in their right order
   */
  public MockInteraction(String[] argumentNames) {
    this.argumentNames = argumentNames;
  }

  /**
   * Custom Answer that is used to track interactions with a particular method of mocked object.
   */
  private class InteractionAnswer implements Answer {

    @Override
    public Object answer(InvocationOnMock invocationOnMock) {
      Object[] argValues = invocationOnMock.getArguments();
      int numArgs = argValues.length;

      assert numArgs == argumentNames.length;

      Map<String, Object> arguments = new HashMap<>();
      for (int i = 0; i < numArgs; i++) {
        arguments.put(argumentNames[i], argValues[i]);
      }

      // Serialize to JSON immediately so that any later modifications to the passed objects
      // do not affect the value.
      interactions.add(GSON.toJsonTree(arguments));
      return null;
    }
  }

  /**
   * Returns list of all interactions (arguments values) with particular method of mocked object
   * in form of JSON string.
   *
   * <p>Example: "[{"handle":4635874800,"height":1},{"handle":4635875424,"height":2}]"</p>
   *
   * @return Result of interactions with mocked object
   */
  public String getInteractions() {
    return GSON.toJson(interactions);
  }

  public Answer createAnswer() {
    return new InteractionAnswer();
  }
}
