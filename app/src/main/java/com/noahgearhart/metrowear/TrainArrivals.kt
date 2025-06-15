package com.noahgearhart.metrowear

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.toColor
import androidx.wear.protolayout.material3.ButtonColors
import androidx.wear.protolayout.types.LayoutColor
import androidx.wear.protolayout.types.argb
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.request.headers
import io.ktor.serialization.kotlinx.json.json

import java.util.Date
import java.util.GregorianCalendar
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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
            return if (destination.length > 13) {
                destination.substring(0, 11) + "...";
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

    companion object {
        fun fromCode(code: String): Line {
            var result = Line.entries.find { it.apiName == code }
            if (result != null) {
                return result
            }
            return Line.UNKNOWN
        }
    }
}

fun getMockArrivals() = listOf(
    TrainArrival(Line.ORANGE, "Vienna", "ARR"),
    TrainArrival(Line.ORANGE, "New Carrolton", "1"),
    TrainArrival(Line.SILVER, "Ashburn", "2"),
    TrainArrival(Line.SILVER, "Largo", "12"),
    TrainArrival(Line.ORANGE, "Vienna", "13"),
)

@Serializable
data class TrainPredictions(
    @SerialName("Trains")
    val trains: List<Train>
) {

    @Serializable
    data class Train(
        @SerialName("Car")
        val car: String? = null,
        @SerialName("Destination")
        val destination: String? = null,
        @SerialName("DestinationCode")
        val destinationCode: String? = null,
        @SerialName("DestinationName")
        val destinationName: String? = null,
        @SerialName("Group")
        val group: String? = null,
        @SerialName("Line")
        val line: String? = null,
        @SerialName("LocationCode")
        val locationCode: String? = null,
        @SerialName("LocationName")
        val locationName: String? = null,
        @SerialName("Min")
        val min: String? = null,
    )
}

suspend fun callApi(): List<TrainArrival> {
    val apiKey = BuildConfig.wmataApiKey;
    Log.i("MetroWear", apiKey)
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }
    val response: TrainPredictions = client.get("https://api.wmata.com/StationPrediction.svc/json/GetPrediction/C01") {
        headers {
            append("api_key", apiKey)
        }
    }.body()
    client.close();
    return response.trains.map {
        TrainArrival(
            line = Line.fromCode(it.line ?: ""),
            destination = it.destinationName ?: "",
            arrivalMessage = it.min ?: ""
        )
    }.sortedBy { it.arrivalTimestamp }
}