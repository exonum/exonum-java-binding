package com.exonum.binding.proxy;

class MapEntry {
  byte[] key;
  byte[] value;

  MapEntry(byte[] key, byte[] value) {
    this.key = key;
    this.value = value;
  }
}
