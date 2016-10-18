package com.habibokanla.knockdetector

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.support.v4.content.ContextCompat
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import io.reactivex.FlowableOnSubscribe
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit

/**
 * 2016
 * Created by habibokanla on 07/08/2016.
 */
class SoundEventFlowable : FlowableOnSubscribe<Boolean> {
    override fun subscribe(e: FlowableEmitter<Boolean>?) {
        this.emitter = e
        this.emitter?.setCancellable {
            teardown()
        }
        setup()
    }

    private constructor()

    private var mediaRecorder: MediaRecorder? = null
    private val spikeThreshold: Int = 17000
    private var internalSubscription: Disposable? = null
    private var emitter: FlowableEmitter<in Boolean>? = null
    private var isStarted = false

    fun setup() {
        prepareRecorder()
        internalSubscription = Flowable.interval(0, 100, TimeUnit.MILLISECONDS).subscribe({
            if (mediaRecorder?.maxAmplitude ?: 0 > spikeThreshold) {
                if (!(emitter?.isCancelled ?: true)) {
                    emitter?.onNext(true)
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

        fun create(context: Context): Flowable<Boolean> {
            val hasMicPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            val hasStoragePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            if (hasMicPermission && hasStoragePermission) {
                return Flowable.create(SoundEventFlowable(), FlowableEmitter.BackpressureMode.LATEST)
            } else {
                var message = ""
                if (!hasMicPermission) {
                    message = "Missing permissions for Audio recording."
                }
                if (!hasStoragePermission) {
                    message += "Missing permission for Storage. "
                }
                return Flowable.error(RuntimeException(message))
            }

        }
    }


}