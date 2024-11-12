package com.example.wifi1; // Replace with your package name

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private boolean shouldInjectJavaScript = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find the WebView and configure it
        WebView webView = findViewById(R.id.webview);
        webView.setWebViewClient(new WebViewClient() {

            public void onPageFinished(WebView view, String url) {
                // JavaScript to fill text fields
                String jsCode = "document.getElementById('ft_un').value = '²³BCE978³';" +
                        "document.getElementById('ft_pd').value = 'ejxFw4tr';" +
                        "document.forms[0].submit();"; // Submit the form if needed

                // Inject the JavaScript into the WebView
                webView.evaluateJavascript(jsCode, null);
            }
        }); // Opens URL in WebView instead of browser

        // Enable JavaScript (if required by the webpage)
        WebSettings webSettings = webView.getSettings();
        webView.getSettings().setJavaScriptEnabled(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        webSettings.setJavaScriptEnabled(true);

        // Load the URL
        webView.loadUrl("http://172.18.10.10:1000/logout?"); // Replace with the desired URL
    }
    public void stopJavaScriptInjection() {
        shouldInjectJavaScript = false;
    }
}
