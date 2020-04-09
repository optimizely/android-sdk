package com.optimizely.ab.android.optimizely_debugger;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.optimizely.ab.android.datafile_handler.DefaultDatafileHandler;
import com.optimizely.ab.android.sdk.OptimizelyClient;
import com.optimizely.ab.android.sdk.OptimizelyManager;
import com.optimizely.ab.android.user_profile.DefaultUserProfileService;
import com.optimizely.ab.bucketing.UserProfileService;
import com.optimizely.ab.config.Experiment;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.optimizelyconfig.OptimizelyConfig;
import com.optimizely.ab.optimizelyconfig.OptimizelyExperiment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.xmlpull.v1.XmlPullParser.TYPES;

public class ForcedVariationsActivity extends AppCompatActivity {
    ListView listView;
    LinearLayout addView;
    FVCustomAdapter adapter;

    TextInputEditText userIdView;
    TextView experimentView;
    TextView variationView;

    String curUserId;
    String curExperimentKey;
    String curVariationKey;

    String[] experimentKeys;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.forced_variations_activity);
        setTitle("Forced Variations");
        listView = findViewById(R.id.listview);
        addView = findViewById(R.id.add_view);
        addView.setVisibility(View.INVISIBLE);
        userIdView = findViewById(R.id.user_id);
        experimentView = findViewById(R.id.experiment_key);
        variationView = findViewById(R.id.variation_key);

        refreshListView();

        OptimizelyManager optimizelyManager = OptimizelyDebugger.getInstance().getOptimizelyManager();
        OptimizelyClient client = optimizelyManager.getOptimizely();
        Map<String, OptimizelyExperiment> map = client.getOptimizelyConfig().getExperimentsMap();
        experimentKeys = map.keySet().toArray(new String[map.size()]);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.forced_variations_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.add) {
            showAddView();
        }

        return super.onOptionsItemSelected(item);
    }

    void refreshListView() {
        OptimizelyManager optimizelyManager = OptimizelyDebugger.getInstance().getOptimizelyManager();
        OptimizelyClient client = optimizelyManager.getOptimizely();

        ArrayList<ForcedVariation> items = new ArrayList();

        ConcurrentHashMap<String, ConcurrentHashMap<String, String>> map = client.forcedVariationMapping;
        for(String userId : map.keySet()) {
            Map<String, String> pairs = map.get(userId);
            for(String experimentKey : pairs.keySet()) {
                String variationKey = pairs.get(experimentKey);
                items.add(new ForcedVariation(userId, experimentKey, variationKey));
            }
        }

        if (adapter == null) {
            adapter = new FVCustomAdapter(getApplicationContext(), items);
            listView.setAdapter(adapter);
        } else {
            adapter.setItems(items);
            adapter.notifyDataSetChanged();
        }
    }

    public void showAddView() {
        addView.setVisibility(View.VISIBLE);
    }

    public void hideAddView(View view) {
        userIdView.setText(null);
        experimentView.setText(null);
        variationView.setText(null);

        addView.setVisibility(View.INVISIBLE);
    }

    public void saveForcedVariation(View view) {

    }

    public void removeForcedVariation(String userId, String experimentKey) {

    }

    public void selectExperiment(View view) {
        NumberPicker pickers = new NumberPicker(this);

        pickers.setMinValue(0);
        pickers.setMaxValue(experimentKeys.length - 1);
        pickers.setDisplayedValues(experimentKeys);
        //disable soft keyboard
        pickers.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        pickers.setWrapSelectorWheel(false);

        int selectedIndex = Arrays.asList(experimentKeys).indexOf(experimentView.getText());
        Log.d("OptimizelyDebugger", "selected: " + experimentView.getText() + "  " + selectedIndex);
        pickers.setValue(selectedIndex);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Experiment");
        builder.setMessage("Select an experiment key:");

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d("OptimizelyDebugger", "select experiment OK");

                int pos = pickers.getValue();
                curExperimentKey = experimentKeys[pos];
                experimentView.setText(curExperimentKey);
            }
        });

        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // close
            }
        });

        builder.setView(pickers).create().show();
    }

    public void selectVariation(View view) {
        Log.d("OptimizelyDebugger", "select variation");


    }

    // ForcedVariation

    class ForcedVariation {
        String userId;
        String experimentKey;
        String variationKey;

        public ForcedVariation(String userId, String experimentKey, String variationKey) {
            this.userId = userId;
            this.experimentKey = experimentKey;
            this.variationKey = variationKey;
        }
    }

    // ArrayAdapter

    class FVCustomAdapter extends BaseAdapter {
        Context context;
        ArrayList<ForcedVariation> items;
        LayoutInflater inflter;

        public FVCustomAdapter(Context context, ArrayList<ForcedVariation> items) {
            this.context = context;
            this.items = items;
            inflter = (LayoutInflater.from(context));
        }

        public void setItems(ArrayList<ForcedVariation> items) {
            this.items = items;
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
            ForcedVariation item = items.get(i);

            view = inflter.inflate(R.layout.debug_item, parent, false);
            ViewGroup.LayoutParams params = view.getLayoutParams();
            float factor = context.getResources().getDisplayMetrics().density;
            params.height = (int)(60.0 * factor);
            view.setLayoutParams(params);

            TextView titleView = view.findViewById(R.id.title);
            TextView valueView = view.findViewById(R.id.value);
            TextView arrowView = view.findViewById(R.id.arrow);

            titleView.setText(item.userId);
            valueView.setText(item.experimentKey + " -> " + item.variationKey);

            arrowView.setVisibility(View.INVISIBLE);
            view.setEnabled(false);

            return view;
        }

    }

}
