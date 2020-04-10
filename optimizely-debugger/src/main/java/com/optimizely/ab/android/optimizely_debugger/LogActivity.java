package com.optimizely.ab.android.optimizely_debugger;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class LogActivity extends AppCompatActivity {
    private ListView listView;
    private RadioGroup levelView;
    private LogCustomAdapter adapter;
    private EditText keywordView;

    private int selectedLogLevel = 2;  // info
    private String keyword;

    private ArrayList<String> logs = new ArrayList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.log_activity);
        setTitle("Logs");

        listView = findViewById(R.id.listview);
        levelView = findViewById(R.id.level_group);
        keywordView = findViewById(R.id.keyword);

        ((RadioButton)levelView.getChildAt(selectedLogLevel)).setChecked(true);
        levelView.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                View radioButton = levelView.findViewById(checkedId);
                selectedLogLevel = levelView.indexOfChild(radioButton);
                refreshListView();
            }
        });

        keywordView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                    keyword = keywordView.getText().toString();
                    refreshListView();

                    // close soft keyboard

                    InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(listView.getRootView().getWindowToken(), 0);

                    return true;
                }

                return false;
            }
        });

        refreshListView();
    }

    ArrayList<String> readDeviceLogs() {
        ArrayList<String> items = new ArrayList();

        try {
            String filterTag = "*";

            String filterLevel;
            switch(selectedLogLevel) {
                case 0: filterLevel = "E"; break;
                case 1: filterLevel = "W"; break;
                case 2: filterLevel = "I"; break;
                default: filterLevel = "D";
            }

            if(TextUtils.isEmpty(keyword)) keyword = "/";   // filer out bogus message with no tags

            String command = "logcat -d -v tag -t 1000 " + filterTag + ":" + filterLevel + " | grep -i " + keyword;

            Log.d("OptimizelyDebugger", command);

            String[] shellCommand = {
                    "/bin/sh",
                    "-c",
                    command
            };

            Process process = Runtime.getRuntime().exec(shellCommand);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                items.add(line);
            }
        } catch (IOException e) {
            Log.d("OptimizelyDebugger", "failed to read device logs");
        }
        return items;
    }

    void clearLogs() {
        new AlertDialog.Builder(LogActivity.this)
                .setMessage("Do you want to clear device logs?")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Runtime.getRuntime().exec("logcat -c");
                            refreshListView();
                        } catch (IOException e) {
                            Log.d("OptimizelyDebugger", "failed to clear device logs");
                        }
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

    // ActionBar menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.log_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.delete) {
            clearLogs();
        }

        return super.onOptionsItemSelected(item);
    }

    // ListView

    void refreshListView() {
        logs = readDeviceLogs();

        if (adapter == null) {
            adapter = new LogCustomAdapter(getApplicationContext(), logs);
            listView.setAdapter(adapter);
        } else {
            adapter.setItems(logs);
            adapter.notifyDataSetChanged();
        }
    }

    // ArrayAdapter

    class LogCustomAdapter extends BaseAdapter {
        Context context;
        ArrayList<String> items;
        LayoutInflater inflter;

        public LogCustomAdapter(Context context, ArrayList<String> items) {
            this.context = context;
            this.items = items;
            inflter = (LayoutInflater.from(context));
        }

        public void setItems(ArrayList<String> items) {
            this.items = items;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public String getItem(int i) {
            return items.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup parent) {
            String item = getItem(i);

            view = inflter.inflate(R.layout.log_item, parent, false);
            ViewGroup.LayoutParams params = view.getLayoutParams();
            float factor = context.getResources().getDisplayMetrics().density;
            params.height = (int) (60.0 * factor);
            view.setLayoutParams(params);

            TextView messageView = view.findViewById(R.id.message);
            messageView.setText(ellipsis(item, 128));

            return view;
        }

    }

    // Utils

    String ellipsis(String text, int length) {
        if(text.length() <= length) return text;
        else return text.substring(0, length - 3) + "...";
    }

}
