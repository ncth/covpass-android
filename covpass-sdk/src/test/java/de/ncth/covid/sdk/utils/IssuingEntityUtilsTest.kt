/*
 * (C) Copyright IBM Deutschland GmbH 2021
 * (C) Copyright IBM Corp. 2021
 */

package de.ncth.covid.sdk.utils

import de.ncth.covid.sdk.cert.BlacklistedEntityException
import de.ncth.covid.sdk.cert.validateEntity
import kotlin.test.Test

internal class IssuingEntityUtilsTest {

    @Test
    fun `test valid entity`() {
        val uvci = "01DE/ABCDEFG/safs@"
        validateEntity(uvci)
    }

    @Test(expected = BlacklistedEntityException::class)
    fun `test blacklisted entity`() {
        val uvci = "01DE/foobar/safs@"
        validateEntity(uvci)
    }
}
