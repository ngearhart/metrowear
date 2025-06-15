/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.noahgearhart.metrowear.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.ListHeader
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.noahgearhart.metrowear.GeoJson
import com.noahgearhart.metrowear.R
import com.noahgearhart.metrowear.Station
import com.noahgearhart.metrowear.callApi
import com.noahgearhart.metrowear.getClosestStationToCoordinates
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json


class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var currentStatus = listOf<TrainArrival>();
    private var closestStation: Station? = null;
    private var requestCode = 93479024;
    private var stationData: GeoJson? = null;
    val scope = CoroutineScope(Job() + Dispatchers.Main)

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
//        installSplashScreen()

        Log.i("MetroWear", "onCreate")
        super.onCreate(savedInstanceState)


        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                Log.i("MetroWear", "Got locations")
                for (location in p0.locations) {
                    Log.i("MetroWear", "Lat=" + location.latitude + ",long=" + location.longitude)
                }
                if (stationData != null) {
                    closestStation = getClosestStationToCoordinates(stationData!!, p0.locations.last().latitude, p0.locations.last().longitude)
                    Log.i("MetroWear", "Closest Station = " + closestStation!!.name);
                    oneTimeTrainApiUpdate()
                }
            }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setTheme(android.R.style.Theme_DeviceDefault)

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            Log.e("MetroWear", "No location permission")
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                requestCode
            )
        } else {
            Log.i("MetroWear", "Location permission allowed")
            startLocationUpdates()
        }

        updateUI()
        scope.launch {
            withContext(Dispatchers.IO) {
                if (stationData == null) {
                    resources.openRawResource(R.raw.stations)
                        .bufferedReader().use {
                            val jsonDeserializer = Json {
                                isLenient = true
                                ignoreUnknownKeys = true
                            }
                            stationData = jsonDeserializer.decodeFromString<GeoJson>(it.readText())
                        }
                }
            }
        }
    }

    fun oneTimeTrainApiUpdate() {
        scope.launch {
            Log.i("MetroWear", "Running one time train API update")
            withContext(Dispatchers.IO) {
                if (closestStation != null) {
                    currentStatus = callApi(closestStation!!)
                }
                updateUI()
            }
        }
    }

    fun updateUI() {
        setContent {
            ComposeList(currentStatus, closestStation?.name ?: "Loading")
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            startLocationUpdates();
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
        scope.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        internal const val EXTRA_JOURNEY = "journey"
        internal const val EXTRA_JOURNEY_CONVERSATION = "journey:conversation"
        internal const val EXTRA_JOURNEY_NEW = "journey:new"
        internal const val EXTRA_CONVERSATION_CONTACT = "conversation:contact"
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationUpdates() {
        Log.i("MetroWear", "Starting location updates")
        fusedLocationClient.requestLocationUpdates(
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 15000).build(),
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        Log.i("MetroWear", "Stopping location updates")
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    ComposeList(getMockArrivals(), "duh")
}

@OptIn(ExperimentalTime::class)
@Composable
fun ComposeList(arrivals: List<TrainArrival>, location: String) {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    Text(text = location)
                }
            }
            // ... other items
            arrivals.sortedBy { it.arrivalTimestamp }.map {
                item {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = it.line.androidColor,
                            contentColor = it.line.textColor,
                            iconColor = it.line.textColor
                        ),
                        onClick = { /* ... */ },
//                        icon = {
//                            Icon(
//                                imageVector = Icons.Default.Build,
//                                contentDescription = "Train",
//                            )
//                        },
                    ) {
                        Text(
                            text = it.shortDestination,
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
