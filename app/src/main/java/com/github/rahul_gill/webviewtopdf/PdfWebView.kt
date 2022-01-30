package com.github.rahul_gill.webviewtopdf

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.print.PdfPrint
import android.print.PdfPrint.CallbackPrint
import android.print.PrintAttributes
import android.print.PrintAttributes.Resolution
import android.provider.MediaStore
import android.util.Log
import android.webkit.WebView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import java.io.File
import java.io.InputStream


object PdfGenUtil {

    private const val REQUEST_CODE = 101

    fun downloadPdf(activity: Activity, webView: WebView, fileName: String, onError: (String?) -> Unit, onSuccess: () -> Unit){
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            createWebPrintJob(activity, webView, fileName, object : Callback {
                override fun success(path: File?) {
                    if (path != null) {
                        try {
                            viewPdfFile(activity, path.absolutePath)
                            onSuccess()
                        } catch (e: Exception) {
                            onError(null)
                            e.printStackTrace()
                        }
                    }
                }

                override fun failure(message: String?) {
                    onError(message)
                }
            })
        } else Toast.makeText(activity, "This feature is not available in this android version", Toast.LENGTH_SHORT).show()

    }

    fun sharePdf(activity: Activity, webView: WebView, fileName: String, onError: (String?) -> Unit, onSuccess: () -> Unit){
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            createWebPrintJob(activity, webView, fileName, object : Callback {
                override fun success(path: File?) {
                    if (path != null) {
                        try {
                            sharePdfFile(activity, path.absolutePath)
                            onSuccess()
                        } catch (e: Exception) {
                            onError(null)
                            e.printStackTrace()
                        }
                    }
                }

                override fun failure(message: String?) {
                    onError(message)
                }
            })
        } else Toast.makeText(activity, "This feature is not available in this android version", Toast.LENGTH_SHORT).show()

    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun createWebPrintJob(activity: Activity, webView: WebView, fileName: String, callback: Callback) {
        Log.d(TAG, "sdk version ${Build.VERSION.SDK_INT}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE)
                callback.failure("NO_PERMISSION")
                Log.d("PDF_DEBUG", "failure no permission")
                return
            }
        }


        val jobName = activity.getString(R.string.app_name) + " Document"
        val pdfPrint = PdfPrint(printAttributes =
        PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setResolution(Resolution("pdf", "pdf", 600, 600))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS).build()
        )

        pdfPrint.print(activity, webView.createPrintDocumentAdapter(jobName), fileName, object : CallbackPrint {
            override fun success(file: File?) {
                if(file == null) {
                    callback.failure()
                    Log.d("PDF_DEBUG", "failure in createWebPrintJob file was null")
                }
                else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        saveDownload(activity, "$fileName.pdf", file.inputStream())
                    callback.success(file)
                }
            }
            override fun onFailure() {
                callback.failure()
                Log.d("PDF_DEBUG", "failure in createWebPrintJob")
            }
        })
    }


    fun viewPdfFile(activity: Activity, path: String) {
        val uri: Uri = FileProvider.getUriForFile(activity,BuildConfig.APPLICATION_ID +  ".provider", File(path))

        val target = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        //TODO: hardcoded string
        val intent = Intent.createChooser(target, "Open File")
        try {
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(activity, "No application to open the file", Toast.LENGTH_SHORT).show()
        }
    }

    fun sharePdfFile(activity: Activity, path: String){
        val uri: Uri = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID +  ".provider", File(path))
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "This is example")
        }
        activity.startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveDownload(context: Context, fileName: String, pdfInputStream: InputStream) {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val itemUri = resolver.insert(collection, values)
        if (itemUri != null) {
            resolver.openFileDescriptor(itemUri, "w").use { parcelFileDescriptor ->
                ParcelFileDescriptor.AutoCloseOutputStream(parcelFileDescriptor)
                    .write(pdfInputStream.readBytes())
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(itemUri, values, null, null)
        }
    }

    interface Callback {
        fun success(path: File?)
        fun failure(message: String? = null)
    }
}