/*
 * (C) Copyright IBM Deutschland GmbH 2021
 * (C) Copyright IBM Corp. 2021
 */

package de.ncth.covid.sdk.cert

import COSE.OneKey
import de.ncth.covid.base45.Base45DecodeException
import de.ncth.covid.sdk.cert.models.CBORWebToken
import de.ncth.covid.sdk.crypto.readPem
import de.ncth.covid.sdk.dependencies.defaultCbor
import de.ncth.covid.sdk.utils.BaseSdkTest
import de.ncth.covid.sdk.utils.readResource
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class QRCoderTest : BaseSdkTest() {
    val sealCert by lazy { readPem(readResource("seal-cert.pem")).first() }
    val data by lazy { readResource("vaccination-cert.txt").replace("\n", "") }
    val validator by lazy {
        CertValidator(
            listOf(TrustedCert(country = "DE", kid = "asdf", certificate = sealCert)),
            defaultCbor,
        )
    }
    val qrCoder by lazy { QRCoder(validator) }

    @Test
    fun `validate cert`() {
        val cose = qrCoder.decodeCose(data)
        assertTrue(cose.validate(OneKey(sealCert.publicKey, null)))
    }

    @Test
    fun `expired certificate`() {
        assertFailsWith<ExpiredCwtException> { qrCoder.decodeCovCert(data) }
    }

    @Test
    fun `decode broken certificate`() {
        assertFailsWith<DgcDecodeException> {
            qrCoder.decodeCose(data.slice(0 until data.length - 3))
        }
        assertFailsWith<Base45DecodeException> {
            qrCoder.decodeCose(data.slice(0 until data.length - 2))
        }
    }

    @Test
    fun `decode certificate`() {
        val cose = qrCoder.decodeCose(data)
        // Skip seal validation and only decode + validate the QR data itself
        validator.decodeAndValidate(CBORWebToken.decode(cose.GetContent()), sealCert)
    }
}
