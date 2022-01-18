/*
 * (C) Copyright IBM Deutschland GmbH 2021
 * (C) Copyright IBM Corp. 2021
 */

package de.ncth.covid.app.information

import com.ibm.health.common.navigation.android.FragmentNav
import de.ncth.covid.app.R
import de.ncth.covid.commonapp.information.InformationFragment
import kotlinx.parcelize.Parcelize

@Parcelize
internal class CovPassInformationFragmentNav : FragmentNav(CovPassInformationFragment::class)

/**
 * Covpass specific Information screen. Overrides the abstract functions from [InformationFragment].
 */
internal class CovPassInformationFragment : InformationFragment() {

    override fun getFAQLinkRes() = R.string.information_faq_link

    override fun getImprintLinkRes() = R.string.information_imprint_link

    override fun getEasyLanguageLinkRes(): Int = R.string.easy_language_link
}
