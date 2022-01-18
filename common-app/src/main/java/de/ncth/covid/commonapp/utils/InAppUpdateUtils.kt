/*
 * (C) Copyright IBM Deutschland GmbH 2021
 * (C) Copyright IBM Corp. 2021
 */

package de.ncth.covid.commonapp.utils

import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.model.UpdateAvailability
import com.ibm.health.common.android.utils.getString
import com.ibm.health.common.navigation.android.Navigator
import de.ncth.covid.commonapp.BaseActivity.Companion.UPDATE_AVAILABLE_TAG
import de.ncth.covid.commonapp.R
import de.ncth.covid.commonapp.dialog.DialogModel
import de.ncth.covid.commonapp.dialog.showDialog

internal fun startLookingForUpdate(appUpdateManager: AppUpdateManager, navigator: Navigator) {
    // Returns an intent object that you use to check for an update.
    val appUpdateInfoTask = appUpdateManager.appUpdateInfo

    // Checks if there is an update available
    appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
        if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
            val dialogModel = DialogModel(
                titleRes = R.string.dialog_start_screen_title,
                messageString = getString(R.string.dialog_start_screen_message),
                positiveButtonTextRes = R.string.dialog_start_screen_button_update,
                negativeButtonTextRes = R.string.dialog_start_screen_button_later,
                tag = UPDATE_AVAILABLE_TAG
            )
            showDialog(dialogModel, navigator.fragmentManager)
        }
    }
}
