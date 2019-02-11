package com.exonum.client;

import static com.google.common.base.MoreObjects.toStringHelper;

import com.google.gson.annotations.SerializedName;

/**
 * Json object wrapper for submit transaction request.
 */
class SubmitTxRequest {
  @SerializedName("tx_body")
  String body;

  SubmitTxRequest(String body) {
    this.body = body;
  }

  @Override
  public String toString() {
    return toStringHelper(this)
        .add("body", body)
        .toString();
  }
}
