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

package com.exonum.binding.common.message;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.io.BaseEncoding;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;

/**
 * Auto-generated superclass of {@link Message.Builder}, derived from the API of {@link Message}.
 *
 * <p>This code is mostly auto-generated with FreeBuilder.
 * It's checked in and modified because ByteBuffers and byte arrays are mutable objects and need to
 * be copied (see {@link Value#getBody()} & {@link Value#getSignature}).
 */
abstract class Message_Builder2 {
  
  /** Creates a new builder using {@code value} as a template. */
  public static Message.Builder from(Message value) {
    return new Message.Builder().mergeFrom(value);
  }

  private static final Joiner COMMA_JOINER = Joiner.on(", ").skipNulls();

  private static final BaseEncoding HEX_ENCODING = BaseEncoding.base16().lowerCase();

  private enum Property {
    NETWORK_ID("networkId"),
    VERSION("version"),
    SERVICE_ID("serviceId"),
    MESSAGE_TYPE("messageType"),
    BODY("body"),
    SIGNATURE("signature"),
    ;

    private final String name;

    private Property(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  private byte networkId;
  private byte version;
  private short serviceId;
  private short messageType;
  private ByteBuffer body;
  private byte[] signature;
  private final EnumSet<Message_Builder2.Property> _unsetProperties =
      EnumSet.allOf(Message_Builder2.Property.class);

  /**
   * Sets the value to be returned by {@link Message#getNetworkId()}.
   *
   * @return this {@code Builder} object
   */
  public Message.Builder setNetworkId(byte networkId) {
    this.networkId = networkId;
    _unsetProperties.remove(Message_Builder2.Property.NETWORK_ID);
    return (Message.Builder) this;
  }

  /**
   * Returns the value that will be returned by {@link Message#getNetworkId()}.
   *
   * @throws IllegalStateException if the field has not been set
   */
  public byte getNetworkId() {
    Preconditions.checkState(
        !_unsetProperties.contains(Message_Builder2.Property.NETWORK_ID), "networkId not set");
    return networkId;
  }

  /**
   * Sets the value to be returned by {@link Message#getVersion()}.
   *
   * @return this {@code Builder} object
   */
  public Message.Builder setVersion(byte version) {
    this.version = version;
    _unsetProperties.remove(Message_Builder2.Property.VERSION);
    return (Message.Builder) this;
  }

  /**
   * Returns the value that will be returned by {@link Message#getVersion()}.
   *
   * @throws IllegalStateException if the field has not been set
   */
  public byte getVersion() {
    Preconditions.checkState(
        !_unsetProperties.contains(Message_Builder2.Property.VERSION), "version not set");
    return version;
  }

  /**
   * Sets the value to be returned by {@link Message#getServiceId()}.
   *
   * @return this {@code Builder} object
   */
  public Message.Builder setServiceId(short serviceId) {
    this.serviceId = serviceId;
    _unsetProperties.remove(Message_Builder2.Property.SERVICE_ID);
    return (Message.Builder) this;
  }

  /**
   * Returns the value that will be returned by {@link Message#getServiceId()}.
   *
   * @throws IllegalStateException if the field has not been set
   */
  public short getServiceId() {
    Preconditions.checkState(
        !_unsetProperties.contains(Message_Builder2.Property.SERVICE_ID), "serviceId not set");
    return serviceId;
  }

  /**
   * Sets the value to be returned by {@link Message#getMessageType()}.
   *
   * @return this {@code Builder} object
   */
  public Message.Builder setMessageType(short messageType) {
    this.messageType = messageType;
    _unsetProperties.remove(Message_Builder2.Property.MESSAGE_TYPE);
    return (Message.Builder) this;
  }

  /**
   * Returns the value that will be returned by {@link Message#getMessageType()}.
   *
   * @throws IllegalStateException if the field has not been set
   */
  public short getMessageType() {
    Preconditions.checkState(
        !_unsetProperties.contains(Message_Builder2.Property.MESSAGE_TYPE), "messageType not set");
    return messageType;
  }

  /**
   * Sets the value to be returned by {@link Message#getBody()}.
   *
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code body} is null
   */
  public Message.Builder setBody(ByteBuffer body) {
    this.body = Preconditions.checkNotNull(body);
    _unsetProperties.remove(Message_Builder2.Property.BODY);
    return (Message.Builder) this;
  }

  /**
   * Returns the value that will be returned by {@link Message#getBody()}.
   *
   * @throws IllegalStateException if the field has not been set
   */
  public ByteBuffer getBody() {
    Preconditions.checkState(
        !_unsetProperties.contains(Message_Builder2.Property.BODY), "body not set");
    return body;
  }

  /**
   * Sets the value to be returned by {@link Message#getSignature()}.
   *
   * @return this {@code Builder} object
   * @throws NullPointerException if {@code signature} is null
   */
  public Message.Builder setSignature(byte[] signature) {
    this.signature = signature.clone();
    _unsetProperties.remove(Message_Builder2.Property.SIGNATURE);
    return (Message.Builder) this;
  }

  /**
   * Returns the value that will be returned by {@link Message#getSignature()}.
   *
   * @throws IllegalStateException if the field has not been set
   */
  public byte[] getSignature() {
    Preconditions.checkState(
        !_unsetProperties.contains(Message_Builder2.Property.SIGNATURE), "signature not set");
    return signature;
  }

  /** Sets all property values using the given {@code Message} as a template. */
  public Message.Builder mergeFrom(Message value) {
    Message_Builder2 _defaults = new Message.Builder();
    if (_defaults._unsetProperties.contains(Message_Builder2.Property.NETWORK_ID)
        || !Objects.equals(value.getNetworkId(), _defaults.getNetworkId())) {
      setNetworkId(value.getNetworkId());
    }
    if (_defaults._unsetProperties.contains(Message_Builder2.Property.VERSION)
        || !Objects.equals(value.getVersion(), _defaults.getVersion())) {
      setVersion(value.getVersion());
    }
    if (_defaults._unsetProperties.contains(Message_Builder2.Property.SERVICE_ID)
        || !Objects.equals(value.getServiceId(), _defaults.getServiceId())) {
      setServiceId(value.getServiceId());
    }
    if (_defaults._unsetProperties.contains(Message_Builder2.Property.MESSAGE_TYPE)
        || !Objects.equals(value.getMessageType(), _defaults.getMessageType())) {
      setMessageType(value.getMessageType());
    }
    if (_defaults._unsetProperties.contains(Message_Builder2.Property.BODY)
        || !Objects.equals(value.getBody(), _defaults.getBody())) {
      setBody(value.getBody());
    }
    if (_defaults._unsetProperties.contains(Message_Builder2.Property.SIGNATURE)
        || !Arrays.equals(value.getSignature(), _defaults.getSignature())) {
      setSignature(value.getSignature());
    }
    return (Message.Builder) this;
  }

  /**
   * Copies values from the given {@code Builder}. Does not affect any properties not set on the
   * input.
   */
  public Message.Builder mergeFrom(Message.Builder template) {
    // Upcast to access private fields; otherwise, oddly, we get an access violation.
    Message_Builder2 base = template;
    Message_Builder2 _defaults = new Message.Builder();
    if (!base._unsetProperties.contains(Message_Builder2.Property.NETWORK_ID)
        && (_defaults._unsetProperties.contains(Message_Builder2.Property.NETWORK_ID)
        || !Objects.equals(template.getNetworkId(), _defaults.getNetworkId()))) {
      setNetworkId(template.getNetworkId());
    }
    if (!base._unsetProperties.contains(Message_Builder2.Property.VERSION)
        && (_defaults._unsetProperties.contains(Message_Builder2.Property.VERSION)
        || !Objects.equals(template.getVersion(), _defaults.getVersion()))) {
      setVersion(template.getVersion());
    }
    if (!base._unsetProperties.contains(Message_Builder2.Property.SERVICE_ID)
        && (_defaults._unsetProperties.contains(Message_Builder2.Property.SERVICE_ID)
        || !Objects.equals(template.getServiceId(), _defaults.getServiceId()))) {
      setServiceId(template.getServiceId());
    }
    if (!base._unsetProperties.contains(Message_Builder2.Property.MESSAGE_TYPE)
        && (_defaults._unsetProperties.contains(Message_Builder2.Property.MESSAGE_TYPE)
        || !Objects.equals(template.getMessageType(), _defaults.getMessageType()))) {
      setMessageType(template.getMessageType());
    }
    if (!base._unsetProperties.contains(Message_Builder2.Property.BODY)
        && (_defaults._unsetProperties.contains(Message_Builder2.Property.BODY)
        || !Objects.equals(template.getBody(), _defaults.getBody()))) {
      setBody(template.getBody());
    }
    if (!base._unsetProperties.contains(Message_Builder2.Property.SIGNATURE)
        && (_defaults._unsetProperties.contains(Message_Builder2.Property.SIGNATURE)
        || !Arrays.equals(template.getSignature(), _defaults.getSignature()))) {
      setSignature(template.getSignature());
    }
    return (Message.Builder) this;
  }

  /** Resets the state of this builder. */
  public Message.Builder clear() {
    Message_Builder2 _defaults = new Message.Builder();
    networkId = _defaults.networkId;
    version = _defaults.version;
    serviceId = _defaults.serviceId;
    messageType = _defaults.messageType;
    body = _defaults.body;
    signature = _defaults.signature;
    _unsetProperties.clear();
    _unsetProperties.addAll(_defaults._unsetProperties);
    return (Message.Builder) this;
  }

  /**
   * Returns a newly-created {@link Message} based on the contents of the {@code Builder}.
   *
   * @throws IllegalStateException if any field has not been set
   */
  public Message build() {
    Preconditions.checkState(_unsetProperties.isEmpty(), "Not set: %s", _unsetProperties);
    return new Message_Builder2.Value(this);
  }

  /**
   * Returns a newly-created partial {@link Message} for use in unit tests. State checking will not
   * be performed. Unset properties will throw an {@link UnsupportedOperationException} when
   * accessed via the partial object.
   *
   * <p>Partials should only ever be used in tests. They permit writing robust test cases that won't
   * fail if this type gains more application-level constraints (e.g. new required fields) in
   * future. If you require partially complete values in production code, consider using a Builder.
   */
  @VisibleForTesting()
  public Message buildPartial() {
    return new Message_Builder2.Partial(this);
  }


  @VisibleForTesting
  static final class Value implements Message {
    private final byte networkId;
    private final byte version;
    private final short serviceId;
    private final short messageType;
    private final ByteBuffer body;
    private final byte[] signature;

    private Value(Message_Builder2 builder) {
      this.networkId = builder.networkId;
      this.version = builder.version;
      this.serviceId = builder.serviceId;
      this.messageType = builder.messageType;
      this.body = builder.body;
      this.signature = builder.signature;
    }

    @Override
    public byte getNetworkId() {
      return networkId;
    }

    @Override
    public byte getVersion() {
      return version;
    }

    @Override
    public short getServiceId() {
      return serviceId;
    }

    @Override
    public short getMessageType() {
      return messageType;
    }

    @Override
    public ByteBuffer getBody() {
      return body.duplicate();
    }

    @Override
    public byte[] getSignature() {
      return signature.clone();
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Message_Builder2.Value)) {
        return false;
      }
      Message_Builder2.Value other = (Message_Builder2.Value) obj;
      return Objects.equals(networkId, other.networkId)
          && Objects.equals(version, other.version)
          && Objects.equals(serviceId, other.serviceId)
          && Objects.equals(messageType, other.messageType)
          && Objects.equals(body, other.body)
          && Arrays.equals(signature, other.signature);
    }

