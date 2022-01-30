package android.print

import android.content.Context
import android.os.Build
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PrintDocumentAdapter.LayoutResultCallback
import android.print.PrintDocumentAdapter.WriteResultCallback
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.File

const val TAG = "PdfPrint"

@RequiresApi(Build.VERSION_CODES.KITKAT)
class PdfPrint(private val printAttributes: PrintAttributes) {
    var file: File? = null

    fun print(context: Context, printAdapter: PrintDocumentAdapter, fileName: String, callback: CallbackPrint) {
        printAdapter.onLayout(null, printAttributes, null, object : LayoutResultCallback() {
            override fun onLayoutFinished(info: PrintDocumentInfo, changed: Boolean) {
                val output =
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        getOutputFileForQAndAbove(context, fileName)
                    else getOutputFileForBelowQ(Environment.getExternalStoragePublicDirectory("Download"), fileName)

                if(output == null) {
                    Log.d(TAG, "file to be written was null")
                    callback.onFailure()
                }
                else printAdapter.onWrite(
                    arrayOf(PageRange.ALL_PAGES),
                    output,
                    CancellationSignal(),
                    object : WriteResultCallback() {
                        override fun onWriteFinished(pages: Array<PageRange>) {
                            super.onWriteFinished(pages)
                            if (pages.isNotEmpty()) {
                                callback.success(file)
                            } else {
                                Log.d(TAG, "pages are empty")
                                callback.onFailure()
                            }
                        }
                    })
            }
        }, null)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getOutputFileForQAndAbove(context: Context, fileName: String): ParcelFileDescriptor? {
        Log.d(TAG, "above android q")
        try {
            file = File.createTempFile(fileName, "pdf", context.cacheDir)
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open ParcelFileDescriptor1", e)
        }
        return null
    }

    private fun getOutputFileForBelowQ(path: File, fileName: String): ParcelFileDescriptor? {
        Log.d(TAG, "below android q")
        if (!path.exists()) {
            path.mkdirs()
        }
        file = File(path, "$fileName.pdf")
        return try {
            file!!.createNewFile()
            ParcelFileDescriptor.open(file, 805306368)
        } catch (var5: java.lang.Exception) {
            Log.e(TAG, "Failed to open ParcelFileDescriptor", var5)
            null
        }
    }

    interface CallbackPrint {
        fun success(file: File?)
        fun onFailure()
    }

}