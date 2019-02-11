package com.exonum.client;

import static com.google.common.base.MoreObjects.toStringHelper;

import com.exonum.binding.common.hash.HashCode;
import com.google.gson.annotations.SerializedName;

/**
 * Json object wrapper for submit transaction response.
 */
class SubmitTxResponse {
  @SerializedName("tx_hash")
  HashCode hash;

  @Override
  public String toString() {
    return toStringHelper(this)
        .add("hash", hash)
        .toString();
  }
}
