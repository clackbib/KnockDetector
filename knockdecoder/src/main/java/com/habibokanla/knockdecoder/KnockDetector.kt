package com.habibokanla.knockdecoder

import android.content.Context
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
class KnockDetector(private val context: Context) : FlowableOnSubscribe<Int> {


    private var emitter: FlowableEmitter<Int>? = null
    private var subscription: Disposable? = null
    private var knocks: ArrayList<Boolean> = ArrayList()

    override fun subscribe(e: FlowableEmitter<Int>?) {
        this.emitter = e
        e?.setCancellable { teardown() }

        val mSoundKnockDetector = SoundEventFlowable.create(context)
        val mAccelSpikeEventObservable = MotionEventFlowable.create(context)

        subscription = mAccelSpikeEventObservable
                .join<Boolean, Boolean, Boolean, Boolean>(mSoundKnockDetector, closedWindow(), flowableWindow(), BiFunction { left, right ->
                    left && right
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
    private fun flowableWindow(): Function<in Boolean, out Publisher<Boolean>>? {
        return Function {
            Single.just(true)
                    .delay(DETECTION_WINDOW_MS, TimeUnit.MILLISECONDS)
                    .toFlowable()
        }
    }

    /**
     * @Return a stream representing a closed time window. Since accelerometer spike are more common than sound spikes,
     * they do not open a detection window.
     */
    private fun closedWindow(): Function<in Boolean, out Publisher<Boolean>> {
        return Function {
            Completable.complete().toFlowable()
        }
    }

    private fun teardown() {
        subscription?.dispose()
    }

    companion object {

        val DETECTION_WINDOW_MS = 300L
        val SOUND_DEBOUNCE_DURATION_MS = 200L
        val KNOCK_COUNT_DEBOUNCE_S = 1L

        fun create(context: Context): Flowable<Int> {
            return Flowable.create(KnockDetector(context), FlowableEmitter.BackpressureMode.BUFFER)
        }
    }


}