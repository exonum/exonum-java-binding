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

package com.exonum.binding.common.message;

import com.exonum.binding.common.hash.Funnel;
import com.exonum.binding.common.hash.PrimitiveSink;
import com.google.protobuf.ByteString;

/** A funnel for ByteStrings which puts their bytes to the sink without copying. */
enum ByteStringFunnel implements Funnel<ByteString> {
  INSTANCE;

  static Funnel<ByteString> byteStringFunnel() {
    return INSTANCE;
  }

  @Override
  public void funnel(ByteString from, PrimitiveSink into) {
    // We use #asReadOnlyByteBuffer instead of #asReadOnlyByteBufferList because
    // it is not expected that in our main usages of this funnel a RopeByteString will
    // appear.
    // If this funnel is used more widely, it might be reasonable to use #asReadOnlyByteBufferList
    // to prevent copying in case of RopeByteString.
    into.putBytes(from.asReadOnlyByteBuffer());
  }
}
