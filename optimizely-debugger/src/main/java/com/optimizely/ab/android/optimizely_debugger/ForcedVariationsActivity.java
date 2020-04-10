package com.optimizely.ab.android.optimizely_debugger;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.optimizely.ab.android.sdk.OptimizelyClient;
import com.optimizely.ab.android.sdk.OptimizelyManager;
import com.optimizely.ab.optimizelyconfig.OptimizelyExperiment;
import com.optimizely.ab.optimizelyconfig.OptimizelyVariation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ForcedVariationsActivity extends AppCompatActivity {
    ListView listView;
    LinearLayout addView;
    FVCustomAdapter adapter;

    TextInputEditText userIdView;
    TextView experimentView;
    TextView variationView;

    String selectedExperimentKey;
    String[] experimentKeys = {};
    String[] variationKeys = {};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.forced_variations_activity);
        setTitle("Forced Variations");
        listView = findViewById(R.id.listview);
        addView = findViewById(R.id.add_view);
        userIdView = findViewById(R.id.user_id);
        experimentView = findViewById(R.id.experiment_key);
        variationView = findViewById(R.id.variation_key);

        hideAddView();
        refreshListView();

        experimentKeys = getAllExperimentKeys();
    }

    // ActionBar menu

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

    // ListView

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
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    new AlertDialog.Builder(ForcedVariationsActivity.this)
                            .setMessage("Do you want to remove this forced-variation item?")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ForcedVariation item = (ForcedVariation) listView.getAdapter().getItem(position);
                                    removeForcedVariation(item.experimentKey, item.userId);
                                    refreshListView();
                                }
                            })
                            .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // close
                                }
                            })
                            .create().show();
                }
            });
        } else {
            adapter.setItems(items);
            adapter.notifyDataSetChanged();
        }
    }

    public void onSaveClicked(View view) {
        saveForcedVariation();
    }

    public void onCancelClicked(View view) {
        hideAddView();
    }

    public void showAddView() {
        addView.setVisibility(View.VISIBLE);
    }

    public void hideAddView() {
        userIdView.setText(null);
        experimentView.setText(null);
        variationView.setText(null);

        addView.setVisibility(View.GONE);

        // close soft keyboard

        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(listView.getRootView().getWindowToken(), 0);
    }

    public void saveForcedVariation() {
        String userId = userIdView.getText().toString();
        String experimentKey = experimentView.getText().toString();
        String variationKey = variationView.getText().toString();

        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(experimentKey) || TextUtils.isEmpty(variationKey)) {
            new AlertDialog.Builder(this)
                    .setMessage("Invalid data for forced variation setting. Try again!")
                    .setPositiveButton("DISMISS", null)
                    .create().show();
            return;
        }

        updateForcedVariation(experimentKey, userId, variationKey);

        hideAddView();
        refreshListView();
    }

    // Experiment + Variation Pickers

    public void selectExperiment(View view) {
        String[] items = experimentKeys;
        TextView textView = experimentView;

        NumberPicker pickers = new NumberPicker(this);

        pickers.setMinValue(0);
        pickers.setMaxValue(items.length - 1);
        pickers.setDisplayedValues(items);

        //disable soft keyboard
        pickers.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        pickers.setWrapSelectorWheel(false);

        int selectedIndex = Arrays.asList(items).indexOf(textView.getText());
        pickers.setValue(selectedIndex);

        new AlertDialog.Builder(this)
                .setMessage("Select an experiment key:")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int pos = pickers.getValue();
                        selectedExperimentKey = experimentKeys[pos];
                        textView.setText(selectedExperimentKey);

                        variationKeys = getAllVariationKeysForExperiment(selectedExperimentKey);
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // close
                    }
                })
                .setView(pickers)
                .create().show();
    }

    public void selectVariation(View view) {
        if (selectedExperimentKey == null) {
            new AlertDialog.Builder(this)
                    .setMessage("Select an experiment key first and try again!")
                    .setPositiveButton("DISMISS", null)
                    .create().show();
            return;
        }

        String[] items = variationKeys;
        TextView textView = variationView;

        NumberPicker pickers = new NumberPicker(this);

        pickers.setMinValue(0);
        pickers.setMaxValue(items.length - 1);
        pickers.setDisplayedValues(items);

        //disable soft keyboard
        pickers.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        pickers.setWrapSelectorWheel(false);

        int selectedIndex = Arrays.asList(items).indexOf(textView.getText());
        pickers.setValue(selectedIndex);

        new AlertDialog.Builder(this)
                .setMessage("Select a variation key:")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int pos = pickers.getValue();
                        String variationKey = variationKeys[pos];
                        textView.setText(variationKey);
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // close
                    }
                })
                .setView(pickers)
                .create().show();
    }

    // Project Config

    String[] getAllExperimentKeys() {
        OptimizelyManager optimizelyManager = OptimizelyDebugger.getInstance().getOptimizelyManager();
        OptimizelyClient client = optimizelyManager.getOptimizely();
        Map<String, OptimizelyExperiment> map = client.getOptimizelyConfig().getExperimentsMap();
        return map.keySet().toArray(new String[map.size()]);
    }

    String[] getAllVariationKeysForExperiment(String experimentKey) {
        OptimizelyManager optimizelyManager = OptimizelyDebugger.getInstance().getOptimizelyManager();
        OptimizelyClient client = optimizelyManager.getOptimizely();
        OptimizelyExperiment experiment = client.getOptimizelyConfig().getExperimentsMap().get(experimentKey);
        Map<String, OptimizelyVariation> map = experiment.getVariationsMap();
        return map.keySet().toArray(new String[map.size()]);
    }

    public void updateForcedVariation(String experimentKey, String userId, @Nullable String variationKey) {
        OptimizelyManager optimizelyManager = OptimizelyDebugger.getInstance().getOptimizelyManager();
        OptimizelyClient client = optimizelyManager.getOptimizely();
        client.setForcedVariation(experimentKey, userId, variationKey);
    }

    public void removeForcedVariation(String experimentKey, String userId) {
        updateForcedVariation(experimentKey, userId, null);
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
        public ForcedVariation getItem(int i) {
            return items.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup parent) {
            ForcedVariation item = getItem(i);

            view = inflter.inflate(R.layout.forced_variation_item, parent, false);
            ViewGroup.LayoutParams params = view.getLayoutParams();
            float factor = context.getResources().getDisplayMetrics().density;
            params.height = (int)(60.0 * factor);
            view.setLayoutParams(params);

            TextView titleView = view.findViewById(R.id.title);
            TextView valueView = view.findViewById(R.id.message);
            ImageView iconView = view.findViewById(R.id.delete);

            titleView.setText(item.userId);
            valueView.setText(item.experimentKey + " -> " + item.variationKey);
            iconView.setImageResource(android.R.drawable.ic_menu_delete);

            return view;
        }

    }

}
