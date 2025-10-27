package com.stadiamaps.ferrostar.core

import uniffi.ferrostar.NavState
import uniffi.ferrostar.NavigationCache
import uniffi.ferrostar.NavigationCachingConfig
import uniffi.ferrostar.NavigationControllerConfig
import uniffi.ferrostar.NavigationObserver
import uniffi.ferrostar.NavigationRecorder
import uniffi.ferrostar.NavigationSession
import uniffi.ferrostar.NavigationSessionCache
import uniffi.ferrostar.Route
import uniffi.ferrostar.createNavigationSession

/**
 * Create a navigation session builder.
 *
 * This class generates navigation sessions inside FerrostarCore when navigation starts or resumes.
 *
 * @param config The default Navigation Controller configuration.
 */
class FerrostarSessionBuilder(private var config: NavigationControllerConfig) {

  private var caching: NavigationSessionCache? = null
  private var recorder: NavigationRecorder? = null
  private var custom = emptyList<NavigationObserver>()

  /**
   * Determine if there's a valid navigation snapshot. This is a getter that checks the cache, so
   * use it sparingly.
   *
   * @return True if there's a fresh snapshot, false otherwise.
   */
  val canResume: Boolean
    get() = caching?.canResume() ?: false

  /**
   * Add navigation session caching to the session.
   *
   * This can be used to resume navigation from a sessions snapshot.
   *
   * @param config The caching configuration
   * @param cache The platform specific caching
   * @return The modified session builder.
   */
  fun withCaching(
      config: NavigationCachingConfig,
      cache: NavigationCache
  ): FerrostarSessionBuilder {
    this.caching = NavigationSessionCache(config, cache)
    return this
  }

  /**
   * Add a navigation session recorder to the session.
   *
   * The navigation session recorder logs every navigation event for playback in the Ferrostar web
   * project. This is useful for detailed debugging and analysis, but should be disabled in
   * production as it's a very expensive system.
   *
   * @param recorder The navigation session recorder.
   * @return The modified session builder.
   */
  fun withRecorder(recorder: NavigationRecorder): FerrostarSessionBuilder {
    this.recorder = recorder
    return this
  }

  /**
   * Add a Custom navigation observer.
   *
   * @param observer Any NavigationObserver
   * @return The modified session builder.
   */
  fun withObserver(observer: NavigationObserver): FerrostarSessionBuilder {
    this.custom = this.custom + observer
    return this
  }

  fun buildResumedSession(): Triple<NavigationSession, Route, NavState> {
    val record = caching?.load() ?: throw NoCachedSession()
    val tripState = record.tripState ?: throw NoCachedSession()

    val session = build(record.route)
    val navState = NavState(tripState, this.config.stepAdvanceCondition)
    return Triple(session, record.route, navState)
  }

  fun build(route: Route, config: NavigationControllerConfig? = null): NavigationSession {
    val observers: List<NavigationObserver> = listOfNotNull(caching, recorder) + custom

    config?.let { this.config = it }

    return createNavigationSession(route, config ?: this.config, observers)
  }
}
