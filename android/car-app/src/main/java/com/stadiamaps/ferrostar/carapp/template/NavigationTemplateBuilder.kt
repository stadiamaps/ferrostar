package com.stadiamaps.ferrostar.carapp.template

import androidx.car.app.CarContext
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import com.stadiamaps.ferrostar.carapp.R
import com.stadiamaps.ferrostar.carapp.template.icons.InterfaceCarIcons
import com.stadiamaps.ferrostar.carapp.template.models.FerrostarRoutingInfo
import com.stadiamaps.ferrostar.carapp.template.models.toCarTravelEstimate
import com.stadiamaps.ferrostar.core.extensions.progress
import uniffi.ferrostar.DrivingSide
import uniffi.ferrostar.TripState

class NavigationTemplateBuilder(
    private val carContext: CarContext
) {
    private val carIcons = InterfaceCarIcons(carContext)
    private var tripState: TripState? = null
    private var backupDrivingSide: DrivingSide = DrivingSide.RIGHT

    private var onStopTapped: (() -> Unit)? = null

    private var isMuted: Boolean = false
    private var onMuteTapped: (() -> Unit)? = null

    private var onZoomInTapped: (() -> Unit)? = null

    private var onZoomOutTapped: (() -> Unit)? = null

    private var cameraIsCenteredOnUser: Boolean = true
    private var onCycleCameraTapped: (() -> Unit)? = null

    fun setTripState(tripState: TripState?): NavigationTemplateBuilder {
        this.tripState = tripState
        return this
    }

    fun setBackupDrivingSide(drivingSide: DrivingSide): NavigationTemplateBuilder {
        this.backupDrivingSide = drivingSide
        return this
    }

    fun setOnStopNavigation(onStopTapped: () -> Unit): NavigationTemplateBuilder {
        this.onStopTapped = onStopTapped
        return this
    }

    fun setOnMute(
        isMuted: Boolean?,
        onMuteTapped: () -> Unit
    ): NavigationTemplateBuilder {
        this.isMuted = isMuted ?: false
        this.onMuteTapped = onMuteTapped
        return this
    }

    fun setOnZoom(
        onZoomInTapped: () -> Unit,
        onZoomOutTapped: () -> Unit
    ): NavigationTemplateBuilder {
        this.onZoomInTapped = onZoomInTapped
        this.onZoomOutTapped = onZoomOutTapped
        return this
    }

    fun setOnCycleCamera(
        cameraIsCenteredOnUser: Boolean?,
        onCycleCameraTapped: () -> Unit
    ): NavigationTemplateBuilder {
        this.cameraIsCenteredOnUser = cameraIsCenteredOnUser ?: true
        this.onCycleCameraTapped = onCycleCameraTapped
        return this
    }

    fun build(): Template =
        NavigationTemplate.Builder()
            .setActionStrip(buildActionStrip())
            .setMapActionStrip(buildMapActionStrip())
            .apply {
                tripState?.let { state ->
                  val info = FerrostarRoutingInfo.Builder(carContext)
                      .setTripState(state)
                      .build()
                  setNavigationInfo(info)

                  state.progress()?.let {
                    setDestinationTravelEstimate(it.toCarTravelEstimate())
                  }
                }
            }
            .build()

    private fun buildActionStrip(): ActionStrip {
        return ActionStrip.Builder()
            .apply {
                onStopTapped?.let {
                    addAction(
                        Action.Builder()
                            .setTitle(carContext.getString(R.string.stop_nav))
                            .setOnClickListener(it)
                            .build()
                    )
                }
            }
            .build()
    }

    private fun buildMapActionStrip(): ActionStrip =
        ActionStrip.Builder()
            .apply {
                onMuteTapped?.let {
                    addAction(
                        Action.Builder()
                            .setIcon(carIcons.mute(isMuted))
                            .setOnClickListener(it)
                            .build()
                    )
                }
                onZoomInTapped?.let {
                    addAction(
                        Action.Builder()
                            .setIcon(carIcons.add)
                            .setOnClickListener(it)
                            .build()
                    )
                }
                onZoomOutTapped?.let {
                    addAction(
                        Action.Builder()
                            .setIcon(carIcons.remove)
                            .setOnClickListener(it)
                            .build()
                    )
                }
                onCycleCameraTapped?.let {
                    addAction(
                        Action.Builder()
                            .setIcon(carIcons.camera(cameraIsCenteredOnUser))
                            .setOnClickListener(it)
                            .build()
                    )
                }
            }
            .build()
}

