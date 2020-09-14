package com.haxotech.glinic_manager;

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import io.flutter.plugin.common.EventChannel

class SensorListener() :
        EventChannel.StreamHandler, SensorEventListener {
    private var eventSink: EventChannel.EventSink? = null

    // EventChannel.StreamHandler methods
    override fun onListen(
            arguments: Any?, eventSink: EventChannel.EventSink?) {
        this.eventSink = eventSink
        registerIfActive()
    }
    override fun onCancel(arguments: Any?) {
        unregisterIfActive()
        eventSink = null
    }

    // SensorEventListener methods.
    override fun onSensorChanged(event: SensorEvent) {
        eventSink?.success(event.values)
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW)
            eventSink?.error("SENSOR", "Low accuracy detected", null)
    }
    // Lifecycle methods.
    fun registerIfActive() {
        if (eventSink == null) return
        "Hello"
    }
    fun unregisterIfActive() {
        if (eventSink == null) return
        "unregister"
    }
}