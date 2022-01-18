/*
 * (C) Copyright IBM Deutschland GmbH 2021
 * (C) Copyright IBM Corp. 2021
 */

package de.ncth.covid.commonapp.dependencies

import android.content.SharedPreferences
import androidx.lifecycle.LifecycleOwner
import com.ensody.reactivestate.DependencyAccessor
import de.ncth.covid.commonapp.errorhandling.CommonErrorHandler
import de.ncth.covid.commonapp.storage.OnboardingRepository
import de.ncth.covid.commonapp.truetime.TimeValidationRepository
import de.ncth.covid.commonapp.updateinfo.UpdateInfoRepository
import de.ncth.covid.sdk.dependencies.sdkDeps
import de.ncth.covid.sdk.storage.CborSharedPrefsStore
import de.ncth.covid.sdk.storage.getEncryptedSharedPreferences
import kotlinx.serialization.cbor.Cbor

/**
 * Global var for making the [CommonDependencies] accessible.
 */
@DependencyAccessor
public lateinit var commonDeps: CommonDependencies

@OptIn(DependencyAccessor::class)
public val LifecycleOwner.commonDeps: CommonDependencies get() = de.ncth.covid.commonapp.dependencies.commonDeps

/**
 * Access to various dependencies for common-app module.
 */
@OptIn(DependencyAccessor::class)
public abstract class CommonDependencies {

    /**
     * The [CommonErrorHandler].
     */
    public abstract val errorHandler: CommonErrorHandler

    private val cbor: Cbor = sdkDeps.cbor

    public val onboardingRepository: OnboardingRepository = OnboardingRepository(
        CborSharedPrefsStore("onboarding_prefs", cbor)
    )

    public val updateInfoRepository: UpdateInfoRepository = UpdateInfoRepository(
        CborSharedPrefsStore("update_info_prefs", cbor)
    )

    public val timeValidationRepository: TimeValidationRepository = TimeValidationRepository()

    public val trueTimeSharedPrefs: SharedPreferences =
        getEncryptedSharedPreferences("true_time_shared_prefs")
}