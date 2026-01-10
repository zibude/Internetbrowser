package com.zibuude.internetbrowser1;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.*;
import android.view.View;
import android.webkit.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private EditText urlBar;
    private WebView webView;
    private ProgressBar progressBar;
    private Button btnBack, btnForward, btnReload, btnTab;
    
    // プロキシIP（メニューから変更可能）
    private String dynamicProxyIp = "192.168.1.23"; 

    // タブ・ブックマーク管理
    private ArrayList tabHtmls = new ArrayList();
    private ArrayList tabUrls = new ArrayList();
    private ArrayList bookmarks = new ArrayList();
    private int currentTabIndex = 0;
    private final int MAX_TABS = 10;

    // メニューID
    private static final int MENU_SEARCH = 1;
    private static final int MENU_BOOKMARK = 2;
    private static final int MENU_SETTINGS = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI紐付け
        urlBar = (EditText) findViewById(R.id.url_bar);
        webView = (WebView) findViewById(R.id.webview);
        progressBar = (ProgressBar) findViewById(R.id.progress_horizontal);
        btnBack = (Button) findViewById(R.id.back_button);
        btnForward = (Button) findViewById(R.id.forward_button);
        btnReload = (Button) findViewById(R.id.reload_button);
        btnTab = (Button) findViewById(R.id.tab_button);
        Button go = (Button) findViewById(R.id.go_button);

        // WebView設定
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setLoadsImagesAutomatically(true);
        ws.setBuiltInZoomControls(true);
        try { ws.setPluginsEnabled(false); } catch (Exception e) {}

        // リスナー設定
        btnBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { if (webView.canGoBack()) webView.goBack(); }
        });
        btnForward.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { if (webView.canGoForward()) webView.goForward(); }
        });
        btnReload.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { executeProxyConnection(urlBar.getText().toString()); }
        });
        btnTab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showTabDialog(); }
        });
        go.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { executeProxyConnection(urlBar.getText().toString().trim()); }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // 【重要】localhostそのものへのリクエスト以外はすべてプロキシへ飛ばす
                if (url != null && url.startsWith("http") && !url.startsWith("http://localhost")) {
                    executeProxyConnection(url);
                    return true;
                }
                return false;
            }
        });

        // 初期タブ起動（Bing）
        tabUrls.add("https://www.bing.com");
        tabHtmls.add(""); 
        executeProxyConnection("https://www.bing.com");
    }

    // --- メニュー機能 ---
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        menu.add(0, MENU_SEARCH, 0, "Find in Page");
        menu.add(0, MENU_BOOKMARK, 0, "Bookmarks");
        menu.add(0, MENU_SETTINGS, 0, "Set Proxy IP");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SEARCH: showSearchDialog(); return true;
            case MENU_BOOKMARK: showBookmarkDialog(); return true;
            case MENU_SETTINGS: showIPSettingDialog(); return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // --- ダイアログ類 ---
    private void showTabDialog() {
        int tabCount = tabUrls.size();
        final String[] items = new String[tabCount + (tabCount < MAX_TABS ? 1 : 0)];
        for (int i = 0; i < tabCount; i++) {
            items[i] = (i == currentTabIndex ? "★ " : "") + "Tab " + (i + 1) + ": " + tabUrls.get(i);
        }
        if (tabCount < MAX_TABS) items[items.length - 1] = "[+] New Tab";

        new AlertDialog.Builder(this).setTitle("Tabs").setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (which < tabUrls.size()) switchTab(which);
                else createNewTab();
            }
        }).show();
    }

    private void createNewTab() {
        tabUrls.add("https://www.bing.com");
        tabHtmls.add("");
        currentTabIndex = tabUrls.size() - 1;
        executeProxyConnection("https://www.bing.com");
    }

    private void switchTab(int index) {
        currentTabIndex = index;
        String url = (String)tabUrls.get(index);
        String html = (String)tabHtmls.get(index);
        urlBar.setText(url);
        if (html == null || html.length() == 0) executeProxyConnection(url);
        else loadHtmlWithProxyBase(html, url);
    }

    private void showSearchDialog() {
        final EditText input = new EditText(this);
        new AlertDialog.Builder(this).setTitle("Find").setView(input).setPositiveButton("Go", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                webView.findAll(input.getText().toString());
            }
        }).show();
    }

    private void showBookmarkDialog() {
        String[] items = new String[bookmarks.size() + 1];
        for (int i = 0; i < bookmarks.size(); i++) items[i] = (String)bookmarks.get(i);
        items[items.length - 1] = "[+] Add Bookmark";

        new AlertDialog.Builder(this).setTitle("Bookmarks").setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (which < bookmarks.size()) executeProxyConnection((String)bookmarks.get(which));
                else { bookmarks.add(urlBar.getText().toString()); }
            }
        }).show();
    }

    private void showIPSettingDialog() {
        final EditText input = new EditText(this);
        input.setText(dynamicProxyIp);
        new AlertDialog.Builder(this).setTitle("Proxy IP").setView(input).setPositiveButton("Save", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dynamicProxyIp = input.getText().toString().trim();
            }
        }).show();
    }

    // --- HTML読み込みの共通処理 ---
    private void loadHtmlWithProxyBase(String html, String originalUrl) {
        // 【重要】BaseURLをプロキシサーバーのURLに設定することで、内部リンクの迷子を防ぐ
        String proxyBase = "http://" + dynamicProxyIp + ":8000/";
        webView.loadDataWithBaseURL(proxyBase, html, "text/html", "utf-8", originalUrl);
    }

    // --- 通信ロジック ---
    private void executeProxyConnection(final String targetUrl) {
        if (targetUrl == null || targetUrl.length() == 0 || targetUrl.equals("about:blank")) return;

        // すでにプロキシ経由のURLやlocalhostならそのまま
        String processedUrl = targetUrl;
        if (!targetUrl.startsWith("http://" + dynamicProxyIp) && !targetUrl.startsWith("http://localhost")) {
            if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
                if (targetUrl.contains(".") && !targetUrl.contains(" ")) {
                    processedUrl = "https://" + targetUrl;
                } else {
                    try { processedUrl = "https://www.bing.com/search?q=" + URLEncoder.encode(targetUrl, "UTF-8"); } 
                    catch (Exception e) { processedUrl = "https://www.bing.com/search?q=" + targetUrl; }
                }
            }
        }
        
        final String finalTarget = processedUrl;
        final String proxyUrl = (finalTarget.startsWith("http://" + dynamicProxyIp) || finalTarget.startsWith("http://localhost")) ? 
                               finalTarget : "http://" + dynamicProxyIp + ":8000/" + finalTarget;

        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(20);

        new Thread(new Runnable() {
            public void run() {
                try {
                    URL url = new URL(proxyUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(15000);
                    
                    InputStream in = conn.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line).append("\n");
                    br.close();
                    
                    final String html = sb.toString();

                    runOnUiThread(new Runnable() {
                        public void run() {
                            progressBar.setProgress(100);
                            progressBar.setVisibility(View.GONE);
                            
                            String displayUrl = finalTarget;
                            // 表示用URLからプロキシ部分を削る
                            if (displayUrl.startsWith("http://" + dynamicProxyIp + ":8000/")) {
                                displayUrl = displayUrl.substring(("http://" + dynamicProxyIp + ":8000/").length());
                            }
                            urlBar.setText(displayUrl);

                            if (currentTabIndex < tabUrls.size()) {
                                tabUrls.set(currentTabIndex, displayUrl);
                                tabHtmls.set(currentTabIndex, html);
                            }
                            loadHtmlWithProxyBase(html, displayUrl);
                        }
                    });
                } catch (final Exception e) {
                    final String msg = e.getMessage();
                    runOnUiThread(new Runnable() {
                        public void run() { 
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, "Error: " + msg, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }
}
