/*
 * (C) Copyright IBM Deutschland GmbH 2021
 * (C) Copyright IBM Corp. 2021
 */

package de.ncth.covid.sdk.crypto

import de.ncth.covid.sdk.cert.TrustedCert

/**
 * Identifier for a [TrustedCert].
 */
public data class KeyIdentifier(public val data: ByteArray) {
    override fun equals(other: Any?): Boolean =
        this === other || other is KeyIdentifier && data.contentEquals(other.data)

    override fun hashCode(): Int = data.contentHashCode()
}
