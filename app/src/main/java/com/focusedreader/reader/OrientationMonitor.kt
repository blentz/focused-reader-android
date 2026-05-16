package com.focusedreader.reader

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

enum class FaceOrientation { UP, DOWN, UNKNOWN }

@Singleton
class OrientationMonitor @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    fun orientationEvents(debounceMs: Long = 1500): Flow<FaceOrientation> = callbackFlow {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_GRAVITY)
        if (sm == null || sensor == null) {
            trySend(FaceOrientation.UNKNOWN)
            close()
            return@callbackFlow
        }

        var lastEmit = 0L
        val confirmer = OrientationConfirmer()

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val z = event.values.getOrNull(2) ?: return
                val o = when {
                    z > 7f -> FaceOrientation.UP
                    z < -7f -> FaceOrientation.DOWN
                    else -> return
                }
                val confirmed = confirmer.observe(o) ?: return
                val now = System.currentTimeMillis()
                if (now - lastEmit < debounceMs) return
                lastEmit = now
                trySend(confirmed)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { sm.unregisterListener(listener) }
    }.distinctUntilChanged()
}
