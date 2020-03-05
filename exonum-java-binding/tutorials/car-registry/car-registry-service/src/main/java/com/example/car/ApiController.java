/*
 * Copyright 2020 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.car;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import com.exonum.binding.core.service.Node;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

final class ApiController {

  private final MyService service;
  private final Node node;

  ApiController(MyService service, Node node) {
    this.service = service;
    this.node = node;
  }

  void mount(Router router) {
    router.get("/vehicle/:id").handler(this::findVehicle);
  }

  private void findVehicle(RoutingContext routingContext) {
    // Extract the requested vehicle ID
    var vehicleId = routingContext.pathParam("id");
    // Find it in the registry. The Node#withBlockchainData provides
    // the required context with the current, immutable database state.
    var vehicleOpt = node.withBlockchainData(
        (blockchainData) -> service.findVehicle(vehicleId, blockchainData));
    if (vehicleOpt.isPresent()) {
      // Respond with the vehicle details
      var vehicle = vehicleOpt.get();
      routingContext.response()
          .putHeader(CONTENT_TYPE, "application/octet-stream")
          .end(Buffer.buffer(vehicle.toByteArray()));
    } else {
      // Respond that the vehicle with such ID is not found
      routingContext.response()
          .setStatusCode(HTTP_NOT_FOUND)
          .end();
    }
  }
}
