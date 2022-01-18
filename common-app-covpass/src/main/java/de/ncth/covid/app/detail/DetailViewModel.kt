/*
 * (C) Copyright IBM Deutschland GmbH 2021
 * (C) Copyright IBM Corp. 2021
 */

package de.ncth.covid.app.detail

import com.ensody.reactivestate.BaseReactiveState
import com.ensody.reactivestate.DependencyAccessor
import com.ibm.health.common.android.utils.BaseEvents
import de.ncth.covid.app.common.ToggleFavoriteUseCase
import de.ncth.covid.app.dependencies.CovpassDependencies
import de.ncth.covid.app.dependencies.covpassDeps
import de.ncth.covid.sdk.cert.models.BoosterResult
import de.ncth.covid.sdk.cert.models.GroupedCertificatesId
import kotlinx.coroutines.CoroutineScope

internal interface DetailEvents<T> : BaseEvents {
    fun onHasSeenBoosterDetailNotificationUpdated(tag: T)
}

internal class DetailViewModel<T> @OptIn(DependencyAccessor::class) constructor(
    scope: CoroutineScope,
    private val covpassDependencies: CovpassDependencies = covpassDeps,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase = covpassDeps.toggleFavoriteUseCase,
) : BaseReactiveState<DetailEvents<T>>(scope) {

    fun onFavoriteClick(certId: GroupedCertificatesId) {
        launch {
            toggleFavoriteUseCase.toggleFavorite(certId)
        }
    }

    fun updateHasSeenBoosterDetailNotification(certId: GroupedCertificatesId, tag: T) {
        launch {
            covpassDependencies.certRepository.certs.update { groupedCertificateList ->
                groupedCertificateList.certificates.find { it.id == certId }?.let {
                    if (it.boosterNotification.result == BoosterResult.Passed) {
                        it.hasSeenBoosterDetailNotification = true
                    }
                }
            }
            eventNotifier { onHasSeenBoosterDetailNotificationUpdated(tag) }
        }
    }
}
