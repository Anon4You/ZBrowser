package alienkrishn.zbrowser;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

/**
 * Settings Activity for ZBrowser
 */
public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private static final String PREFS_NAME = "ZBrowserPrefs";

    // UI Elements
    private EditText editHomePage;
    private CheckBox checkJavaScript;
    private CheckBox checkImages;
    private CheckBox checkZoom;
    private CheckBox checkDarkMode;
    private CheckBox checkDesktopMode;
    private CheckBox checkBlockAds;
    private Spinner spinnerSearchEngine;
    private Button btnSave, btnClearData, btnBookmarks, btnAbout;

    // Search engines
    private static final String[] SEARCH_ENGINES = {
        "Google - https://www.google.com/search?q=",
        "Bing - https://www.bing.com/search?q=",
        "DuckDuckGo - https://duckduckgo.com/?q=",
        "Baidu - https://www.baidu.com/s?wd="
    };

    private static final String[] SEARCH_URLS = {
        "https://www.google.com/search?q=",
        "https://www.bing.com/search?q=",
        "https://duckduckgo.com/?q=",
        "https://www.baidu.com/s?wd="
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        initView();
        loadSettings();
        setupListeners();
    }

    private void initView() {
        editHomePage = findViewById(R.id.editHomePage);
        checkJavaScript = findViewById(R.id.checkJavaScript);
        checkImages = findViewById(R.id.checkImages);
        checkZoom = findViewById(R.id.checkZoom);
        checkDarkMode = findViewById(R.id.checkDarkMode);
        checkDesktopMode = findViewById(R.id.checkDesktopMode);
        checkBlockAds = findViewById(R.id.checkBlockAds);
        spinnerSearchEngine = findViewById(R.id.spinnerSearchEngine);
        btnSave = findViewById(R.id.btnSave);
        btnClearData = findViewById(R.id.btnClearData);
        btnBookmarks = findViewById(R.id.btnBookmarks);
        btnAbout = findViewById(R.id.btnAbout);

        // Setup search engine spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, SEARCH_ENGINES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSearchEngine.setAdapter(adapter);
    }

    private void loadSettings() {
        // Load home page
        editHomePage.setText(prefs.getString("homePage", "https://www.google.com/"));

        // Load JavaScript setting
        checkJavaScript.setChecked(prefs.getBoolean("javaScript", true));

        // Load Images setting
        checkImages.setChecked(prefs.getBoolean("images", true));

        // Load Zoom setting
        checkZoom.setChecked(prefs.getBoolean("zoom", true));

        // Load Dark Mode setting
        checkDarkMode.setChecked(prefs.getBoolean("darkMode", false));

        // Load Desktop Mode setting
        checkDesktopMode.setChecked(prefs.getBoolean("desktopMode", false));

        // Load Block Ads setting
        checkBlockAds.setChecked(prefs.getBoolean("blockAds", false));

        // Load Search Engine
        int searchEngine = prefs.getInt("searchEngine", 0);
        spinnerSearchEngine.setSelection(searchEngine);
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> saveSettings());
        btnClearData.setOnClickListener(v -> showClearDataDialog());
        btnBookmarks.setOnClickListener(v -> showBookmarks());
        btnAbout.setOnClickListener(v -> showAboutDialog());
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = prefs.edit();

        // Save home page
        String homePage = editHomePage.getText().toString().trim();
        if (homePage.isEmpty()) {
            homePage = "https://www.google.com/";
        }
        editor.putString("homePage", homePage);

        // Save settings
        editor.putBoolean("javaScript", checkJavaScript.isChecked());
        editor.putBoolean("images", checkImages.isChecked());
        editor.putBoolean("zoom", checkZoom.isChecked());
        editor.putBoolean("darkMode", checkDarkMode.isChecked());
        editor.putBoolean("desktopMode", checkDesktopMode.isChecked());
        editor.putBoolean("blockAds", checkBlockAds.isChecked());
        editor.putInt("searchEngine", spinnerSearchEngine.getSelectedItemPosition());

        editor.apply();

        Toast.makeText(this, "Settings saved! Some changes require restart.", Toast.LENGTH_SHORT).show();

        // Apply search engine to MainActivity
        MainActivity.setSearchUrl(SEARCH_URLS[spinnerSearchEngine.getSelectedItemPosition()]);
    }

    private void showClearDataDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Browsing Data")
                .setMessage("Are you sure you want to clear all browsing data? This includes history, cache, and cookies. This action cannot be undone.")
                .setPositiveButton("Clear", (dialog, which) -> clearBrowsingData())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearBrowsingData() {
        try {
            WebView webView = new WebView(this);
            webView.clearHistory();
            webView.clearCache(true);
            webView.clearFormData();

            // Clear cookies
            android.webkit.CookieManager.getInstance().removeAllCookies(null);
            android.webkit.CookieManager.getInstance().flush();

            Toast.makeText(this, "Browsing data cleared!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error clearing data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showBookmarks() {
        SharedPreferences bookmarks = getSharedPreferences("Bookmarks", MODE_PRIVATE);
        java.util.Map<String, ?> allBookmarks = bookmarks.getAll();

        if (allBookmarks.isEmpty()) {
            Toast.makeText(this, "No bookmarks saved", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Bookmarks");

        String[] bookmarkTitles = new String[allBookmarks.size()];
        int i = 0;
        for (String key : allBookmarks.keySet()) {
            String value = (String) allBookmarks.get(key);
            String[] parts = value.split("\\|");
            bookmarkTitles[i] = parts.length > 0 ? parts[0] : "Unknown";
            i++;
        }

        builder.setItems(bookmarkTitles, (dialog, which) -> {
            // Handle bookmark click - could open the bookmark
            Toast.makeText(this, "Bookmark selected", Toast.LENGTH_SHORT).show();
        });

        builder.setPositiveButton("Done", null);
        builder.setNeutralButton("Clear All", (dialog, which) -> {
            SharedPreferences.Editor editor = bookmarks.edit();
            editor.clear();
            editor.apply();
            Toast.makeText(this, "All bookmarks cleared", Toast.LENGTH_SHORT).show();
        });

        builder.show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("About ZBrowser")
                .setMessage("ZBrowser - Lightweight Advanced Web Browser\n\n" +
                        "Version: 1.0\n" +
                        "Based on MkBrowser by Mengkun\n\n" +
                        "Features:\n" +
                        "- Tab browsing\n" +
                        "- Incognito mode\n" +
                        "- Download manager\n" +
                        "- Desktop mode\n" +
                        "- Night mode\n" +
                        "- Bookmarks\n" +
                        "- Find in page\n\n" +
                        "A fast, simple, and privacy-focused mobile browser.")
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
