/*
 * (C) Copyright IBM Deutschland GmbH 2021
 * (C) Copyright IBM Corp. 2021
 */

package de.ncth.covid.app.dependencies

import androidx.lifecycle.LifecycleOwner
import com.ensody.reactivestate.DependencyAccessor
import com.ibm.health.common.android.utils.androidDeps
import de.ncth.covid.app.checkerremark.CheckerRemarkRepository
import de.ncth.covid.app.common.ToggleFavoriteUseCase
import de.ncth.covid.sdk.dependencies.sdkDeps
import de.ncth.covid.sdk.storage.CborSharedPrefsStore
import de.ncth.covid.sdk.storage.CertRepository
import kotlinx.serialization.cbor.Cbor

/**
 * Global var for making the [CovpassDependencies] accessible.
 */
@DependencyAccessor
internal lateinit var covpassDeps: CovpassDependencies

@OptIn(DependencyAccessor::class)
internal val LifecycleOwner.covpassDeps: CovpassDependencies get() = de.ncth.covid.app.dependencies.covpassDeps

/**
 * Access to various dependencies for common-app-covpass module.
 */
@OptIn(DependencyAccessor::class)
internal abstract class CovpassDependencies {

    private val cbor: Cbor get() = sdkDeps.cbor

    private val certificateListMapper get() = sdkDeps.certificateListMapper

    val fileProviderAuthority: String get() = androidDeps.application.packageName + ".covpass.provider"

    val certRepository: CertRepository by lazy {
        CertRepository(
            CborSharedPrefsStore("covpass_prefs", cbor),
            certificateListMapper,
        )
    }

    val checkerRemarkRepository: CheckerRemarkRepository by lazy {
        CheckerRemarkRepository(
            CborSharedPrefsStore("checker_remark_prefs", cbor),
        )
    }

    val toggleFavoriteUseCase by lazy { ToggleFavoriteUseCase(certRepository) }
}