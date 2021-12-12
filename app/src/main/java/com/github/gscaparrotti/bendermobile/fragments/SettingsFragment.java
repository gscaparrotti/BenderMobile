package com.github.gscaparrotti.bendermobile.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.github.gscaparrotti.bendermobile.R;
import com.github.gscaparrotti.bendermobile.activities.MainActivity;

public class SettingsFragment extends Fragment {

    public SettingsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_settings, container, false);
        final TextView currentIP = view.findViewById(R.id.currentIPView);
        currentIP.setText("Current IP: " + MainActivity.commonContext.getSharedPreferences("BenderIP", 0).getString("BenderIP", "Absent"));
        view.findViewById(R.id.IPOkButton).setOnClickListener(v -> {
            EditText ipEditText = view.findViewById(R.id.IPeditText);
            String ip = ipEditText.getText().toString();
            if (SettingsFragment.validIP(ip)) {
                SharedPreferences.Editor editor = MainActivity.commonContext.getSharedPreferences("BenderIP", 0).edit();
                editor.putString("BenderIP", ip);
                editor.commit();
                currentIP.setText("Current IP: " + MainActivity.commonContext.getSharedPreferences("BenderIP", 0).getString("BenderIP", "Absent"));
            } else {
                Toast.makeText(MainActivity.commonContext, "Invalid IP", Toast.LENGTH_SHORT).show();
            }
        });
        return view;
    }

    private static boolean validIP(String ip) {
        try {
            if (ip == null || ip.isEmpty()) {
                return false;
            }
            String[] parts = ip.split("\\.");
            if (parts.length != 4) {
                return false;
            }
            for (String s : parts) {
                int i = Integer.parseInt(s);
                if ((i < 0) || (i > 255)) {
                    return false;
                }
            }
            if (ip.endsWith(".")) {
                return false;
            }
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }
}
