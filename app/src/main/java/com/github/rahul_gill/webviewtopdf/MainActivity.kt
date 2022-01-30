package com.github.rahul_gill.webviewtopdf

import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.rahul_gill.webviewtopdf.databinding.ActivityMainBinding


const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnDownload.setOnButtonClick {
            downloadPdf(binding.pdfWebView)
        }
        binding.btnShare.setOnButtonClick {
            sharePdf(binding.pdfWebView)
        }
        binding.pdfWebView.settings.apply {
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            setSupportZoom(true)
        }

        binding.pdfWebView.loadUrl("https://news.ycombinator.com/")


        binding.pdfWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                binding.pdfWebView.visibility = View.VISIBLE
                binding.linearLayout3.visibility = View.VISIBLE
                binding.progressCircular.visibility = View.GONE
            }
        }


    }

    private fun onError(noPermission: Boolean) {
        Log.d(TAG, "pdf gen error occurred noPermission: $noPermission")
        setButtonLoadingFalse()
        if(!noPermission)
            Toast.makeText(this, "Error occurred", Toast.LENGTH_SHORT).show()
    }

    private fun setButtonLoadingFalse() {
        binding.btnDownload.isLoading = false
        binding.btnShare.isLoading = false
    }

    private fun downloadPdf(webView: WebView) {
        binding.btnDownload.isLoading = true
        val fileName = "downloaded_file.pdf"
        PdfGenUtil.downloadPdf(this, webView, fileName, { onError("NO_PERMISSION" == it) }, ::setButtonLoadingFalse)
    }

    private fun sharePdf(webView: WebView) {
        binding.btnShare.isLoading = true
        val fileName = "shared_file.pdf"
        PdfGenUtil.sharePdf(this, webView, fileName, { onError("NO_PERMISSION" == it) }, ::setButtonLoadingFalse)
    }
}