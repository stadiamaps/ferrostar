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
      this.destination = Destination.Builder()
          .setName(destination)
          .build()

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
              val estimate = progress.toCarTravelEstimate()

              addStep(step, estimate)

              destination?.let {
                addDestination(it, estimate)
              }
            }
          }
          .apply {
            tripState?.currentRoadName()?.let {
              setCurrentRoad(it)
            }
          }
          .build()
    }
  }
}
