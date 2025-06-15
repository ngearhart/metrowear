/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.noahgearhart.metrowear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.ListHeader
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.tooling.preview.devices.WearDevices
import com.noahgearhart.metrowear.TrainArrival
import com.noahgearhart.metrowear.getMockArrivals
import kotlin.time.ExperimentalTime
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ScreenScaffold
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import com.google.android.horologist.compose.layout.ColumnItemType
import androidx.wear.compose.material3.lazy.transformedHeight

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            ComposeList(getMockArrivals())
        }
    }


    companion object {
        internal const val EXTRA_JOURNEY = "journey"
        internal const val EXTRA_JOURNEY_CONVERSATION = "journey:conversation"
        internal const val EXTRA_JOURNEY_NEW = "journey:new"
        internal const val EXTRA_CONVERSATION_CONTACT = "conversation:contact"
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    ComposeList(getMockArrivals())
}

@OptIn(ExperimentalTime::class)
@Composable
fun ComposeList(arrivals: List<TrainArrival>) {
    // [START android_wear_list]
    val columnState = rememberTransformingLazyColumnState()
    val contentPadding = rememberResponsiveColumnPadding(
        first = ColumnItemType.ListHeader,
        last = ColumnItemType.Button,
    )
    val transformationSpec = rememberTransformationSpec()
    ScreenScaffold(
        scrollState = columnState,
        contentPadding = contentPadding
    ) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = contentPadding
        ) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    Text(text = "Clarendon Arrivals")
                }
            }
            // ... other items
            arrivals.sortedBy { it.arrivalTimestamp }.map {
                item {
                    Button(
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                        colors = ButtonDefaults.buttonColors(containerColor = it.line.androidColor, contentColor = it.line.textColor, iconColor = it.line.textColor),
                        onClick = { /* ... */ },
//                        icon = {
//                            Icon(
//                                imageVector = Icons.Default.Build,
//                                contentDescription = "Train",
//                            )
//                        },
                    ) {
                        Text(
                            text = it.destination,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = it.arrivalMessage,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
    // [END android_wear_list]
}
