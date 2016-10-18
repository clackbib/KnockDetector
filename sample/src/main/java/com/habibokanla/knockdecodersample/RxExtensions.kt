package com.habibokanla.knockdecodersample

import io.reactivex.disposables.Disposable

/**
 * 2016
 * Created by habibokanla on 17/10/2016.
 */
fun Disposable.safeDispose() {
    if (!isDisposed) {
        dispose()
    }
}