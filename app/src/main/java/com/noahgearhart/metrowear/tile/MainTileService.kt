package com.noahgearhart.metrowear.tile

import android.content.ComponentName
import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ActionBuilders.launchAction
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.Colors
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.protolayout.material3.ButtonGroupDefaults.DEFAULT_SPACER_BETWEEN_BUTTON_GROUPS
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textButton
import androidx.wear.protolayout.material3.textEdgeButton
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.tooling.preview.Preview
import androidx.wear.tiles.tooling.preview.TilePreviewData
import androidx.wear.tooling.preview.devices.WearDevices
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService
import com.noahgearhart.metrowear.TrainArrival
import com.noahgearhart.metrowear.getMockArrivals
import com.noahgearhart.metrowear.presentation.MainActivity

private const val RESOURCES_VERSION = "0"

/**
 * Skeleton for a tile with no images.
 */
@OptIn(ExperimentalHorologistApi::class)
class MainTileService : SuspendingTileService() {

    override suspend fun resourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ) = resources(requestParams)

    override suspend fun tileRequest(
        requestParams: RequestBuilders.TileRequest
    ) = tile(requestParams, this)
}

private fun resources(
    requestParams: RequestBuilders.ResourcesRequest
): ResourceBuilders.Resources {
    return ResourceBuilders.Resources.Builder()
        .setVersion(RESOURCES_VERSION)
        .build()
}

private fun tile(
    requestParams: RequestBuilders.TileRequest,
    context: Context,
): TileBuilders.Tile {
    val singleTileTimeline = TimelineBuilders.Timeline.Builder()
        .addTimelineEntry(
            TimelineBuilders.TimelineEntry.Builder()
                .setLayout(
                    LayoutElementBuilders.Layout.Builder()
                        .setRoot(tileLayout(context, requestParams.deviceConfiguration, "Clarendon",
                            getMockArrivals()
                        ))
                        .build()
                )
                .build()
        )
        .build()

    return TileBuilders.Tile.Builder()
        .setResourcesVersion(RESOURCES_VERSION)
        .setTileTimeline(singleTileTimeline)
        .build()
}

fun tileLayout(
    context: Context,
    deviceParameters: DeviceParameters,
    stationName: String,
    arrivals: List<TrainArrival>,
): LayoutElement {
    val stationNameConverted = if (stationName.length > 10) {
        stationName.substring(0, 10) + "..."
    } else {
        "$stationName Arrivals"
    }

    val action = clickable(
        id = "more-0",
        action =
            launchAction(
                ComponentName(
                    "com.noahgearhart.metrowear",
                    "com.noahgearhart.metrowear.presentation.MainActivity",
                ),
                mapOf(
                    MainActivity.EXTRA_JOURNEY to
                            ActionBuilders.stringExtra(MainActivity.EXTRA_JOURNEY_CONVERSATION)
                ),
            ),
    )

    return materialScope(
        context = context,
        deviceConfiguration = deviceParameters,
        allowDynamicTheme = true,
    ) {
        primaryLayout(
            titleSlot = { text(text = stationNameConverted.layoutString) },
            mainSlot = {
                column {
                    setWidth(expand())
                    setHeight(expand())
                    if (arrivals.isNotEmpty()) {
                        addContent(
                            textButton(
                                labelContent = {
                                    text(text = (arrivals[0].shortDestination + " - " + arrivals[0].arrivalMessage).layoutString)
                                },
                                onClick = action,
                                width = expand(),
                                colors = arrivals[0].line.buttonColor
                            )
                        )
                    }
                    if (arrivals.size > 1) {
                        addContent(DEFAULT_SPACER_BETWEEN_BUTTON_GROUPS)
                        addContent(
                            textButton(
                                labelContent = {
                                    text(text = (arrivals[1].shortDestination + " - " + arrivals[1].arrivalMessage).layoutString)
                                },
                                onClick = action,
                                width = expand(),
                                colors = arrivals[0].line.buttonColor
                            )
                        )
                    }
                }
            },
            bottomSlot = {
                textEdgeButton(
                    onClick = action,
                    labelContent = { text("More".layoutString) }
                )
            }
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND)
@Preview(device = WearDevices.LARGE_ROUND)
fun tilePreview(context: Context) = TilePreviewData(::resources) {
    tile(it, context)
}

fun column(builder: Column.Builder.() -> Unit): Column = Column.Builder().apply(builder).build()
