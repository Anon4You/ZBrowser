package alienkrishn.zbrowser;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import alienkrishn.zbrowser.adapter.TabsAdapter;
import alienkrishn.zbrowser.model.TabInfo;

/**
 * ZBrowser - A lightweight advanced web browser
 * Based on MkBrowser by Mengkun (mkblog.cn)
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private WebView webView;
    private ProgressBar progressBar;
    private EditText textUrl;
    private ImageView webIcon, goBack, goForward, navSet, goHome, btnStart, btnTabs;
    private TextView tabCount;

    private long exitTime = 0;

    private Context mContext;
    private InputMethodManager manager;

    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    private static final int PRESS_BACK_EXIT_GAP = 2000;

    // Search URL (can be changed from settings)
    private static String searchUrl = "https://www.google.com/search?q=";

    // Track if user is searching (not navigating to URL)
    private boolean isSearching = false;
    private String searchQuery = "";

    // Tabs management
    private List<TabInfo> tabs;
    private int currentTabIndex = 0;
    private boolean isIncognitoMode = false;

    // Fullscreen
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private int originalOrientation;

    // Settings
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "ZBrowserPrefs";

    // Night mode
    private boolean isNightMode = false;

    public static void setSearchUrl(String url) {
        searchUrl = url;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Prevent bottom buttons from being pushed up
        getWindow().setSoftInputMode
                (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN |
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        setContentView(R.layout.activity_main);

        mContext = MainActivity.this;
        manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Initialize tabs list
        tabs = new ArrayList<>();

        // Bind views
        initView();

        // Load settings
        loadSettings();

        // Initialize first tab
        createNewTab(false);

        // Initialize WebView
        initWeb();
    }

    /**
     * Bind views
     */
    private void initView() {
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        textUrl = findViewById(R.id.textUrl);
        webIcon = findViewById(R.id.webIcon);
        btnStart = findViewById(R.id.btnStart);
        goBack = findViewById(R.id.goBack);
        goForward = findViewById(R.id.goForward);
        navSet = findViewById(R.id.navSet);
        goHome = findViewById(R.id.goHome);
        btnTabs = findViewById(R.id.btnTabs);
        tabCount = findViewById(R.id.tabCount);

        // Bind click listeners
        btnStart.setOnClickListener(this);
        goBack.setOnClickListener(this);
        goForward.setOnClickListener(this);
        navSet.setOnClickListener(this);
        goHome.setOnClickListener(this);
        btnTabs.setOnClickListener(this);

        // Address bar focus and blur handling
        textUrl.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    // Show current URL
                    textUrl.setText(webView.getUrl());
                    // Cursor at the end
                    textUrl.setSelection(textUrl.getText().length());
                    // Show internet icon
                    webIcon.setImageResource(R.drawable.internet);
                    // Show go button
                    btnStart.setImageResource(R.drawable.go);
                } else {
                    // Show site name
                    textUrl.setText(webView.getTitle());
                    // Show site icon
                    webIcon.setImageBitmap(webView.getFavicon());
                    // Show refresh button
                    btnStart.setImageResource(R.drawable.refresh);
                }
            }
        });

        // Listen for keyboard enter to search
        textUrl.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                if (keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    // Execute search
                    btnStart.callOnClick();
                    textUrl.clearFocus();
                }
                return false;
            }
        });
    }

    /**
     * Load settings from SharedPreferences
     */
    private void loadSettings() {
        // Load search engine
        int searchEngine = prefs.getInt("searchEngine", 0);
        String[] searchUrls = {
            "https://www.google.com/search?q=",
            "https://www.bing.com/search?q=",
            "https://duckduckgo.com/?q=",
            "https://www.baidu.com/s?wd="
        };
        searchUrl = searchUrls[searchEngine];

        // Load night mode
        isNightMode = prefs.getBoolean("darkMode", false);
        if (isNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }

    /**
     * Create a new tab
     */
    private void createNewTab(boolean isIncognito) {
        TabInfo newTab = new TabInfo("New Tab", "", isIncognito);
        tabs.add(newTab);
        currentTabIndex = tabs.size() - 1;
        updateTabCount();
    }

    /**
     * Update tab count display
     */
    private void updateTabCount() {
        if (tabCount != null) {
            tabCount.setText(String.valueOf(tabs.size()));
        }
    }

    /**
     * Show tabs overview
     */
    private void showTabsOverview() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tabs (" + tabs.size() + ")");

        // Create tabs list view
        LinearLayout tabsLayout = new LinearLayout(this);
        tabsLayout.setOrientation(LinearLayout.VERTICAL);
        tabsLayout.setPadding(16, 16, 16, 16);

        for (int i = 0; i < tabs.size(); i++) {
            final int tabIndex = i;
            TabInfo tab = tabs.get(i);

            LinearLayout tabItem = new LinearLayout(this);
            tabItem.setOrientation(LinearLayout.HORIZONTAL);
            tabItem.setPadding(16, 16, 16, 16);
            tabItem.setBackgroundResource(android.R.drawable.list_selector_background);

            TextView tabTitle = new TextView(this);
            tabTitle.setText(tab.getTitle());
            tabTitle.setTextSize(16);
            tabTitle.setSingleLine();
            tabTitle.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            ImageView closeBtn = new ImageView(this);
            closeBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            closeBtn.setPadding(16, 0, 0, 0);
            closeBtn.setOnClickListener(v -> {
                closeTab(tabIndex);
            });

            tabItem.addView(tabTitle);
            tabItem.addView(closeBtn);
            tabItem.setOnClickListener(v -> {
                switchToTab(tabIndex);
            });

            tabsLayout.addView(tabItem);
        }

        // New tab button
        android.widget.Button newTabBtn = new android.widget.Button(this);
        newTabBtn.setText("+ New Tab");
        newTabBtn.setOnClickListener(v -> {
            createNewTab(isIncognitoMode);
            updateTabCount();
        });
        tabsLayout.addView(newTabBtn);

        builder.setView(tabsLayout);
        builder.setPositiveButton("Done", null);
        builder.show();
    }

    /**
     * Switch to a specific tab
     */
    private void switchToTab(int index) {
        if (index >= 0 && index < tabs.size()) {
            currentTabIndex = index;
            TabInfo tab = tabs.get(index);
            webView.loadUrl(tab.getUrl());
            updateTabCount();
        }
    }

    /**
     * Close a tab
     */
    private void closeTab(int index) {
        if (tabs.size() > 1) {
            tabs.remove(index);
            if (currentTabIndex >= tabs.size()) {
                currentTabIndex = tabs.size() - 1;
            }
            updateTabCount();
            TabInfo currentTab = tabs.get(currentTabIndex);
            if (!currentTab.getUrl().isEmpty()) {
                webView.loadUrl(currentTab.getUrl());
            }
        } else {
            Toast.makeText(this, "Cannot close the last tab", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Initialize WebView
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void initWeb() {
        // Override WebViewClient
        webView.setWebViewClient(new MkWebViewClient());
        // Override WebChromeClient
        webView.setWebChromeClient(new MkWebChromeClient());

        WebSettings settings = webView.getSettings();
        // Enable JavaScript
        settings.setJavaScriptEnabled(true);
        // Set browser UserAgent
        settings.setUserAgentString(settings.getUserAgentString() + " ZBrowser/" + getVerName(mContext));

        // Adjust images to fit WebView size
        settings.setUseWideViewPort(true);
        // Scale to screen size
        settings.setLoadWithOverviewMode(true);

        // Support zoom, default is true. Required for the next setting.
        settings.setSupportZoom(true);
        // Set built-in zoom controls. If false, WebView cannot be zoomed
        settings.setBuiltInZoomControls(true);
        // Hide native zoom controls
        settings.setDisplayZoomControls(false);

        // Cache
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        // Allow file access
        settings.setAllowFileAccess(true);
        // Support JS opening new windows
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        // Support automatic image loading
        settings.setLoadsImagesAutomatically(true);
        // Set default encoding
        settings.setDefaultTextEncodingName("utf-8");
        // Local storage
        settings.setDomStorageEnabled(true);
        settings.setPluginState(WebSettings.PluginState.ON);

        // Mixed content mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // Enable desktop mode if set in settings
        boolean desktopMode = prefs.getBoolean("desktopMode", false);
        settings.setUseWideViewPort(desktopMode);
        settings.setLoadWithOverviewMode(!desktopMode);

        // Setup download listener
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                handleDownload(url, contentDisposition, mimetype);
            }
        });

        // Load home page
        webView.loadUrl(getResources().getString(R.string.home_url));
    }

    /**
     * Handle file download
     */
    private void handleDownload(String url, String contentDisposition, String mimetype) {
        String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle(fileName);
        request.setDescription("Downloading via ZBrowser");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        downloadManager.enqueue(request);

        Toast.makeText(this, "Download started: " + fileName, Toast.LENGTH_SHORT).show();
    }

    /**
     * Override WebViewClient
     */
    private class MkWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // Open new page in current view instead of external browser

            if (url == null) {
                // Return true to handle ourselves, false to not handle
                return true;
            }

            // Normal content, load it
            if (url.startsWith(HTTP) || url.startsWith(HTTPS)) {
                view.loadUrl(url);
                return true;
            }

            // Call third-party apps, prevent crash (if phone doesn't have app to handle scheme, it may crash)
            // Show permission dialog before opening external app
            showExternalAppDialog(url);
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            // Page started loading, show progress bar
            progressBar.setProgress(0);
            progressBar.setVisibility(View.VISIBLE);

            // Update status text - show search query if searching
            if (isSearching && !searchQuery.isEmpty()) {
                textUrl.setText(searchQuery);
            } else {
                textUrl.setText("Loading...");
            }

            // Switch to default web icon
            webIcon.setImageResource(R.drawable.internet);

            // Update current tab info
            if (!tabs.isEmpty()) {
                tabs.get(currentTabIndex).setUrl(url);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            // Page finished loading, hide progress bar
            progressBar.setVisibility(View.INVISIBLE);

            // Change title
            setTitle(webView.getTitle());
            // Show page title
            textUrl.setText(webView.getTitle());

            // Update current tab info
            if (!tabs.isEmpty()) {
                TabInfo currentTab = tabs.get(currentTabIndex);
                currentTab.setTitle(webView.getTitle());
                currentTab.setUrl(url);
                currentTab.setFavicon(webView.getFavicon());
            }

            // Apply night mode if enabled
            if (isNightMode) {
                applyNightMode();
            }
        }

        @Override
        public void onPageCommitVisible(WebView view, String url) {
            super.onPageCommitVisible(view, url);
            // Update favicon
            if (!tabs.isEmpty()) {
                tabs.get(currentTabIndex).setFavicon(webView.getFavicon());
            }
        }
    }

    /**
     * Apply night mode to webpage
     */
    private void applyNightMode() {
        webView.evaluateJavascript(
            "(function() {" +
            "  var style = document.createElement('style');" +
            "  style.innerHTML = 'html { filter: invert(1) hue-rotate(180deg) !important; } img, video { filter: invert(1) hue-rotate(180deg) !important; }';" +
            "  document.head.appendChild(style);" +
            "})()", null);
    }

    /**
     * Override WebChromeClient
     */
    private class MkWebChromeClient extends WebChromeClient {
        private static final int WEB_PROGRESS_MAX = 100;

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);

            // Update progress bar with loading progress
            progressBar.setProgress(newProgress);
            if (newProgress > 0) {
                if (newProgress == WEB_PROGRESS_MAX) {
                    progressBar.setVisibility(View.INVISIBLE);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                }
            }
        }

        @Override
        public void onReceivedIcon(WebView view, Bitmap icon) {
            super.onReceivedIcon(view, icon);

            // Change icon
            webIcon.setImageBitmap(icon);

            // Update current tab favicon
            if (!tabs.isEmpty()) {
                tabs.get(currentTabIndex).setFavicon(icon);
            }
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);

            // Change title
            setTitle(title);
            // Show page title
            textUrl.setText(title);

            // Update current tab title
            if (!tabs.isEmpty()) {
                tabs.get(currentTabIndex).setTitle(title);
            }
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            if (customView != null) {
                callback.onCustomViewHidden();
                return;
            }
            customView = view;
            customViewCallback = callback;
            getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
            setContentView(customView);
        }

        @Override
        public void onHideCustomView() {
            if (customView == null) return;
            setContentView(findViewById(android.R.id.content).getRootView());
            customView = null;
            customViewCallback.onCustomViewHidden();
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(0);
        }
    }

    /**
     * Show dialog before opening external app
     */
    private void showExternalAppDialog(String url) {
        String appName = "this app";
        try {
            Uri uri = Uri.parse(url);
            if (uri.getScheme() != null) {
                appName = uri.getScheme() + " app";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        final String finalAppName = appName;
        new AlertDialog.Builder(this)
                .setTitle("Open External App?")
                .setMessage("Do you want to open " + finalAppName + " to handle this link?\n\n" + url)
                .setPositiveButton("Open", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(mContext, "Cannot open app: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Show options menu
     */
    private void showOptionsMenu() {
        PopupMenu popup = new PopupMenu(this, navSet);
        popup.getMenuInflater().inflate(R.menu.main_menu, popup.getMenu());

        // Add incognito toggle
        popup.getMenu().add(0, 100, 0, isIncognitoMode ? "Exit Incognito" : "New Incognito Tab");

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_new_tab) {
                createNewTab(false);
                Toast.makeText(this, "New tab created", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == 100) {
                // Incognito toggle
                if (isIncognitoMode) {
                    // Clear incognito data
                    CookieManager.getInstance().removeAllCookies(null);
                    CookieManager.getInstance().flush();
                    isIncognitoMode = false;
                    Toast.makeText(this, "Exited Incognito Mode", Toast.LENGTH_SHORT).show();
                } else {
                    createNewTab(true);
                    isIncognitoMode = true;
                    Toast.makeText(this, "Incognito Mode Enabled", Toast.LENGTH_SHORT).show();
                }
                updateTabCount();
                return true;
            } else if (id == R.id.action_find_in_page) {
                showFindInPageDialog();
                return true;
            } else if (id == R.id.action_desktop_mode) {
                toggleDesktopMode();
                return true;
            } else if (id == R.id.action_night_mode) {
                toggleNightMode();
                return true;
            } else if (id == R.id.action_add_bookmark) {
                addBookmark();
                return true;
            } else if (id == R.id.action_share) {
                sharePage();
                return true;
            } else if (id == R.id.action_copy_url) {
                copyUrl();
                return true;
            } else if (id == R.id.action_refresh) {
                webView.reload();
                return true;
            } else if (id == R.id.action_settings) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });

        popup.show();
    }

    /**
     * Find in page dialog
     */
    private void showFindInPageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Find in Page");

        EditText input = new EditText(this);
        input.setHint("Enter text to find");
        builder.setView(input);

        builder.setPositiveButton("Find", (dialog, which) -> {
            String text = input.getText().toString();
            if (!text.isEmpty()) {
                webView.findAllAsync(text);
                Toast.makeText(this, "Finding: " + text, Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Clear", (dialog, which) -> {
            webView.clearMatches();
        });

        builder.show();
    }

    /**
     * Toggle desktop mode
     */
    private void toggleDesktopMode() {
        boolean desktopMode = !prefs.getBoolean("desktopMode", false);
        prefs.edit().putBoolean("desktopMode", desktopMode).apply();

        WebSettings settings = webView.getSettings();
        if (desktopMode) {
            // Desktop User Agent
            String ua = settings.getUserAgentString();
            if (ua.contains("Mobile")) {
                ua = ua.replace("Mobile", "");
            }
            settings.setUserAgentString(ua);
            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(false);
            Toast.makeText(this, "Desktop Mode Enabled", Toast.LENGTH_SHORT).show();
        } else {
            // Mobile User Agent
            settings.setUserAgentString(settings.getUserAgentString() + " Mobile");
            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(true);
            Toast.makeText(this, "Desktop Mode Disabled", Toast.LENGTH_SHORT).show();
        }

        webView.reload();
    }

    /**
     * Toggle night mode
     */
    private void toggleNightMode() {
        isNightMode = !isNightMode;
        prefs.edit().putBoolean("darkMode", isNightMode).apply();

        if (isNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            applyNightMode();
            Toast.makeText(this, "Night Mode Enabled", Toast.LENGTH_SHORT).show();
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            webView.reload();
            Toast.makeText(this, "Night Mode Disabled", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Add bookmark
     */
    private void addBookmark() {
        String url = webView.getUrl();
        String title = webView.getTitle();

        if (url != null && !url.isEmpty()) {
            SharedPreferences bookmarks = getSharedPreferences("Bookmarks", MODE_PRIVATE);
            SharedPreferences.Editor editor = bookmarks.edit();
            String key = "bookmark_" + System.currentTimeMillis();
            editor.putString(key, title + "|" + url);
            editor.apply();

            Toast.makeText(this, "Bookmark added: " + title, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Share page
     */
    private void sharePage() {
        String url = webView.getUrl();
        if (url != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, url);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        }
    }

    /**
     * Copy URL
     */
    private void copyUrl() {
        String url = webView.getUrl();
        if (url != null) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("URL", url);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "URL copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handle back button
     */
    @Override
    public void onBackPressed() {
        // Check if in fullscreen
        if (customView != null) {
            webView.getWebChromeClient().onHideCustomView();
            return;
        }

        // Go back if possible
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            if ((System.currentTimeMillis() - exitTime) > PRESS_BACK_EXIT_GAP) {
                // Double tap to exit
                Toast.makeText(mContext, "Press again to exit",
                        Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        // Go or refresh
        if (id == R.id.btnStart) {
            if (textUrl.hasFocus()) {
                // Hide soft keyboard
                if (manager.isActive()) {
                    manager.hideSoftInputFromWindow(textUrl.getApplicationWindowToken(), 0);
                }

                // Address bar has focus, it's navigation
                String input = textUrl.getText().toString();
                if (!isHttpUrl(input)) {
                    // Not a URL, load with search engine
                    isSearching = true;
                    searchQuery = input;
                    try {
                        // URL encode
                        input = URLEncoder.encode(input, "utf-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    input = searchUrl + input;
                } else {
                    isSearching = false;
                    searchQuery = "";
                }
                webView.loadUrl(input);

                // Remove focus from address bar
                textUrl.clearFocus();
            } else {
                // Address bar has no focus, it's refresh
                webView.reload();
            }
        }
        // Back
        else if (id == R.id.goBack) {
            webView.goBack();
        }
        // Forward
        else if (id == R.id.goForward) {
            webView.goForward();
        }
        // Settings/Menu
        else if (id == R.id.navSet) {
            showOptionsMenu();
        }
        // Home
        else if (id == R.id.goHome) {
            isSearching = false;
            searchQuery = "";
            webView.loadUrl(getResources().getString(R.string.home_url));
        }
        // Tabs
        else if (id == R.id.btnTabs) {
            showTabsOverview();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            webView.getClass().getMethod("onPause").invoke(webView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            webView.getClass().getMethod("onResume").invoke(webView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }

    /**
     * Check if string is a URL
     *
     * @param urls String to check
     * @return true: is URL, false: is not URL
     */
    public static boolean isHttpUrl(String urls) {
        boolean isUrl;
        // URL regex pattern
        String regex = "(((https|http)?://)?([a-z0-9]+[.])|(www.))"
                + "\\w+[.|\\/]([a-z0-9]{0,})?[[.]([a-z0-9]{0,})]+((/[\\S&&[^,;\\u4E00-\\u9FA5]]+)+)?([.][a-z0-9]{0,}+|/?)";

        Pattern pat = Pattern.compile(regex.trim());
        Matcher mat = pat.matcher(urls.trim());
        isUrl = mat.matches();
        return isUrl;
    }

    /**
     * Get version name
     *
     * @param context Context
     * @return Current version name
     */
    private static String getVerName(Context context) {
        String verName = "unKnow";
        try {
            verName = context.getPackageManager().
                    getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return verName;
    }
}
