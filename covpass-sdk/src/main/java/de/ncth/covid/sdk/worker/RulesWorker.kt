/*
 * (C) Copyright IBM Deutschland GmbH 2021
 * (C) Copyright IBM Corp. 2021
 */

package de.ncth.covid.sdk.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ensody.reactivestate.DependencyAccessor
import de.ncth.covid.logging.Lumber
import de.ncth.covid.sdk.dependencies.sdkDeps

@OptIn(DependencyAccessor::class)
public class RulesWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result =
        try {
            sdkDeps.covPassRulesRepository.loadRules()
            Result.success()
        } catch (e: Throwable) {
            Lumber.e(e)
            Result.retry()
        }
}
