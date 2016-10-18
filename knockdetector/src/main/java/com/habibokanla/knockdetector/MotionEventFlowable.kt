package com.habibokanla.knockdetector

import android.Manifest
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.support.v4.content.ContextCompat
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import io.reactivex.FlowableOnSubscribe

/**
 * 2016
 * Created by habibokanla on 07/08/2016.
 */
class MotionEventFlowable private constructor(context: Context) : FlowableOnSubscribe<Boolean> {

    val mSensorManager: SensorManager

    init {
        mSensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    override fun subscribe(e: FlowableEmitter<Boolean>?) {
        this.emitter = e
        emitter?.setCancellable {
            mSensorManager.unregisterListener(listener)
        }
        mSensorManager.registerListener(listener, mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_UI)
    }

    private var emitter: FlowableEmitter<in Boolean>? = null

    private val thresholdZ = 3f
    private val thresholdX = 5f
    private val thresholdY = 5f


    private var prevZVal = 0f
    private var currentZVal = 0f

    private var prevXVal = 0f
    private var currentXVal = 0f

    private var prevYVal = 0f
    private var currentYVal = 0f

    private val listener = object : SensorEventListener {
        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        }

        override fun onSensorChanged(event: SensorEvent?) {

            prevXVal = currentXVal
            currentXVal = Math.abs(event!!.values[0]) // X-axis
            val diffX = currentXVal - prevXVal

            prevYVal = currentYVal
            currentYVal = Math.abs(event.values[1]) // Y-axis
            val diffY = currentYVal - prevYVal

            prevZVal = currentZVal
            currentZVal = Math.abs(event.values[2]) // Z-axis
            val diffZ = currentZVal - prevZVal

            if (currentZVal > prevZVal && diffZ > thresholdZ && diffX < thresholdX && diffY < thresholdY) {
                emitter?.onNext(true)
            }
        }
    }

    companion object {
        fun create(context: Context): Flowable<Boolean> {
            return Flowable.create(MotionEventFlowable(context), FlowableEmitter.BackpressureMode.LATEST)
        }
    }


}