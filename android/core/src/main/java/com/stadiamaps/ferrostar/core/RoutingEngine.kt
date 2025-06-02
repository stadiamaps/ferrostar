package com.stadiamaps.ferrostar.core

sealed class RoutingEngine {
    data class Valhalla(val endpoint: String, val profile: String) : RoutingEngine()
    data class GraphHopper(val endpoint: String, val profile: String) : RoutingEngine()
}