package com.stadiamaps.ferrostar.auto

import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate

/** Builds a [NavigationTemplate] for the idle (not navigating) state with pan support. */
fun buildDemoMapTemplate(): Template =
    NavigationTemplate.Builder()
        .setActionStrip(ActionStrip.Builder().addAction(Action.PAN).build())
        .build()
