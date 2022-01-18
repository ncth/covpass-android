/*
 * (C) Copyright IBM Deutschland GmbH 2021
 * (C) Copyright IBM Corp. 2021
 */

package de.ncth.covid.app.detail

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.print.PdfBuilder
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import androidx.core.content.FileProvider
import com.ensody.reactivestate.BaseReactiveState
import com.ensody.reactivestate.DependencyAccessor
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.ibm.health.common.android.utils.BaseEvents
import com.ibm.health.common.android.utils.androidDeps
import com.journeyapps.barcodescanner.BarcodeEncoder
import de.ncth.covid.app.dependencies.covpassDeps
import de.ncth.covid.sdk.cert.models.CombinedCovCertificate
import de.ncth.covid.sdk.cert.models.Recovery
import de.ncth.covid.sdk.cert.models.TestCert
import de.ncth.covid.sdk.cert.models.Vaccination
import de.ncth.covid.sdk.utils.sanitizeFileName
import io.ktor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Interface to communicate events from [DetailExportPdfViewModel] to [DetailExportPdfFragment].
 */
internal interface SharePdfEvents : BaseEvents {
    fun onSharePdf(uri: Uri)
}

/**
 * ViewModel to handle business logic related to [DetailExportPdfFragment].
 */
internal class DetailExportPdfViewModel @OptIn(DependencyAccessor::class) constructor(
    scope: CoroutineScope,
    private val applicationContext: Context = androidDeps.application,
    private val providerAuthority: String = covpassDeps.fileProviderAuthority,
) : BaseReactiveState<SharePdfEvents>(scope) {

    val pdfString: MutableStateFlow<String> = MutableStateFlow("")
    private val fileName: MutableStateFlow<String> = MutableStateFlow("")

    fun onShareStart(printDocumentAdapter: PrintDocumentAdapter) {
        launch {
            val attributes = PrintAttributes.Builder()
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(PrintAttributes.Resolution("pdf", "pdf", 600, 600))
                .build()
            val fileName = "Certificate-${fileName.value}.pdf".sanitizeFileName()
            val pdfFile = File(applicationContext.cacheDir, fileName)
            PdfBuilder(attributes).createPdf(printDocumentAdapter, pdfFile)
            eventNotifier { onSharePdf(uriFromFile(applicationContext, pdfFile)) }
        }
    }

    private fun uriFromFile(context: Context, file: File): Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        FileProvider.getUriForFile(context, providerAuthority, file)
    } else {
        Uri.fromFile(file)
    }

    fun onShareClick(combinedCovCertificate: CombinedCovCertificate) {
        launch {
            fileName.value = combinedCovCertificate.covCertificate.fullName.replace(" ", "-")
            pdfString.value = when (val dgcEntry = combinedCovCertificate.covCertificate.dgcEntry) {
                is Vaccination -> {
                    PdfUtils.replaceVaccinationValues(
                        applicationContext,
                        combinedCovCertificate,
                        combinedCovCertificate.qrContent.toBase64EncodedString(),
                        dgcEntry
                    )
                }
                is Recovery -> {
                    PdfUtils.replaceRecoveryValues(
                        applicationContext,
                        combinedCovCertificate,
                        combinedCovCertificate.qrContent.toBase64EncodedString(),
                        dgcEntry
                    )
                }
                is TestCert -> {
                    PdfUtils.replaceTestCertificateValues(
                        applicationContext,
                        combinedCovCertificate,
                        combinedCovCertificate.qrContent.toBase64EncodedString(),
                        dgcEntry
                    )
                }
            }
        }
    }

    private fun String.toBase64EncodedString(): String {
        return BarcodeEncoder().encodeBitmap(
            this,
            BarcodeFormat.QR_CODE,
            619,
            619,
            mapOf(EncodeHintType.MARGIN to 0)
        ).convertToPngAndEncodeBase64()
    }

    private fun Bitmap.convertToPngAndEncodeBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray().encodeBase64()
    }
}