    @Override
    public int hashCode() {
      int signatureHash = Arrays.hashCode(signature);
      return Objects.hash(networkId, version, serviceId, messageType, body, signatureHash);
    }

    @Override
    public String toString() {
      return "Message{"
          + "networkId="
          + networkId
          + ", "
          + "version="
          + version
          + ", "
          + "serviceId="
          + serviceId
          + ", "
          + "messageType="
          + messageType
          + ", "
          + "body="
          + body
          + ", "
          + "signature="
          + HEX_ENCODING.encode(signature)
          + "}";
    }
  }

  @VisibleForTesting
  static final class Partial implements Message {
    private final byte networkId;
    private final byte version;
    private final short serviceId;
    private final short messageType;
    private final ByteBuffer body;
    private final byte[] signature;
    private final EnumSet<Message_Builder2.Property> _unsetProperties;

    Partial(Message_Builder2 builder) {
      this.networkId = builder.networkId;
      this.version = builder.version;
      this.serviceId = builder.serviceId;
      this.messageType = builder.messageType;
      this.body = builder.body;
      this.signature = builder.signature;
      this._unsetProperties = builder._unsetProperties.clone();
    }

    @Override
    public byte getNetworkId() {
      if (_unsetProperties.contains(Message_Builder2.Property.NETWORK_ID)) {
        throw new UnsupportedOperationException("networkId not set");
      }
      return networkId;
    }

