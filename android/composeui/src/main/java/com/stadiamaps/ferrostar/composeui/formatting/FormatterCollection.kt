package com.stadiamaps.ferrostar.composeui.formatting

interface FormatterCollection {

  /** The formatter for the distance to the next step. */
  val distanceFormatter: DistanceFormatter

  /** The formatter for the estimated arrival date and time. */
  val estimatedArrivalFormatter: DateTimeFormatter

  /** The formatter for the remaining duration. */
  val durationFormatter: DurationFormatter
}

/** TODO: add description and consider naming for android. */
data class StandardFormatterCollection(
    override val distanceFormatter: DistanceFormatter = LocalizedDistanceFormatter(),
    override val estimatedArrivalFormatter: DateTimeFormatter = EstimatedArrivalDateTimeFormatter(),
    override val durationFormatter: DurationFormatter = LocalizedDurationFormatter()
) : FormatterCollection
