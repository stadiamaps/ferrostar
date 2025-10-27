package com.stadiamaps.ferrostar.core

import uniffi.ferrostar.GraphHopperVoiceUnits

sealed class RoutingEngine {
  data class Valhalla(val endpoint: String, val profile: String) : RoutingEngine()

  data class GraphHopper(
      val endpoint: String,
      val profile: String,
      val locale: String,
      val voiceUnits: GraphHopperVoiceUnits
  ) : RoutingEngine()
}
