/*
 * (C) Copyright IBM Deutschland GmbH 2021
 * (C) Copyright IBM Corp. 2021
 */

package de.ncth.covid.app.main

import android.os.Bundle
import de.ncth.covid.app.onboarding.WelcomeFragmentNav
import de.ncth.covid.commonapp.BaseActivity
import de.ncth.covid.commonapp.dependencies.commonDeps
import de.ncth.covid.commonapp.storage.OnboardingRepository.Companion.CURRENT_DATA_PRIVACY_VERSION

/**
 * The only Activity in the app, hosts all fragments.
 * Initially triggers the navigation to [WelcomeFragmentNav] or [MainFragmentNav].
 */
internal class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (navigator.isEmpty() && savedInstanceState == null) {
            if (commonDeps.onboardingRepository.dataPrivacyVersionAccepted.value >= CURRENT_DATA_PRIVACY_VERSION) {
                navigator.push(MainFragmentNav())
            } else {
                navigator.push(WelcomeFragmentNav())
            }
        }
    }
}