    @Override
    public byte getVersion() {
      if (_unsetProperties.contains(Message_Builder2.Property.VERSION)) {
        throw new UnsupportedOperationException("version not set");
      }
      return version;
    }

    @Override
    public short getServiceId() {
      if (_unsetProperties.contains(Message_Builder2.Property.SERVICE_ID)) {
        throw new UnsupportedOperationException("serviceId not set");
      }
      return serviceId;
    }

    @Override
    public short getMessageType() {
      if (_unsetProperties.contains(Message_Builder2.Property.MESSAGE_TYPE)) {
        throw new UnsupportedOperationException("messageType not set");
      }
      return messageType;
    }

    @Override
    public ByteBuffer getBody() {
      if (_unsetProperties.contains(Message_Builder2.Property.BODY)) {
        throw new UnsupportedOperationException("body not set");
      }
      return body.duplicate();
    }

    @Override
    public byte[] getSignature() {
      if (_unsetProperties.contains(Message_Builder2.Property.SIGNATURE)) {
        throw new UnsupportedOperationException("signature not set");
      }
      return signature.clone();
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Message_Builder2.Partial)) {
        return false;
      }
      Message_Builder2.Partial other = (Message_Builder2.Partial) obj;
      return Objects.equals(networkId, other.networkId)
          && Objects.equals(version, other.version)
          && Objects.equals(serviceId, other.serviceId)
          && Objects.equals(messageType, other.messageType)
          && Objects.equals(body, other.body)
          && Arrays.equals(signature, other.signature)
          && Objects.equals(_unsetProperties, other._unsetProperties);
    }

    @Override
    public int hashCode() {
      int signatureHash = Arrays.hashCode(signature);
      return Objects.hash(
          networkId, version, serviceId, messageType, body, signatureHash, _unsetProperties);
    }

    @Override
    public String toString() {
      return "partial Message{"
          + COMMA_JOINER.join(
          (!_unsetProperties.contains(Message_Builder2.Property.NETWORK_ID)
              ? "networkId=" + networkId
              : null),
          (!_unsetProperties.contains(Message_Builder2.Property.VERSION)
              ? "version=" + version
              : null),
          (!_unsetProperties.contains(Message_Builder2.Property.SERVICE_ID)
              ? "serviceId=" + serviceId
              : null),
          (!_unsetProperties.contains(Message_Builder2.Property.MESSAGE_TYPE)
              ? "messageType=" + messageType
              : null),
          (!_unsetProperties.contains(Message_Builder2.Property.BODY) ? "body=" + body : null),
          (!_unsetProperties.contains(Message_Builder2.Property.SIGNATURE)
              ? "signature=" + HEX_ENCODING.encode(signature)
              : null))
          + "}";
    }
  }
}
