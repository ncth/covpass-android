/*
 * (C) Copyright IBM Deutschland GmbH 2021
 * (C) Copyright IBM Corp. 2021
 */

package de.ncth.covid.certificateValidator.onboarding

import com.ibm.health.common.navigation.android.FragmentNav
import com.ibm.health.common.navigation.android.findNavigator
import de.ncth.covid.certificateValidator.main.MainFragmentNav
import de.ncth.covid.commonapp.dependencies.commonDeps
import de.ncth.covid.commonapp.onboarding.BaseOnboardingContainerFragment
import de.ncth.covid.commonapp.storage.OnboardingRepository.Companion.CURRENT_DATA_PRIVACY_VERSION
import de.ncth.covid.commonapp.utils.SimpleFragmentStateAdapter
import kotlinx.parcelize.Parcelize

@Parcelize
internal class OnboardingContainerFragmentNav : FragmentNav(OnboardingContainerFragment::class)

/**
 * Fragment which holds the [SimpleFragmentStateAdapter] with CovPass Check specific onboarding steps.
 */
internal class OnboardingContainerFragment : BaseOnboardingContainerFragment() {

    override val fragmentStateAdapter by lazy {
        SimpleFragmentStateAdapter(
            parent = this,
            fragments = listOf(
                OnboardingInfo0Fragment(),
                OnboardingInfo1Fragment(),
                OnboardingInfo2Fragment(),
                OnboardingConsentFragment(),
            ),
        )
    }

    override fun finishOnboarding() {
        launchWhenStarted {
            commonDeps.onboardingRepository.dataPrivacyVersionAccepted
                .set(CURRENT_DATA_PRIVACY_VERSION)
            findNavigator().popAll()
            findNavigator().push(MainFragmentNav(), true)
        }
    }
}
