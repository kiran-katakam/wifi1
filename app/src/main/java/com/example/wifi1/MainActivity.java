package com.example.wifi1;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.NetworkCapabilities;
import android.content.Context;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "UserCredentials";
    private static final String KEY_USERNAME_LIST = "username_list";
    private static final String KEY_PASSWORD = "password";
    private static String username;
    private Network wifiNetwork = null;

    private WebView webView;
    private List<String> usernameList = new ArrayList<String>();
    private String password;
    private ActivityResultLauncher<Intent> wifiSettingsLauncher;
    private static final String TAG = "MainActivity";

    @Override
    public void onBackPressed() {
        finishAffinity(); // Closes all activities and exits the app
        System.exit(0);   // Optional: Ensures the process is killed
    }

    @Override
    protected void onStop() {
        super.onStop();
        finishAffinity();
        System.exit(0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        wifiSettingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "Returned from Wi-Fi settings");
                    bindToWifiNetwork();
                }
        );

        loadCredentials();

        if (usernameList.isEmpty() || password == null) {
            showCredentialsDialog();
        } else {
            bindToWifiNetwork();
        }
    }

    private void initWebView() {
        webView = findViewById(R.id.webview);
        webView.setWebViewClient(new WebViewClient());

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
    }

    private void proceedAfterWifiBinding() {
        initWebView();
        loadWebViewWithCredentials();
    }

    private void loadCredentials() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        password = sharedPreferences.getString(KEY_PASSWORD, null);

        try {
            String usernameJson = sharedPreferences.getString(KEY_USERNAME_LIST, "[]");
            JSONArray jsonArray = new JSONArray(usernameJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                usernameList.add(jsonArray.getString(i));
            }
            if (!usernameList.isEmpty()) {
                username = usernameList.get(0);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void saveCredentials(String username, String password) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(KEY_PASSWORD, password);

        if (!usernameList.contains(username)) {
            usernameList.add(username);
        }
        changeUsernameList();

        JSONArray jsonArray = new JSONArray(usernameList);
        editor.putString(KEY_USERNAME_LIST, jsonArray.toString());
        editor.apply();
    }

    private void showCredentialsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Credentials");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText usernameInput = new EditText(this);
        usernameInput.setHint("Username");
        layout.addView(usernameInput);

        final EditText passwordInput = new EditText(this);
        passwordInput.setHint("Password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(passwordInput);

        builder.setView(layout);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String username = usernameInput.getText().toString();
                password = passwordInput.getText().toString();
                saveCredentials(username, password);
                bindToWifiNetwork();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }


    private void loadWebViewWithCredentials() {

        Toast loadingToast = Toast.makeText(MainActivity.this, "⏳ Loading the portal... Please wait.", Toast.LENGTH_SHORT);
        loadingToast.show();

        webView.loadUrl("http://172.18.10.10:1000/logout?");

        webView.setWebViewClient(new WebViewClient() {
            int i = 1;

            @Override
            public void onPageFinished(WebView view, String url) {
                if (!usernameList.isEmpty()) {
                    String jsCode = "document.getElementById('ft_un').value = '" + usernameList.get(i) + "';" +
                            "document.getElementById('ft_pd').value = '" + password + "';" +
                            "document.forms[0].submit();";

                    Log.d(TAG, "Username: " + usernameList.get(i));

                    i++;
                    i = i % usernameList.size();
                    webView.evaluateJavascript(jsCode, null);
                }

                String jsCheck = "(() => {" +
                        "   const text = document.body.innerText.toLowerCase();" +
                        "   if (text.includes('successfully') && text.includes('logged') && text.includes('out')) {" +
                        "       return 'loggedout';" +
                        "   } else if (text.includes('successful') && text.includes('authentication')) {" +
                        "       return 'loggedin';" +
                        "   } else {" +
                        "       return 'unknown';" +
                        "   }" +
                        "})()";

                webView.evaluateJavascript(jsCheck, value -> {
                    if (value != null) {

                        value = value.replace("\"", "").trim();

                        Log.d(TAG, "Status: " + value);

                        if (value.equals("loggedout")) {

                            Toast.makeText(MainActivity.this, "✅  Wi-Fi Logged Out Successfully", Toast.LENGTH_SHORT).show();

                        } else if (value.equals("loggedin")) {

                            Toast.makeText(MainActivity.this, "✅  Wi-Fi Logged In Successfully", Toast.LENGTH_SHORT).show();
                            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                            connectivityManager.reportNetworkConnectivity(wifiNetwork, true);

                        }
                    }
                });

            }
        });


    }


    private void connectToWifi() {
        Toast.makeText(this, "📶 Connect to Wi-Fi and Come Back", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Wi-Fi is not connected");

        Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
        wifiSettingsLauncher.launch(intent);
    }

    private void bindToWifiNetwork() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();

        wifiNetwork = null;

        Network[] allNetworks = connectivityManager.getAllNetworks();
        for (Network network : allNetworks) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                wifiNetwork = network;
                break;
            }
        }

        if (wifiNetwork != null) {
            connectivityManager.bindProcessToNetwork(wifiNetwork);
            Log.d(TAG, "Already connected and bound to Wi-Fi");
            proceedAfterWifiBinding();
        } else {
            connectToWifi();
        }
    }



    void changeUsernameList() {
        final char[] superscripts = { '⁰', '¹', '²', '³', '⁴', '⁵', '⁶', '⁷', '⁸', '⁹' };
        String id = usernameList.get(0);
        for (int k = 0; k <= usernameList.size(); k++) {
            if (k < usernameList.size()) {
                id = usernameList.get(k);
            }
            for (int i = 0; i < id.length(); i++) {
                char charid[] = id.toCharArray();
                if (Character.isDigit(charid[i])) {
                    charid[i] = superscripts[Character.getNumericValue(charid[i])];
                    String newId = new String(charid);
                    if (!usernameList.contains(newId)) {
                        usernameList.add(newId);
                    }
                }
            }
        }
    }

    private void createList() {
        String un_temp = KEY_USERNAME_LIST;
    }
}
