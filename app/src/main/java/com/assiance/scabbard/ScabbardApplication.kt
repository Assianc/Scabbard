package com.assiance.scabbard

import android.app.Activity
import android.app.Application
import android.os.Bundle

class ScabbardApplication : Application() {
    companion object {
        val allActivities = mutableListOf<Activity>()
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                allActivities.add(activity)
            }

            override fun onActivityDestroyed(activity: Activity) {
                allActivities.remove(activity)
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        })
    }
} 