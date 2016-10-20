package com.habibokanla.knockdetector

import android.content.Context
import com.habibokanla.knockdetector.SoundEventFlowable
import com.habibokanla.knockdetector.MotionEventFlowable
import io.reactivex.*
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function
import org.reactivestreams.Publisher
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 2016
 * Created by habibokanla on 07/08/2016.
 */
class KnockDetector(private val context: Context, val soundThreshold: Int) : FlowableOnSubscribe<Int> {


    private var emitter: FlowableEmitter<Int>? = null
    private var subscription: Disposable? = null
    private var knocks: ArrayList<Boolean> = ArrayList()

    override fun subscribe(e: FlowableEmitter<Int>?) {
        this.emitter = e
        e?.setCancellable { teardown() }

        val mSoundKnockDetector = SoundEventFlowable.create(context, soundThreshold)
        val mAccelSpikeEventObservable = MotionEventFlowable.create(context)

        subscription = mAccelSpikeEventObservable
                .join<Int, Boolean, Boolean, Boolean>(mSoundKnockDetector, accelWindow(), soundWindow(), BiFunction { left, right ->
                    true
                })
                .debounce(SOUND_DEBOUNCE_DURATION_MS, TimeUnit.MILLISECONDS)
                .doOnNext { knocks.add(it) }
                .debounce(KNOCK_COUNT_DEBOUNCE_S, TimeUnit.SECONDS)
                .map {
                    val count = knocks.size
                    knocks.clear()
                    count
                }
                .filter { it > 1 }
                .subscribe(
                        { this.emitter?.onNext(it) },
                        { this.emitter?.onError(it) })
    }


    /**
     * @Return an Stream representing a window of time during which a sound spike will be considered has a knock,
     * when an accelerometer spike has already been registered
     */
    private fun soundWindow(): Function<in Int, out Publisher<Boolean>>? {
        return Function {
            Single.just(true)
                    .delay(DETECTION_WINDOW_MS, TimeUnit.MILLISECONDS)
                    .toFlowable()
        }
    }

    /**
     * @Return an Stream representing a window of time during which an accelerometer spike will be considered has a knock,
     * when an sound spike has already been registered
     */
    private fun accelWindow(): Function<in Boolean, out Publisher<Boolean>> {
        return Function {
            Single.just(true)
                    .delay(DETECTION_WINDOW_MS, TimeUnit.MILLISECONDS)
                    .toFlowable()
        }
    }

    private fun teardown() {
        subscription?.dispose()
    }

    companion object {

        val DETECTION_WINDOW_MS = 300L
        val SOUND_DEBOUNCE_DURATION_MS = 200L
        val KNOCK_COUNT_DEBOUNCE_S = 1L

        val DEFAULT_SOUND_THRESHOLD = 10000

        fun create(context: Context, soundThreshold: Int = DEFAULT_SOUND_THRESHOLD): Flowable<Int> {
            return Flowable.create(KnockDetector(context, soundThreshold), FlowableEmitter.BackpressureMode.BUFFER)
        }
    }


}