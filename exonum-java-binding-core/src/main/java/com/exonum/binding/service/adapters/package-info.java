/**
 * An internal package with adapters of Java Service interfaces
 * to the interface, convenient to the native code. That brings such benefits:
 * <ul>
 *   <li>Separates user-facing interface and the framework implementation,
 *     enabling us to change them independently.
 *   <li>Provides the native code with a convenient interface (simpler, faster, more reliable).
 * </ul>
 *
 * <p>Also contains an utility factory to produce proxies of native views.
 */
package com.exonum.binding.service.adapters;
