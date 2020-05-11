package com.android.gscaparrotti.bendermobile.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.gscaparrotti.bendermobile.R;
import com.android.gscaparrotti.bendermobile.activities.MainActivity;
import com.android.gscaparrotti.bendermobile.network.HttpServerInteractor;
import com.android.gscaparrotti.bendermobile.network.HttpServerInteractor.Method;
import com.android.gscaparrotti.bendermobile.utilities.BenderAsyncTaskResult;
import com.android.gscaparrotti.bendermobile.utilities.BenderAsyncTaskResult.Empty;
import com.android.gscaparrotti.bendermobile.utilities.FragmentNetworkingBenderAsyncTask;
import com.google.gson.JsonArray;
import java.util.HashMap;
import java.util.Map;
import java9.util.stream.Collectors;

import static com.android.gscaparrotti.bendermobile.utilities.StreamUtils.stream;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnMainFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MainFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainFragment extends Fragment {

    private static HttpServerInteractor http = HttpServerInteractor.getInstance();
    private TableAdapter ta;
    private int tablesCount = 0;
    private Map<Integer, String> names = new HashMap<>();

    private OnMainFragmentInteractionListener mListener;

    public MainFragment() {
    }

    public static MainFragment newInstance() {
        MainFragment fragment = new MainFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        GridView gv = (GridView) view.findViewById(R.id.tablesContainer);
        ta = new TableAdapter(getActivity());
        gv.setAdapter(ta);
        new TableAmountDownloader(MainFragment.this).execute();
        view.findViewById(R.id.mainUpdate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TableAmountDownloader(MainFragment.this).execute();
            }
        });
        view.findViewById(R.id.allPending).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onTablePressedEventFired(0);
            }
        });
        return view;
    }

    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        if (context instanceof OnMainFragmentInteractionListener) {
            mListener = (OnMainFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnMainFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    private void tableAdded(final int tableNumber, final Map<Integer, String> names) {
        reset();
        for (int i = 0; i < tableNumber; i++) {
            addElement(i + 1, names.get(i + 1));
        }
        ta.notifyDataSetChanged();
    }

    private void addElement(final Integer i, final String name) {
        tablesCount++;
        names.put(i, name);
    }

    private void reset() {
        tablesCount = 0;
        names.clear();
    }

    public interface OnMainFragmentInteractionListener {
        void onTablePressedEventFired(int tableNumber);
    }

    private class TableAdapter extends BaseAdapter {

        private LayoutInflater inflater;

        TableAdapter(Context context) {
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return tablesCount;
        }

        @Override
        public Integer getItem(int position) {
            return position + 1;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_table, parent, false);
            }
            final Integer table = getItem(position);
            final TextView tableView = (TextView) convertView.findViewById(R.id.table);
            tableView.setText(getString(R.string.itemTableText) + table + formattedName(names.get(table)));
            convertView.setLongClickable(true);
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onTablePressedEventFired(table);
                }
            });
            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(getString(R.string.ResetConfirmDialogTitle))
                            .setMessage(R.string.ResetConfirmDialogQuestion)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    new TableResetRequestUploader(MainFragment.this).execute(table);
                                    new TableAmountDownloader(MainFragment.this).execute();
                                }})
                            .setNegativeButton(android.R.string.no, null).show();
                    return true;
                }
            });
            return convertView;
        }

        private String formattedName(final String name) {
            return !(name == null) ? " - " + name : "";
        }
    }

    private final class TableResetRequestUploader extends FragmentNetworkingBenderAsyncTask<Integer, Empty> {

        TableResetRequestUploader(Fragment fragment) {
            super(fragment);
        }

        @Override
        protected BenderAsyncTaskResult<Empty> innerDoInBackground(Integer[] objects) {
            final JsonArray orders = http.sendAndReceiveAsJsonArray(ip, 8080, "orders?tableNumber=" + objects[0], Method.GET, null);
            stream(orders)
                .map(e -> e.getAsJsonObject().get("id").getAsLong())
                .forEach(id -> http.sendAndReceiveAsString(ip, 8080, "orders/" + id, Method.DELETE, null));
            return new BenderAsyncTaskResult<>(BenderAsyncTaskResult.EMPTY_RESULT);
        }

        @Override
        protected void innerOnSuccessfulPostExecute(BenderAsyncTaskResult<Empty> result) {
            Toast.makeText(MainActivity.commonContext, getString(R.string.ResetSuccess), Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void innerOnUnsuccessfulPostExecute(BenderAsyncTaskResult<Empty> error) {
            Toast.makeText(MainActivity.commonContext, error.getError().getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private final class TableAmountDownloader extends FragmentNetworkingBenderAsyncTask<Empty, Pair<Integer, Map<Integer, String>>> {

        TableAmountDownloader(Fragment fragment) {
            super(fragment);
        }

        @Override
        protected BenderAsyncTaskResult<Pair<Integer, Map<Integer, String>>> innerDoInBackground(Empty[] objects) {
            final JsonArray receivedAmount = http.sendAndReceiveAsJsonArray(ip, 8080, "tables", Method.GET, null);
            final int amount = stream(receivedAmount)
                .map(e -> e.getAsJsonObject().get("tableNumber").getAsInt())
                .max(Integer::compare)
                .orElse(0);
            final JsonArray receivedNames = http.sendAndReceiveAsJsonArray(ip, 8080, "customers", Method.GET, null);
            final Map<Integer, String> names = stream(receivedNames)
                .filter(e -> !e.getAsJsonObject().get("workingTable").isJsonNull())
                .collect(Collectors.toMap(e -> e.getAsJsonObject().get("workingTable").getAsJsonObject().get("tableNumber").getAsInt(),
                    e -> e.getAsJsonObject().get("name").getAsString()));
            return new BenderAsyncTaskResult<>(new Pair<>(amount, names));
        }

        @Override
        protected void innerOnSuccessfulPostExecute(BenderAsyncTaskResult<Pair<Integer, Map<Integer, String>>> result) {
            MainFragment.this.getView().setBackgroundColor(Color.TRANSPARENT);
            MainFragment.this.tableAdded(result.getResult().first, result.getResult().second);
        }

        @Override
        protected void innerOnUnsuccessfulPostExecute(BenderAsyncTaskResult<Pair<Integer, Map<Integer, String>>> error) {
            MainFragment.this.getView().setBackgroundColor(Color.rgb(204, 94, 61));
            Toast.makeText(MainActivity.commonContext, error.getError().getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
