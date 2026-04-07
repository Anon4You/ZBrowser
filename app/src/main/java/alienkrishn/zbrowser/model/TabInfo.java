package alienkrishn.zbrowser.model;

import android.graphics.Bitmap;

/**
 * Tab information model
 */
public class TabInfo {
    private String title;
    private String url;
    private Bitmap favicon;
    private boolean isIncognito;

    public TabInfo() {
        this.title = "New Tab";
        this.url = "";
        this.isIncognito = false;
    }

    public TabInfo(String title, String url, boolean isIncognito) {
        this.title = title;
        this.url = url;
        this.isIncognito = isIncognito;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Bitmap getFavicon() {
        return favicon;
    }

    public void setFavicon(Bitmap favicon) {
        this.favicon = favicon;
    }

    public boolean isIncognito() {
        return isIncognito;
    }

    public void setIncognito(boolean incognito) {
        isIncognito = incognito;
    }
}
