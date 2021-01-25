package com.github.gscaparrotti.bendermobile.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import com.github.gscaparrotti.bendermobile.R;
import com.github.gscaparrotti.bendermobile.activities.MainActivity;
import com.github.gscaparrotti.bendermobile.dto.CustomerDto;
import com.github.gscaparrotti.bendermobile.dto.OrderDto;
import com.github.gscaparrotti.bendermobile.dto.TableDto;
import com.github.gscaparrotti.bendermobile.network.HttpServerInteractor;
import com.github.gscaparrotti.bendermobile.network.HttpServerInteractor.Method;
import com.github.gscaparrotti.bendermobile.network.PendingHttpRequest;
import com.github.gscaparrotti.bendermobile.utilities.BenderAsyncTaskResult;
import com.github.gscaparrotti.bendermobile.utilities.BenderAsyncTaskResult.Empty;
import com.github.gscaparrotti.bendermobile.utilities.FragmentNetworkingBenderAsyncTask;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java9.util.stream.Collectors;

import static com.github.gscaparrotti.bendermobile.utilities.StreamUtils.stream;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnMainFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MainFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainFragment extends Fragment {

    private static final HttpServerInteractor http = HttpServerInteractor.getInstance();
    private TableAdapter ta;
    private int tablesCount = 0;
    private final Map<Integer, String> names = new HashMap<>();

    private OnMainFragmentInteractionListener mListener;

    public MainFragment() {
    }

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        GridView gv = view.findViewById(R.id.tablesContainer);
        ta = new TableAdapter(requireActivity());
        gv.setAdapter(ta);
        new TableAmountDownloader(MainFragment.this).execute();
        view.findViewById(R.id.mainUpdate).setOnClickListener(v -> new TableAmountDownloader(MainFragment.this).execute());
        view.findViewById(R.id.allPending).setOnClickListener(v -> mListener.onTablePressedEventFired(0));
        return view;
    }

    @Override
    public void onAttach(Context context) {
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

        private final LayoutInflater inflater;

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
            final TextView tableView = convertView.findViewById(R.id.table);
            tableView.setText(getString(R.string.itemTableText) + table + formattedName(names.get(table), table));
            convertView.setLongClickable(true);
            convertView.setOnClickListener(v -> mListener.onTablePressedEventFired(table));
            convertView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(requireActivity())
                        .setTitle(getString(R.string.ResetConfirmDialogTitle))
                        .setMessage(R.string.ResetConfirmDialogQuestion)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                            new TableResetRequestUploader(MainFragment.this).execute(table);
                            new TableAmountDownloader(MainFragment.this).execute();
                        })
                        .setNegativeButton(android.R.string.no, null).show();
                return true;
            });
            return convertView;
        }

        private String formattedName(final String name, final int tableNumber) {
            return !(name == null || name.equals("customer" + tableNumber)) ? " - " + name : "";
        }
    }

    @SuppressLint("StaticFieldLeak")
    private final class TableResetRequestUploader extends FragmentNetworkingBenderAsyncTask<Integer, Empty> {

        TableResetRequestUploader(Fragment fragment) {
            super(fragment);
        }

        @Override
        protected BenderAsyncTaskResult<Empty> innerDoInBackground(Integer[] objects) {
            final List<OrderDto> orders = http.newSendAndReceive(OrderDto.getGetOrderDtoRequest(objects[0]));
            stream(orders)
                .map(OrderDto::getId)
                .forEach(id -> http.newSendAndReceive(new PendingHttpRequest().setMethod(Method.DELETE).setEndpoint("orders/" + id)));
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

    @SuppressLint("StaticFieldLeak")
    private final class TableAmountDownloader extends FragmentNetworkingBenderAsyncTask<Empty, Pair<Integer, Map<Integer, String>>> {

        TableAmountDownloader(Fragment fragment) {
            super(fragment);
        }

        @Override
        protected BenderAsyncTaskResult<Pair<Integer, Map<Integer, String>>> innerDoInBackground(Empty[] objects) {
            final List<TableDto> tables = http.newSendAndReceive(TableDto.getGetTableDtoRequest());
            final int amount = stream(tables)
                .map(TableDto::getTableNumber)
                .max(Integer::compare)
                .orElse(0);
            final List<CustomerDto> customers = http.newSendAndReceive(CustomerDto.getGetCustomerDtoRequest());
            final Map<Integer, String> names = stream(customers)
                .filter(e -> e.getWorkingTable() != null)
                .collect(Collectors.toMap(e -> e.getWorkingTable().getTableNumber(), CustomerDto::getName));
            return new BenderAsyncTaskResult<>(new Pair<>(amount, names));
        }

        @Override
        protected void innerOnSuccessfulPostExecute(BenderAsyncTaskResult<Pair<Integer, Map<Integer, String>>> result) {
            if (MainFragment.this.getView() != null) {
                MainFragment.this.getView().setBackgroundColor(Color.TRANSPARENT);
            }
            MainFragment.this.tableAdded(result.getResult().first, result.getResult().second);
        }

        @Override
        protected void innerOnUnsuccessfulPostExecute(BenderAsyncTaskResult<Pair<Integer, Map<Integer, String>>> error) {
            if (MainFragment.this.getView() != null) {
                MainFragment.this.getView().setBackgroundColor(Color.rgb(204, 94, 61));
            }
            MainFragment.this.tableAdded(0, Collections.emptyMap());
            Toast.makeText(MainActivity.commonContext, error.getError().getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
