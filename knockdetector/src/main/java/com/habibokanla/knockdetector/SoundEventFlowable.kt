package com.habibokanla.knockdetector

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.support.v4.content.ContextCompat
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import io.reactivex.FlowableOnSubscribe
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit

/**
 * 2016
 * Created by habibokanla on 07/08/2016.
 */
class SoundEventFlowable(val spikeThreshold: Int) : FlowableOnSubscribe<Int> {
    override fun subscribe(e: FlowableEmitter<Int>?) {
        this.emitter = e
        this.emitter?.setCancellable {
            teardown()
        }
        setup()
    }

    private var mediaRecorder: MediaRecorder? = null
    private var internalSubscription: Disposable? = null
    private var emitter: FlowableEmitter<in Int>? = null
    private var isStarted = false

    fun setup() {
        prepareRecorder()
        internalSubscription = Flowable.interval(0, 100, TimeUnit.MILLISECONDS).subscribe({
            val amplitude = mediaRecorder?.maxAmplitude ?: 0
            if (amplitude > spikeThreshold) {
                if (!(emitter?.isCancelled ?: true)) {
                    emitter?.onNext(amplitude)
                }
            }
        })
    }

    fun prepareRecorder() {
        if (!isStarted) {
            if (mediaRecorder == null) {
                mediaRecorder = MediaRecorder()
            }
            mediaRecorder?.let {
                it.setAudioSource(MediaRecorder.AudioSource.MIC)
                it.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                it.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                it.setOutputFile(DISCARD_OUTPUT)

                try {
                    it.prepare()
                    it.start()
                    isStarted = true
                } catch (e: Exception) {
                    this.emitter?.onError(e)
                }
            }


        }
    }

    fun teardown() {
        internalSubscription?.dispose()
        if (isStarted) {
            mediaRecorder?.stop()
            isStarted = false
        }
        mediaRecorder?.release()
        mediaRecorder = null
    }


    companion object {
        val DISCARD_OUTPUT = "/dev/null"

        fun create(context: Context, threshold: Int): Flowable<Int> {
            val hasMicPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (hasMicPermission) {
                return Flowable.create(SoundEventFlowable(threshold), FlowableEmitter.BackpressureMode.LATEST)
            } else {
                val message = "Missing permissions for Audio recording."
                return Flowable.error(RuntimeException(message))
            }
        }

        //TODO:Finish calibration logic.
//        fun calibrate(context: Context): Single<Int> {
//            return create(context, 8000).take(3).buffer(3).map { it.average().toInt() }.firstOrError()
//        }
    }


}