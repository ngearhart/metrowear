package com.noahgearhart.metrowear

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.toColor
import androidx.wear.protolayout.material3.ButtonColors
import androidx.wear.protolayout.types.LayoutColor
import androidx.wear.protolayout.types.argb

import java.util.Date
import java.util.GregorianCalendar
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes

data class TrainArrival(
    val line: Line,
    val destination: String,
    val arrivalMessage: String,
) {
    val arrivalTimestamp: Instant
        get() {
            val arrivalMinutesParsed = arrivalMessage.toIntOrNull();
            val clock: Clock = Clock.System
            if (arrivalMinutesParsed != null) {
                return clock.now() + arrivalMinutesParsed.minutes
            }
            if (arrivalMessage == "ARR") {
                return clock.now() - 1.minutes
            }
            if (arrivalMessage == "BRD") {
                return clock.now() - 2.minutes
            }
            return clock.now()
        }

    val shortDestination: String
        get() {
            return if (destination.length > 8) {
                destination.substring(0, 6) + "...";
            } else {
                destination
            }
        }
}

// Color is in the form A R G B
enum class Line(val color: Long, val lightText: Boolean, val apiName: String) {
    UNKNOWN(0xFF505050, true, "UNKNOWN"),
    RED(0xFFFF0000, true, "RD"),
    ORANGE(0xFFFF5500, true, "OR"),
    SILVER(0xFFAAAAAA, false, "SV"),
    YELLOW(0xFFFFFF00, false, "YL"),
    GREEN(0xFF00FF00, false, "GR"),
    BLUE(0xFF0000FF, true, "BL");

    val androidColor: Color
        get() = Color(this.color)

    val textColor: Color
        get() = when(this.lightText) {
            true -> Color(0xFFFFFFFF)
            false -> Color(0xFF000000)
        }

    val buttonColor: ButtonColors
        get() = ButtonColors(
            labelColor = LayoutColor(staticArgb = textColor.toArgb()),
            containerColor = this.color.argb
        )
}

fun getMockArrivals() = listOf(
    TrainArrival(Line.ORANGE, "Vienna", "ARR"),
    TrainArrival(Line.ORANGE, "New Carrolton", "1"),
    TrainArrival(Line.SILVER, "Ashburn", "2"),
    TrainArrival(Line.SILVER, "Largo", "12"),
    TrainArrival(Line.ORANGE, "Vienna", "13"),
)