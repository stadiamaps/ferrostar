package com.stadiamaps.ferrostar.carapp.template

import androidx.car.app.CarContext
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MapController
import androidx.car.app.navigation.model.NavigationTemplate
import com.stadiamaps.ferrostar.carapp.R
import com.stadiamaps.ferrostar.carapp.template.icons.CarIcons
import com.stadiamaps.ferrostar.carapp.template.models.buildNavigationInfo
import com.stadiamaps.ferrostar.carapp.template.models.toCarTravelEstimate
import uniffi.ferrostar.DrivingSide
import uniffi.ferrostar.TripProgress
import uniffi.ferrostar.VisualInstruction

class NavigationTemplateBuilder(
    private val carContext: CarContext
) {
    private var drivingSide: DrivingSide = DrivingSide.RIGHT
    private var visualInstruction: VisualInstruction? = null
    private var tripProgress: TripProgress? = null

    private var onStopTapped: (() -> Unit)? = null

    private var isMuted: Boolean = false
    private var onMuteTapped: (() -> Unit)? = null

    private var onZoomInTapped: (() -> Unit)? = null

    private var onZoomOutTapped: (() -> Unit)? = null

    private var cameraIsCenteredOnUser: Boolean = true
    private var onCycleCameraTapped: (() -> Unit)? = null

    fun setDrivingSite(drivingSide: DrivingSide): NavigationTemplateBuilder {
        this.drivingSide = drivingSide
        return this
    }

    fun setVisualInstruction(visualInstruction: VisualInstruction?): NavigationTemplateBuilder {
        this.visualInstruction = visualInstruction
        return this
    }

    fun setTripProgress(tripProgress: TripProgress?): NavigationTemplateBuilder {
        this.tripProgress = tripProgress
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
                visualInstruction?.let { instruction ->
                    setNavigationInfo(
                        buildNavigationInfo(
                            instruction,
                            progress = tripProgress,
                            context = carContext,
                            drivingSide = drivingSide,
                        )
                    )
                }
                tripProgress?.let {
                    setDestinationTravelEstimate(it.toCarTravelEstimate())
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

    fun buildMapController(): MapController {
        return MapController.Builder()
            .setMapActionStrip(buildMapActionStrip())
            .build()
    }

    private val carIcons = CarIcons(carContext)

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

    companion object {
        const val TAG = "NavScreenTemplateBuilder"
    }
}

