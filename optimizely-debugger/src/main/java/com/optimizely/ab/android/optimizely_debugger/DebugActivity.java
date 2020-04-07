package com.optimizely.ab.android.optimizely_debugger;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optimizely.ab.android.sdk.OptimizelyClient;
import com.optimizely.ab.android.sdk.OptimizelyManager;
import com.optimizely.ab.optimizelyconfig.OptimizelyConfig;

import java.util.ArrayList;

public class DebugActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.debug_activity);
        setTitle("Optimizely Debugger");

        ArrayList<DebugEntry> items = makeTableEntries();

        ListView listview = findViewById(R.id.listview);
        DebugCustomAdapter customAdapter = new DebugCustomAdapter(getApplicationContext(), items);
        listview.setAdapter(customAdapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DebugEntry item = (DebugEntry)items.get(position);
                if(item.action != null) item.action.run();
            }
        });
    }

    ArrayList<DebugEntry> makeTableEntries() {
        OptimizelyManager optimizelyManager = OptimizelyDebugger.getInstance().getOptimizelyManager();

        ArrayList items = new ArrayList<DebugEntry>();
        items.add(new DebugEntry("SDK Key", optimizelyManager.getSdkKey(), null));
        items.add(new DebugEntry("ProjectConfig", null, () -> {
            Intent intent = new Intent(DebugActivity.this, PropsActivity.class);
            intent.putExtra("title", "ProjectConfig");

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                OptimizelyConfig config = optimizelyManager.getOptimizely().getOptimizelyConfig();
                Bundle bundle = new Bundle();
                bundle.putBinder("props", new ObjectWrapperForBinder(config));
                intent.putExtras(bundle);
            }

            DebugActivity.this.startActivity(intent);
        }));
        items.add(new DebugEntry("Logs", null, () -> {
            Intent intent = new Intent(DebugActivity.this, LogActivity.class);
            DebugActivity.this.startActivity(intent);
        }));
        items.add(new DebugEntry("Forced Variations", null, () -> {
            Intent intent = new Intent(DebugActivity.this, ForcedVariationsActivity.class);
            DebugActivity.this.startActivity(intent);
        }));

        return items;
    }

    // Table Entry

    class DebugEntry {
        String title;
        String value;
        Runnable action;

        DebugEntry(String title, String value, Runnable action) {
            this.title = title;
            this.value = value;
            this.action = action;
        }
    }

    // ArrayAdapter

    class DebugCustomAdapter extends BaseAdapter {
        Context context;
        ArrayList<DebugEntry> items;
        LayoutInflater inflter;

        public DebugCustomAdapter(Context context, ArrayList<DebugEntry> items) {
            this.context = context;
            this.items = items;
            inflter = (LayoutInflater.from(context));
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup parent) {
            DebugEntry item = items.get(i);

            view = inflter.inflate(R.layout.debug_item, parent, false);
            ViewGroup.LayoutParams params = view.getLayoutParams();
            float factor = context.getResources().getDisplayMetrics().density;
            params.height = (int)(60.0 * factor);
            view.setLayoutParams(params);

            TextView titleView = view.findViewById(R.id.title);
            TextView valueView = view.findViewById(R.id.value);
            TextView arrowView = view.findViewById(R.id.arrow);

            titleView.setText(item.title);
            valueView.setText(item.value);

            if(item.action == null){
                arrowView.setVisibility(View.INVISIBLE);
                view.setEnabled(false);
            }

            return view;
        }

    }

}
