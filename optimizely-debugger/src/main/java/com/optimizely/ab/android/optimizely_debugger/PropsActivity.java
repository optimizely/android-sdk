package com.optimizely.ab.android.optimizely_debugger;

import android.bluetooth.BluetoothAssignedNumbers;
import android.content.Context;
import android.content.Intent;
import android.graphics.Path;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.optimizely.ab.android.sdk.OptimizelyClient;
import com.optimizely.ab.android.sdk.OptimizelyManager;
import com.optimizely.ab.optimizelyconfig.OptimizelyConfig;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PropsActivity extends AppCompatActivity {
    Map<String, Object> props;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String title = getIntent().getExtras().getString("title");

        Object propObject = null;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            propObject = ((ObjectWrapperForBinder)getIntent().getExtras().getBinder("props")).getData();
        }
        props = makeProps(propObject);

        setContentView(R.layout.props_activity);
        setTitle(title);

        ListView listview = findViewById(R.id.listview);
        PropsCustomAdapter customAdapter = new PropsCustomAdapter(getApplicationContext(), props);
        listview.setAdapter(customAdapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String key = customAdapter.getItemKey(position);
                Object item = customAdapter.getItem(position);

                if (isValueType((item))) {
                    return;
                }

                Intent intent = new Intent(PropsActivity.this, PropsActivity.class);
                intent.putExtra("title", key);

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    Bundle bundle = new Bundle();
                    bundle.putBinder("props", new ObjectWrapperForBinder(item));
                    intent.putExtras(bundle);
                }

                PropsActivity.this.startActivity(intent);
            }
        });
    }

    // Props

    Map<String, Object> makeProps(Object propObject) {
        Map<String, Object> map = new HashMap<String, Object>();

        if (propObject instanceof Map) {
            map = (Map<String, Object>)propObject;
        } else {
            Log.d("OptimizelyDebugger", "optimizely config received");

            for(Field field : propObject.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    map.put(field.getName(), field.get(propObject));
                } catch (IllegalAccessException e) {
                    Log.d("OptimizelyDebugger", "props get object error: " + field.getName());
                }
            }
        }

        return map;
    }

    boolean isValueType(Object object) {
        return object instanceof String || object instanceof Boolean;
    }

    // ArrayAdapter

    class PropsCustomAdapter extends BaseAdapter {
        Context context;
        Map<String, Object> map;
        LayoutInflater inflter;

        public PropsCustomAdapter(Context context, Map<String, Object> map) {
            this.context = context;
            this.map = map;
            inflter = (LayoutInflater.from(context));
        }

        @Override
        public int getCount() {
            return map.size();
        }

        @Override
        public Object getItem(int i) {
            String key = getItemKey(i);
            return map.get(key);
        }

        public String getItemKey(int i) {
            return (String) map.keySet().toArray()[i];
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup parent) {
            String key = getItemKey(i);
            Object item = getItem(i);

            view = inflter.inflate(R.layout.debug_item, parent, false);
            ViewGroup.LayoutParams params = view.getLayoutParams();
            float factor = context.getResources().getDisplayMetrics().density;
            params.height = (int)(60.0 * factor);
            view.setLayoutParams(params);

            TextView titleView = view.findViewById(R.id.title);
            TextView valueView = view.findViewById(R.id.value);
            TextView arrowView = view.findViewById(R.id.arrow);

            titleView.setText(key);
            if (isValueType((item))) {
                valueView.setText(item.toString());

                arrowView.setVisibility(View.INVISIBLE);
                view.setEnabled(false);
            }

            return view;
        }

    }

}
