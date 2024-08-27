package com.ferrostar.carui

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.connection.CarConnection
import androidx.lifecycle.LifecycleOwner
import java.lang.ref.WeakReference

class FerrostarCarAppManager(
  context: Context
): ServiceConnection {

  companion object {
    private const val TAG = "FerrostarCarAppManager"
  }

  val weakContext: WeakReference<Context> = WeakReference(context)
  val context: Context
    get() = weakContext.get() ?: throw IllegalStateException("Context is null")

  fun start(lifecycleOwner: LifecycleOwner) {
    CarConnection(context).type.observe(lifecycleOwner) { connection ->
      Log.d(TAG, "CarConnection type: $connection")
    }

//    val intent = Intent(carContext, FerrostarCarAppService::class.java)
//    context.start
  }

  override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
    TODO("Not yet implemented")
  }

  override fun onServiceDisconnected(name: ComponentName?) {
    TODO("Not yet implemented")
  }
}