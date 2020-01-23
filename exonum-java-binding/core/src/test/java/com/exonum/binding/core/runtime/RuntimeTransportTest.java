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

package com.exonum.binding.core.runtime;

import static com.exonum.binding.core.runtime.ServiceRuntime.API_ROOT_PATH;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exonum.binding.core.transport.Server;
import io.vertx.ext.web.Router;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RuntimeTransportTest {

  private static final int PORT = 8080;

  @Mock private Server server;
  private RuntimeTransport transport;

  @BeforeEach
  void setUp() {
    transport = new RuntimeTransport(server, PORT);
  }

  @Test
  void start() {
    when(server.start(PORT)).thenReturn(CompletableFuture.completedFuture(PORT));

    transport.start();

    verify(server).start(PORT);
  }

  @Test
  void connectServiceApi() {
    Router serviceRouter = mock(Router.class);
    when(serviceRouter.getRoutes()).thenReturn(emptyList());
    when(server.createRouter()).thenReturn(serviceRouter);
    String serviceApiPath = "test-service";

    ServiceWrapper service = mock(ServiceWrapper.class);
    when(service.getPublicApiRelativePath()).thenReturn(serviceApiPath);

    transport.connectServiceApi(service);

    verify(service).createPublicApiHandlers(serviceRouter);
    verify(server).mountSubRouter(API_ROOT_PATH + "/" + serviceApiPath, serviceRouter);
  }

  @Test
  void disconnectServiceApi() {
    String serviceApiPath = "test-service";
    ServiceWrapper service = mock(ServiceWrapper.class);
    when(service.getPublicApiRelativePath()).thenReturn(serviceApiPath);

    transport.disconnectServiceApi(service);

    verify(server).removeSubRouter(API_ROOT_PATH + "/" + serviceApiPath);
  }

  @Test
  void close() throws InterruptedException {
    when(server.stop()).thenReturn(CompletableFuture.completedFuture(null));

    transport.close();

    verify(server).stop();
  }

  @Test
  void closeReportsOtherFailures() {
    CompletableFuture<Void> stopResult = new CompletableFuture<>();
    Throwable stopCause = new RuntimeException("Stop failure cause");
    stopResult.completeExceptionally(stopCause);

    when(server.stop()).thenReturn(stopResult);

    IllegalStateException e = assertThrows(IllegalStateException.class, () -> transport.close());

    assertThat(e).hasCause(stopCause);
  }
}
