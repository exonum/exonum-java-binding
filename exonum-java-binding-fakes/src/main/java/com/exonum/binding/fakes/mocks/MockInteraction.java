package com.exonum.binding.fakes.mocks;

import com.google.gson.Gson;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to make testing callbacks from native code easier. It allows simple tracking of
 * interaction with a specific method of a mocked object and then return it (by calling
 * getInteractions() method from native side) as a JSON string.
 */
public class MockInteraction {
  private final String[] arguments;
  private final List<String> interactions;

  private MockInteraction(String[] arguments) {
    this.arguments = arguments;
    interactions = new ArrayList<>();
  }

  /**
   * Creates new instange of {@link MockInteraction} configured with expected names of arguments for
   * a mocked method.
   *
   * @param expectedArguments Names of expected arguments for a mocked method in their right order.
   * @return New and configured instance of {@link MockInteraction}
   */
  public static MockInteraction createInteraction(String[] expectedArguments) {
    return new MockInteraction(expectedArguments);
  }

  /**
   * Custom Answer that is used to track interactions with a particular method of mocked object.
   */
  private class InteractionAnswer implements Answer {

    @Override
    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
      Object[] args = invocationOnMock.getArguments();
      int numArgs = args.length;

      assert numArgs == arguments.length;

      Gson gson = new Gson();
      StringBuilder sb = new StringBuilder();

      // A piece of custom serialization. Our goal - a string in form of "{"name":value}".
      sb.append('{');
      for (int i = 0; i < numArgs; i++) {
        sb.append(String.format("\"%s\":%s", arguments[i], gson.toJson(args[i])));
        if (i < numArgs - 1) {
          sb.append(',');
        }
      }
      sb.append('}');
      interactions.add(sb.toString());
      return null;
    }
  }

  /**
   * Returns list of all interactions (arguments values) with particular method of mocked object
   * in form of JSON string.
   *
   * Example: "[{"handle":4635874800,"height":1},{"handle":4635875424,"height":2}]"
   *
   * @return Result of interactions with mocked object.
   */
  public String getInteractions() {
    // We don't use Gson here because we need custom serialization for strings and it is simpler
    // to do it by hands
    StringBuilder sb = new StringBuilder();
    sb.append('[');
    for (int i = 0; i < interactions.size() ; i++) {
      sb.append(interactions.get(i));
      if (i < interactions.size() - 1) {
        sb.append(',');
      }
    }
    sb.append(']');
    return sb.toString();
  }

  public Answer createAnswer() {
    return new InteractionAnswer();
  }
}
