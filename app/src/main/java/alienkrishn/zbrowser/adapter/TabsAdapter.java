package alienkrishn.zbrowser.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import alienkrishn.zbrowser.model.TabInfo;

/**
 * Adapter for tabs list
 */
public class TabsAdapter extends BaseAdapter {

    private List<TabInfo> tabs;

    public TabsAdapter(List<TabInfo> tabs) {
        this.tabs = tabs;
    }

    @Override
    public int getCount() {
        return tabs.size();
    }

    @Override
    public Object getItem(int position) {
        return tabs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = new TextView(parent.getContext());
        }

        TextView textView = (TextView) convertView;
        TabInfo tab = tabs.get(position);
        textView.setText(tab.getTitle());
        textView.setPadding(16, 16, 16, 16);

        return textView;
    }
}
