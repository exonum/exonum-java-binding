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

package com.example.car.client;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;

import com.example.car.messages.VehicleOuterClass.Vehicle;
import com.exonum.client.ExonumClient;
import com.exonum.client.response.ServiceInstanceInfo;
import com.google.common.net.UrlEscapers;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.Callable;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "find-vehicle",
    aliases = {"fv"},
    description = {"Finds a vehicle in the registry by its ID"})
public class FindVehicleCommand implements Callable<Integer> {

  @ArgGroup(exclusive = true, multiplicity = "1")
  ServiceIds serviceIds;

  static class ServiceIds {
    @Option(names = "--service-name", description = "The service instance name") String name;
    @Option(names = "--service-id", description = "The service instance ID") int id;
  }

  @Parameters(index = "0", description = "Vehicle ID in the registry")
  String vehicleId;

  @Override
  public Integer call() throws Exception {
    var serviceName = findServiceName();
    var httpClient = HttpClient.newHttpClient();
    var findVehicleRequest = HttpRequest.newBuilder()
        .uri(buildRequestUri(serviceName))
        .GET()
        .build();
    var response = httpClient.send(findVehicleRequest, BodyHandlers.ofByteArray());
    var statusCode = response.statusCode();
    if (statusCode == HTTP_OK) {
      var vehicle = Vehicle.parseFrom(response.body());
      System.out.println("Vehicle: " + vehicle);
      return 0;
    } else if (statusCode == HTTP_NOT_FOUND) {
      System.out.printf("Vehicle with id (%s) is not found.%n", vehicleId);
      return statusCode;
    } else {
      System.out.println("Status code: " + statusCode);
      return statusCode;
    }
  }

  private String findServiceName() {
    if (serviceIds.name != null) {
      // The user graciously provided the name
      return serviceIds.name;
    }
    // The name is unset, look it up by ID using the node public API:
    var exonumClient = ExonumClient.newBuilder()
        .setExonumHost(Config.NODE_PUBLIC_API_HOST)
        .build();
    var serviceId = serviceIds.id;
    return exonumClient.getServiceInfoList().stream()
        .filter(info -> info.getId() == serviceId)
        .findFirst()
        .map(ServiceInstanceInfo::getName)
        .orElseThrow(() -> new IllegalArgumentException("No service with id=" + serviceId));
  }

  private URI buildRequestUri(String serviceName) {
    var escaper = UrlEscapers.urlPathSegmentEscaper();
    var uri = String.format("%s/api/services/%s/vehicle/%s",
        Config.NODE_JAVA_API_HOST, escaper.escape(serviceName), escaper.escape(vehicleId));
    return URI.create(uri);
  }
}
