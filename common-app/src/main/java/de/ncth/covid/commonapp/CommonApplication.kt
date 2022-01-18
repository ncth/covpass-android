/*
 * (C) Copyright IBM Deutschland GmbH 2021
 * (C) Copyright IBM Corp. 2021
 */

package de.ncth.covid.commonapp

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.WindowManager
import android.webkit.WebView
import androidx.fragment.app.FragmentActivity
import androidx.work.*
import com.ensody.reactivestate.DependencyAccessor
import com.ibm.health.common.android.utils.AndroidDependencies
import com.ibm.health.common.android.utils.androidDeps
import com.ibm.health.common.android.utils.isDebuggable
import com.ibm.health.common.navigation.android.*
import com.ibm.health.common.securityprovider.initSecurityProvider
import com.instacart.library.truetime.TrueTime
import de.ncth.covid.commonapp.dependencies.commonDeps
import de.ncth.covid.commonapp.truetime.CustomCache
import de.ncth.covid.commonapp.utils.schedulePeriodicWorker
import de.ncth.covid.http.HttpLogLevel
import de.ncth.covid.http.httpConfig
import de.ncth.covid.logging.Lumber
import de.ncth.covid.sdk.cert.toTrustedCerts
import de.ncth.covid.sdk.dependencies.SdkDependencies
import de.ncth.covid.sdk.dependencies.sdkDeps
import de.ncth.covid.sdk.utils.*
import de.ncth.covid.sdk.worker.DscListWorker
import de.ncth.covid.sdk.worker.RulesWorker
import de.ncth.covid.sdk.worker.ValueSetsWorker
import kotlinx.coroutines.runBlocking

/** Common base application with some common functionality like setting up logging. */
@OptIn(DependencyAccessor::class)
public abstract class CommonApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // IMPORTANT: The security provider has to be initialized before anything else
        initSecurityProvider()
        registerActivityLifecycleCallbacks(activityLifecycleCallbacks)

        if (isDebuggable) {
            Lumber.plantDebugTreeIfNeeded()
            httpConfig.enableLogging(HttpLogLevel.HEADERS)
            WebView.setWebContentsDebuggingEnabled(true)
        }

        navigationDeps = object : NavigationDependencies() {
            override val application = this@CommonApplication
            override val defaultScreenOrientation = Orientation.PORTRAIT
            override val animationConfig = DefaultNavigationAnimationConfig(250)
        }
        androidDeps = object : AndroidDependencies() {
            private val activityNavigator = ActivityNavigator()

            override val application: Application = this@CommonApplication

            override fun currentActivityOrNull(): FragmentActivity? =
                activityNavigator.getCurrentActivityOrNull() as? FragmentActivity
        }
        sdkDeps = object : SdkDependencies() {
            override val application: Application = this@CommonApplication
        }
        prepopulateDb()
    }

    public fun start() {
        sdkDeps.validator.updateTrustedCerts(sdkDeps.dscRepository.dscList.value.toTrustedCerts())
        initializeWorkManager(WorkManager.getInstance(this))
    }

    public open fun initializeWorkManager(workManager: WorkManager) {
        workManager.apply {
            schedulePeriodicWorker<DscListWorker>("dscListWorker")
            schedulePeriodicWorker<RulesWorker>("rulesWorker")
            schedulePeriodicWorker<ValueSetsWorker>("valueSetsWorker")
        }
    }

    public fun initializeTrueTime() {
        Thread {
            runBlocking {
                retry {
                    TrueTime
                        .build()
                        .withNtpHost(DE_NTP_HOST)
                        .withConnectionTimeout(10000)
                        .withCustomizedCache(CustomCache())
                        .initialize()
                }
                commonDeps.timeValidationRepository.validate()
            }
        }.start()
    }

    private fun prepopulateDb() {
        runBlocking {
            if (sdkDeps.covPassRulesRepository.getAllCovPassRules().isNullOrEmpty()) {
                sdkDeps.covPassRulesRepository.prepopulate(
                    sdkDeps.bundledRules
                )
            }
            if (sdkDeps.covPassValueSetsRepository.getAllCovPassValueSets().isNullOrEmpty()) {
                sdkDeps.covPassValueSetsRepository.prepopulate(
                    sdkDeps.bundledValueSets
                )
            }
            if (sdkDeps.covPassBoosterRulesRepository.getAllBoosterRules().isNullOrEmpty()) {
                sdkDeps.covPassBoosterRulesRepository.prepopulate(
                    sdkDeps.bundledBoosterRules
                )
            }
            if (sdkDeps.covPassCountriesRepository.getAllCovPassCountries().isNullOrEmpty()) {
                sdkDeps.covPassCountriesRepository.prepopulate(
                    sdkDeps.bundledCountries
                )
            }
        }
    }

    private val activityLifecycleCallbacks = object : ActivityLifecycleCallbacks {
        override fun onActivityStarted(activity: Activity) {
            enableScreenshots(activity)
        }

        override fun onActivityResumed(activity: Activity) {
            enableScreenshots(activity)
        }

        override fun onActivityPaused(activity: Activity) {
            disableScreenshots(activity)
        }

        override fun onActivityStopped(activity: Activity) {
            disableScreenshots(activity)
        }

        override fun onActivityCreated(activity: Activity, bundle: Bundle?) {}
        override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }

    private fun enableScreenshots(activity: Activity) {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    private fun disableScreenshots(activity: Activity) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    private companion object {
        private const val DE_NTP_HOST = "1.de.pool.ntp.org"
    }
}