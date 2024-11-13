package com.example.wifi1; // Replace with your package name

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "UserCredentials";
    private static final String KEY_USERNAME_LIST = "username_list";
    private static final String KEY_PASSWORD = "password";
    private static String username;

    private WebView webView;
    private List<String> usernameList = new ArrayList<String>();
    private String password;

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
                    if (!usernameList.contains(new String(charid))) {
                        usernameList.add(new String(charid));
                    }
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        webView.setWebViewClient(new WebViewClient());

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        // Load saved username list and password
        loadCredentials();

        // If the username list is empty, show the dialog to ask for credentials
        if (usernameList.isEmpty() || password == null) {
            showCredentialsDialog(); // Prompt user to enter credentials if not saved
        } else {
            loadWebViewWithCredentials(); // Auto-fill credentials if saved
        }
    }

    private void loadCredentials() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Load password
        password = sharedPreferences.getString(KEY_PASSWORD, null);



        // Load username list from JSON
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

        // Save password
        editor.putString(KEY_PASSWORD, password);

        // Add new username to the list if not already present
        if (!usernameList.contains(username)) {
            usernameList.add(username);
        }
        changeUsernameList();
        // Save the username list as JSON
        JSONArray jsonArray = new JSONArray(usernameList);
        editor.putString(KEY_USERNAME_LIST, jsonArray.toString());
        editor.apply();
    }

    private void showCredentialsDialog() {
        // Create a dialog to ask for username and password
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Credentials");

        // Layout for input fields
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        // Username input
        final EditText usernameInput = new EditText(this);
        usernameInput.setHint("Username");
        layout.addView(usernameInput);

        // Password input
        final EditText passwordInput = new EditText(this);
        passwordInput.setHint("Password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(passwordInput);

        builder.setView(layout);

        // Set dialog buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String username = usernameInput.getText().toString();
                password = passwordInput.getText().toString();
                saveCredentials(username, password); // Save credentials
                loadWebViewWithCredentials(); // Load WebView with entered credentials
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void loadWebViewWithCredentials() {
        // Load the URL
        webView.loadUrl("http://172.18.10.10:1000/logout?");

        // Inject JavaScript to fill in the username and password after the page loads
        webView.setWebViewClient(new WebViewClient() {

            int i = 1;
            @Override
            public void onPageFinished(WebView view, String url) {

                if (!usernameList.isEmpty()) {
                    // Use the first username in the list, you can adjust if you want to select a specific username
                    String jsCode = "document.getElementById('ft_un').value = '" + usernameList.get(i) + "';" +
                            "document.getElementById('ft_pd').value = '" + password + "';" +
                            "document.forms[0].submit();"; // Submit the form if needed
                    Log.d(TAG, "Username"+username);
                    i++;
                    webView.evaluateJavascript(jsCode, null);
                }
            }
        });
    }
    private static final String TAG = "MainActivity";
    private void createList() {
        String un_temp = KEY_USERNAME_LIST;
    }
}
