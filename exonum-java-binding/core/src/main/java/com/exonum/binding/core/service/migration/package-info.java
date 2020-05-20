/*
 * Copyright 2020 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * <h1>Migrations Overview</h1>
 * The goal of a data migration is to prepare data of an Exonum service for use with an updated
 * version of the service business logic. In this sense, migrations fulfil the same role
 * as migrations in traditional database management systems and have the similar mechanism:
 *
 * <ul>
 *   <li>Migration scripts will be applied to service data in a specific order during evolution
 *   of a particular service instance. Each script will be applied exactly once.</li>
 *   <li>Several migration scripts may be applied sequentially if the instance is old enough.</li>
 *   <li>Migrations for different service instances are independent, and migration scripts for them
 *   are fully reusable.</li>
 * </ul>
 *
 * <p/>
 * Migrations are performed via {@link com.exonum.binding.core.service.migration.MigrationScript}'s.
 * A script takes data of a service and transforms it to a new version.
 * Migration is non-destructive, i.e., does not remove the old versions of migrated indexes.
 * Instead, new indexes are created in a separate namespace, and atomically replace the old data
 * when the migration is flushed.
 *
 * <p/>
 * Similar to other service lifecycle events, data migrations are managed by the
 * {@linkplain com.exonum.binding.core.runtime.ServiceRuntime runtime}.
 *
 * <p/>
 * <h1>Migration Workflow</h1>
 * Migration starts after the block with the request is committed and is performed asynchronously.
 *
 * <p/>
 * After the local migration completion, validator nodes report the result of migration, which can
 * be either successful or unsuccessful.
 *
 * <p/>
 * If all validators report the successful local migration result, and the resulting state hashes
 * match, migration is committed and flushed in the block, next to block with the last required
 * migration confirmation.
 *
 * <p/>
 * In any other case (e.g. migration failure for at least one node, resulting state hash divergence,
 * lack of report at the deadline height), migration is considered failed and rolled back.
 *
 * <p/>
 * After fixing the reason for migration failure, the migration attempt can be performed once again.
 * It will require a different deadline height or a different seed, since `MigrationRequest` objects
 * are considered unique and supervisor won't attempt to perform the same `MigrationRequest` again.
 *
 * <p/>
 * <h2>Complex Migrations</h2>
 * If a service module contains more than one migration script (e.g. if you need to migrate service
 * from version 0.1 to version 0.3, and this will include execution of two migration scripts:
 * 0.1 -> 0.2 and 0.2 -> 0.3), they will be executed sequentially, one migration script at the time.
 *
 * <p/>
 * <h2>Incomplete Migrations</h2>
 * Migrations require only the current and the last version of artifact to be deployed. If you
 * decide to stop migration before reaching the last version (e.g. you requested migration to
 * version 0.3, but decided to go with version 0.2), you will need to deploy the 0.2 artifact
 * in order to resume the migrated service.
 *
 * <p/>
 * <h1>Examples</h1>
 * Consider the following hypothetical evolution of a service:
 *
 * <table>
 *  <tr>
 *    <th>Version</th><th>Migration</th>
 *  </tr>
 *  <tr>
 *    <td>0.2.0</td><td>#1: Split `name` in user accounts into `first_name` and `last_name`</td>
 *  </tr>
 *  <tr>
 *    <td>0.3.0</td><td> - </td>
 *  </tr>
 *  <tr>
 *    <td>0.4.0</td><td>#2: Consolidate token metadata into a single `Entry`</td>
 *  </tr>
 *  <tr>
 *    <td>0.4.1</td><td> - </td>
 *  </tr>
 *  <tr>
 *    <td>0.4.2</td><td> #3: Compute total number of tokens and add it to metadata</td>
 *  </tr>
 * </table>
 *
 * <p/>
 * In this case:
 * <ul>
 *  <li>If a service instance is migrated from version 0.1.0 to the newest version 0.4.2, all three
 *  scripts need to be executed.</li>
 *  <li>If an instance is migrated from 0.2.0 or 0.3.0, only scripts #2 and #3 need to be executed.
 *  </li>
 *  <li>If an instance is migrated from 0.4.0 or 0.4.1, only script #3 needs to be executed.</li>
 *  <li>If the instance version is 0.4.2, no scripts need to be executed.</li>
 * </ul>
 *
 * @see <a href="https://exonum.com/doc/version/latest/architecture/services/#data-migrations">
 *Exonum documentation</a>
 **/
package com.exonum.binding.core.service.migration;
