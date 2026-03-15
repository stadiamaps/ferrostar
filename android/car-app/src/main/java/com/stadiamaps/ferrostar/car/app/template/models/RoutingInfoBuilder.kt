package com.stadiamaps.ferrostar.car.app.template.models

import android.os.Build
import androidx.car.app.CarContext
import androidx.car.app.navigation.model.RoutingInfo
import com.stadiamaps.ferrostar.core.extensions.currentStep
import com.stadiamaps.ferrostar.core.extensions.progress
import com.stadiamaps.ferrostar.core.extensions.visualInstruction
import uniffi.ferrostar.DrivingSide
import uniffi.ferrostar.TripState

class FerrostarRoutingInfo {
  class Builder(private val context: CarContext) {
    private var tripState: TripState? = null
    private var backupDrivingSide: DrivingSide = DrivingSide.RIGHT

    fun setTripState(tripState: TripState): Builder {
      this.tripState = tripState
      return this
    }

    fun setBackupDrivingSide(drivingSide: DrivingSide): Builder {
      this.backupDrivingSide = drivingSide
      return this
    }

    fun build(): RoutingInfo? {
      val instruction = tripState?.visualInstruction() ?: return null
      val progress = tripState?.progress() ?: return null
      val currentStep = tripState?.currentStep() ?: return null

      val drivingSide = currentStep.drivingSide ?: backupDrivingSide
      val roundaboutExitNumber = currentStep.roundaboutExitNumber?.toInt()

      return RoutingInfo.Builder()
          .setCurrentStep(
              instruction.toCarStep(context, drivingSide, roundaboutExitNumber),
              progress.toCarDistanceToNextManeuver()
          )
          .build()
    }
  }
}


