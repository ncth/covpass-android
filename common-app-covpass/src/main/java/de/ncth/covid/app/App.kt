/*
 * (C) Copyright IBM Deutschland GmbH 2021
 * (C) Copyright IBM Corp. 2021
 */

package de.ncth.covid.app

import androidx.work.WorkManager
import com.ensody.reactivestate.DependencyAccessor
import de.ncth.covid.app.dependencies.CovpassDependencies
import de.ncth.covid.app.dependencies.covpassDeps
import de.ncth.covid.app.errorhandling.ErrorHandler
import de.ncth.covid.commonapp.CommonApplication
import de.ncth.covid.commonapp.dependencies.CommonDependencies
import de.ncth.covid.commonapp.dependencies.commonDeps
import de.ncth.covid.commonapp.utils.schedulePeriodicWorker
import de.ncth.covid.sdk.worker.BoosterRulesWorker
import de.ncth.covid.sdk.worker.CountriesWorker

/**
 * Application class which defines dependencies for the Covpass App
 */
@OptIn(DependencyAccessor::class)
internal class App : CommonApplication() {

    override fun onCreate() {
        super.onCreate()
        covpassDeps = object : CovpassDependencies() {}
        commonDeps = object : CommonDependencies() {
            override val errorHandler = ErrorHandler()
        }
        start()
    }

    override fun initializeWorkManager(workManager: WorkManager) {
        super.initializeWorkManager(workManager)
        workManager.apply {
            schedulePeriodicWorker<BoosterRulesWorker>("boosterRulesWorker")
            schedulePeriodicWorker<CountriesWorker>("countriesWorker")
        }
    }
}
