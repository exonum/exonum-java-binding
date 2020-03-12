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

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

import com.exonum.client.ExonumClient;
import com.exonum.client.response.ServiceInstanceInfo;
import java.util.Optional;
import java.util.function.Predicate;

final class ServiceIdResolver {

  private final ServiceIds unresolvedIds;
  private final ExonumClient client;

  ServiceIdResolver(ServiceIds unresolvedIds, ExonumClient client) {
    checkArgument(unresolvedIds.hasId() || unresolvedIds.hasName());
    this.unresolvedIds = unresolvedIds;
    this.client = client;
  }

  int getId() {
    if (unresolvedIds.hasId()) {
      return unresolvedIds.id;
    }
    var name = unresolvedIds.name;
    return findByName(name)
        .map(ServiceInstanceInfo::getId)
        .orElseThrow(() -> new IllegalStateException(
            format("No service with name (%s)", name)));
  }

  String getName() {
    if (unresolvedIds.hasName()) {
      return unresolvedIds.name;
    }
    var id = unresolvedIds.id;
    return findById(id)
        .map(ServiceInstanceInfo::getName)
        .orElseThrow(() -> new IllegalStateException(
            format("No service with id (%s)", id)));
  }

  private Optional<ServiceInstanceInfo> findByName(String serviceName) {
    return findBy(info -> info.getName().equals(serviceName));
  }

  private Optional<ServiceInstanceInfo> findById(int serviceId) {
    return findBy(info -> info.getId() == serviceId);
  }

  private Optional<ServiceInstanceInfo> findBy(Predicate<? super ServiceInstanceInfo> predicate) {
    return client.getServiceInfoList().stream()
        .filter(predicate)
        .findFirst();
  }
}
