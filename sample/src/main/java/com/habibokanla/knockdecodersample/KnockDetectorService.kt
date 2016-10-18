package com.habibokanla.knockdecodersample

import android.app.IntentService
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Vibrator
import android.support.v4.app.NotificationCompat
import android.widget.Toast
import com.habibokanla.knockdecoder.KnockDetector
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*

/**
 * An [IntentService] subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 *
 *
 * helper methods.
 */
class KnockDetectorService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val action = intent.action
            if (ACTION_START == action) {
                startKnockDetection()
            } else if (ACTION_STOP == action) {
                stopSelf()
                stopKnockDetection()
            }
        } else {
            stopSelf()
        }
        return Service.START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        super.onDestroy()
        stopKnockDetection()
    }

    private fun stopKnockDetection() {
        subscription?.safeDispose()
        isRunning = false
    }


    private var subscription: Disposable? = null
    private var vibrator: Vibrator? = null

    private fun startKnockDetection() {
        showNotification()
        vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        subscription = KnockDetector.create(this)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    val pattern = ArrayList<Long>()
                    for (i in 1..it) {
                        pattern.add(200)
                        pattern.add(200)
                    }
                    vibrator?.vibrate(pattern.toLongArray(), -1)
                }, {
                    Toast.makeText(this, "An error occurred during while trying to start knock detection", Toast.LENGTH_LONG).show()
                    stopSelf()
                })
    }

    private fun showNotification() {
        val notificationIntent = Intent(this, KnockDetectorService::class.java)
        notificationIntent.action = ACTION_STOP
        val pendingIntent = PendingIntent.getService(this, 0, notificationIntent, 0)
        val notification = NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText("Knock Detection is Running!")
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "STOP", pendingIntent)
                .build()

        startForeground(ONGOING_NOTIFICATION_ID, notification)
        isRunning = true
    }

    override fun onBind(intent: Intent): IBinder? {
        throw IllegalStateException("This is not a bound service")
    }

    companion object {
        private val ACTION_START = "com.habibokanla.knockdecodersample.action.START"
        private val ACTION_STOP = "com.habibokanla.knockdecodersample.action.STOP"
        private val ONGOING_NOTIFICATION_ID = 110
        var isRunning = false


        fun start(context: Context) {
            val intent = Intent(context, KnockDetectorService::class.java)
            intent.action = ACTION_START
            context.startService(intent)
        }
    }

}
