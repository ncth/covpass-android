/*
 * (C) Copyright IBM Deutschland GmbH 2021
 * (C) Copyright IBM Corp. 2021
 */

package de.ncth.covid.certificateValidator

import com.ensody.reactivestate.DependencyAccessor
import de.ncth.covid.certificateValidator.errorhandling.ErrorHandler
import de.ncth.covid.commonapp.CommonApplication
import de.ncth.covid.commonapp.dependencies.CommonDependencies
import de.ncth.covid.commonapp.dependencies.commonDeps

/**
 * Application class of CovPass Check.
 */
@OptIn(DependencyAccessor::class)
public class App : CommonApplication() {

    override fun onCreate() {
        super.onCreate()
        commonDeps = object : CommonDependencies() {
            override val errorHandler = ErrorHandler()
        }
        start()
        initializeTrueTime()
    }
}
