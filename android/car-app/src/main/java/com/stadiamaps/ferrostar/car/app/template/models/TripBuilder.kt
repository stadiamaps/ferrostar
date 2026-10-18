package com.stadiamaps.ferrostar.car.app.template.models

import android.content.Context
import androidx.car.app.navigation.model.Destination
import androidx.car.app.navigation.model.Trip
import com.stadiamaps.ferrostar.core.extensions.currentRoadName
import com.stadiamaps.ferrostar.core.extensions.currentStep
import com.stadiamaps.ferrostar.core.extensions.progress
import com.stadiamaps.ferrostar.core.extensions.visualInstruction
import uniffi.ferrostar.DrivingSide
import uniffi.ferrostar.TripState

class FerrostarTrip {
  class Builder(private val context: Context) {

    private var tripState: TripState? = null
    private var destination: Destination? = null
    private var backupDrivingSide: DrivingSide = DrivingSide.RIGHT

    fun setTripState(tripState: TripState): Builder {
      this.tripState = tripState
      return this
    }

    fun setDestination(destination: String): Builder {
      this.destination = Destination.Builder().setName(destination).build()

      return this
    }

    fun setBackupDrivingSide(drivingSide: DrivingSide): Builder {
      this.backupDrivingSide = drivingSide
      return this
    }

    fun build(): Trip {
      val instruction = tripState?.visualInstruction()
      val progress = tripState?.progress()
      val currentStep = tripState?.currentStep()

      return Trip.Builder()
          .apply {
            if (instruction != null && progress != null && currentStep != null) {
              val drivingSide = currentStep.drivingSide ?: backupDrivingSide
              val roundaboutExitNumber = currentStep.roundaboutExitNumber?.toInt()

              val step = instruction.toCarStep(context, drivingSide, roundaboutExitNumber)

              // The Car App Library shows the first step estimate on the instrument cluster,
              // so the step gets the distance and time to the next maneuver.
              // The destination keeps the whole-trip estimate.
              val stepEstimate = progress.toCarStepTravelEstimate(currentStep)
              val tripEstimate = progress.toCarTravelEstimate()

              addStep(step, stepEstimate)

              destination?.let { addDestination(it, tripEstimate) }
            }
          }
          .apply { tripState?.currentRoadName()?.let { setCurrentRoad(it) } }
          .build()
    }
  }
}
