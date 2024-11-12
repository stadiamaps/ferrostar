package com.stadiamaps.ferrostar.composeui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

interface FerrostarTheme {
  @get:Composable val instructionRowTheme: InstructionRowTheme
  @get:Composable val roadNameViewTheme: RoadNameViewTheme
  @get:Composable val tripProgressViewTheme: TripProgressViewTheme
  @get:Composable val buttonSize: DpSize
}

object DefaultFerrostarTheme : FerrostarTheme {
  override val instructionRowTheme: InstructionRowTheme
    @Composable get() = DefaultInstructionRowTheme

  override val roadNameViewTheme: RoadNameViewTheme
    @Composable get() = DefaultRoadNameViewTheme

  override val tripProgressViewTheme: TripProgressViewTheme
    @Composable get() = DefaultTripProgressViewTheme

  override val buttonSize: DpSize
    @Composable get() = DpSize(56.dp, 56.dp)
}
