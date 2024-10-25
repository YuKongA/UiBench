/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.test.uibench;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.ListFragment;

import com.android.test.uibench.listview.CompatListActivity;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends CompatListActivity {
    private static final String EXTRA_PATH = "activity_path";
    private static final String CATEGORY_HWUI_TEST = "com.android.test.uibench.TEST";
    private final static Comparator<Map<String, Object>> sDisplayNameComparator =
            new Comparator<Map<String, Object>>() {
                private final Collator collator = Collator.getInstance();

                public int compare(Map<String, Object> map1, Map<String, Object> map2) {
                    return collator.compare(map1.get("title"), map2.get("title"));
                }
            };
    private String mActivityPath = "";

    @Override
    protected void initializeActivity() {
        Intent intent = getIntent();
        String path = intent.getStringExtra(EXTRA_PATH);

        if (path == null) {
            path = "";
        } else {
            // not root level, display where we are in the hierarchy
            setTitle(path);
        }
        mActivityPath = path;
    }

    @Override
    protected ListAdapter createListAdapter() {
        return new SimpleAdapter(this, getData(mActivityPath),
                android.R.layout.simple_list_item_1, new String[]{"title"},
                new int[]{android.R.id.text1});
    }

    @Override
    protected ListFragment createListFragment() {
        return new TestListFragment();
    }

    protected List<Map<String, Object>> getData(String prefix) {
        List<Map<String, Object>> myData = new ArrayList<>();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(CATEGORY_HWUI_TEST);

        PackageManager pm = getPackageManager();
        @SuppressLint("QueryPermissionsNeeded") List<ResolveInfo> list = pm.queryIntentActivities(mainIntent, 0);

        String[] prefixPath;
        String prefixWithSlash = prefix;

        if (prefix.isEmpty()) {
            prefixPath = null;
        } else {
            prefixPath = prefix.split("/");
            prefixWithSlash = prefix + "/";
        }

        int len = list.size();

        Map<String, Boolean> entries = new HashMap<>();

        for (int i = 0; i < len; i++) {
            ResolveInfo info = list.get(i);
            CharSequence labelSeq = info.loadLabel(pm);
            String label = labelSeq.toString();

            if (prefixWithSlash.isEmpty() || label.startsWith(prefixWithSlash)) {

                String[] labelPath = label.split("/");

                String nextLabel = prefixPath == null ? labelPath[0] : labelPath[prefixPath.length];

                if ((prefixPath != null ? prefixPath.length : 0) == labelPath.length - 1) {
                    addItem(myData, nextLabel, activityIntent(
                            info.activityInfo.applicationInfo.packageName,
                            info.activityInfo.name));
                } else {
                    if (entries.get(nextLabel) == null) {
                        addItem(myData, nextLabel, browseIntent(prefix.isEmpty() ?
                                nextLabel : prefix + "/" + nextLabel));
                        entries.put(nextLabel, true);
                    }
                }
            }
        }

        myData.sort(sDisplayNameComparator);

        return myData;
    }

    protected Intent activityIntent(String pkg, String componentName) {
        Intent result = new Intent();
        result.setClassName(pkg, componentName);
        return result;
    }

    protected Intent browseIntent(String path) {
        Intent result = new Intent();
        result.setClass(this, MainActivity.class);
        result.putExtra(EXTRA_PATH, path);
        return result;
    }

    protected void addItem(List<Map<String, Object>> data, String name, Intent intent) {
        Map<String, Object> temp = new HashMap<>();
        temp.put("title", name);
        temp.put("intent", intent);
        data.add(temp);
    }

    public static class TestListFragment extends ListFragment {
        @Override
        @SuppressWarnings("unchecked")
        public void onListItemClick(ListView l, View v, int position, long id) {
            Map<String, Object> map = (Map<String, Object>) l.getItemAtPosition(position);

            Intent intent = (Intent) map.get("intent");
            startActivity(intent);
        }

        @Override
        public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            getListView().setTextFilterEnabled(true);
        }
    }
}
