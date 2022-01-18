/*
 * (C) Copyright IBM Deutschland GmbH 2021
 * (C) Copyright IBM Corp. 2021
 */

package de.ncth.covid.sdk.cert

import COSE.CoseException
import COSE.Sign1Message
import de.ncth.covid.base45.Base45
import de.ncth.covid.sdk.cert.models.CBORWebToken
import de.ncth.covid.sdk.cert.models.CovCertificate
import de.ncth.covid.sdk.utils.Zlib
import java.io.IOException
import java.security.GeneralSecurityException

/**
 * Used to encode/decode QR code string.
 */
public class QRCoder(private val validator: CertValidator) {

    /** Returns the raw COSE ByteArray contained within the certificate. */
    internal fun decodeRawCose(qr: String): ByteArray {
        val qrContent = qr.removePrefix("HC1:").toByteArray()
        try {
            return Zlib.decompress(Base45.decode(qrContent))
        } catch (e: IOException) {
            throw DgcDecodeException("Not a valid zlib compressed DCC")
        }
    }

    internal fun decodeCose(qr: String): Sign1Message =
        Sign1Message.DecodeFromBytes(decodeRawCose(qr)) as? Sign1Message
            ?: throw CoseException("Not a cose-sign1 message")

    /**
     * Converts a [qrContent] to a [CovCertificate] data model.
     *
     * @throws ExpiredCwtException If the [CBORWebToken] has expired.
     * @throws BadCoseSignatureException If the signature validation failed.
     * @throws CoseException For generic COSE errors.
     * @throws GeneralSecurityException For generic cryptography errors.
     */
    public fun decodeCovCert(qrContent: String): CovCertificate =
        validator.decodeAndValidate(decodeCose(qrContent))
}

/** Thrown when the decoding of a Document Signer Certificate fails. */
public open class DgcDecodeException(message: String) : IllegalArgumentException(message)