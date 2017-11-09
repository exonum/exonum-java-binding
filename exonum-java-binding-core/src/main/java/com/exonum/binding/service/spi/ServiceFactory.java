package com.exonum.binding.service.spi;

import com.exonum.binding.service.Service;

/**
 * An SPI for a service factory.
 */
public interface ServiceFactory {
  Service createService();
}
