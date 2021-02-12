/*
 * This file is part of PCAPdroid.
 *
 * PCAPdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PCAPdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PCAPdroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2020 - Emanuele Faranda
 */

package com.emanuelef.remote_capture;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ConnectionsFragment extends Fragment implements AppStateListener, ConnectionsListener {
    private static final String TAG = "ConnectionsFragment";
    private MainActivity mActivity;
    private Handler mHandler;
    private ConnectionsAdapter mAdapter;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        mActivity = (MainActivity) context;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mActivity.removeAppStateListener(this);

        ConnectionsRegister reg = CaptureService.getConnsRegister();
        if(reg != null)
            reg.setListener(null);

        mActivity = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.connections, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ListView connList = view.findViewById(R.id.connections_view);
        TextView emptyText = view.findViewById(R.id.no_connections);
        connList.setEmptyView(emptyText);

        mAdapter = new ConnectionsAdapter(mActivity);
        connList.setAdapter(mAdapter);
        connList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
                ConnDescriptor item = (ConnDescriptor) adapterView.getItemAtPosition(pos);

                if(item != null) {
                    Intent intent = new Intent(getContext(), ConnectionDetails.class);
                    AppDescriptor app = mActivity.findAppByUid(item.uid);
                    String app_name = null;//;1051

                    if(app != null)
                        app_name = app.getName();
                    else if(item.uid == 1000)
                        app_name = "system";
                    else if(item.uid == 1051)
                        app_name = "netd";

                    intent.putExtra(ConnectionDetails.CONN_EXTRA_KEY, item);

                    if(app_name != null)
                        intent.putExtra(ConnectionDetails.APP_NAME_EXTRA_KEY, app_name);

                    startActivity(intent);
                }
            }
        });

        mHandler = new Handler();
        ConnectionsRegister reg = CaptureService.getConnsRegister();
        if(reg != null)
            reg.setListener(this);

        mActivity.addAppStateListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        CaptureService.askConnectionsDump();
    }

    @Override
    public void appStateChanged(AppState state) {
        if(state == AppState.running) {
            ConnectionsRegister reg = CaptureService.getConnsRegister();

            if(reg != null)
                reg.setListener(this);
        }
    }

    @Override
    public void appsLoaded() {
        // Refresh the adapter to load the apps
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void connectionsChanges() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
            }
        });
    }
}
