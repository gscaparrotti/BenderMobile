package com.github.gscaparrotti.bendermobile.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.github.gscaparrotti.bendermobile.R;
import com.github.gscaparrotti.bendermobile.activities.MainActivity;
import com.github.gscaparrotti.bendermobile.dto.CustomerDto;
import com.github.gscaparrotti.bendermobile.network.HttpServerInteractor;
import com.github.gscaparrotti.bendermobile.utilities.BenderAsyncTaskResult;
import com.github.gscaparrotti.bendermobile.utilities.BenderAsyncTaskResult.Empty;
import com.github.gscaparrotti.bendermobile.utilities.FragmentNetworkingBenderAsyncTask;
import com.github.gscaparrotti.bendermodel.model.Pair;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java9.util.Comparators;
import java9.util.stream.Collectors;

import static com.github.gscaparrotti.bendermobile.utilities.StreamUtils.stream;

public class CustomersFragment extends Fragment {

    private static final HttpServerInteractor http = HttpServerInteractor.getInstance();
    private final List<Pair<CustomerDto, Boolean>> list = new LinkedList<>();
    private CustomersAdapter adapter;
    private Timer timer;

    public CustomersFragment() {
    }

    public static CustomersFragment newInstance() {
        CustomersFragment fragment = new CustomersFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer, container, false);
        ListView listView = view.findViewById(R.id.customersList);
        adapter = new CustomersAdapter(requireActivity(), list);
        listView.setAdapter(adapter);
        Button update = view.findViewById(R.id.showCustomersButton);
        //noinspection unchecked
        update.setOnClickListener(v -> new ServerCustomersUploader(this).execute(this.list));
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("FRAGMENT ON RESUME", "FRAGMENT ON RESUME");
        new ServerCustomersDownloader(this).execute();
    }

    @Override
    public void onStop() {
        super.onStop();
        super.onDestroyView();
        Log.d("FRAGMENT STOP", "FRAGMENT STOP");
    }

    private void updateOrders(final List<CustomerDto> newList) {
        assert this.getView() != null;
        list.clear();
        final List<Pair<CustomerDto, Boolean>> temp = stream(newList)
            .filter(c -> c.getWorkingTable() != null)
            .sorted(Comparators.comparingInt(c -> c.getWorkingTable().getTableNumber()))
            .map(c -> new Pair<>(c, false))
            .collect(Collectors.toUnmodifiableList());
        list.addAll(temp);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private class CustomersAdapter extends ArrayAdapter<Pair<CustomerDto, Boolean>> {

        private final LayoutInflater inflater;

        CustomersAdapter(Context context, List<Pair<CustomerDto, Boolean>> customers) {
            super(context, 0, customers);
            inflater = LayoutInflater.from(context);
        }

        @Override
        @NonNull
        @SuppressLint("CutPasteId")
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            class MyTextWatcher implements TextWatcher {
                final EditText editText;
                private MyTextWatcher(final EditText editText) {
                    this.editText = editText;
                }
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    if (this.editText.getTag() != null && this.editText.hasFocus()) {
                        final int position = (int) this.editText.getTag();
                        final int a = System.identityHashCode(this);
                        stream(CustomersFragment.this.list)
                            .filter(c -> c.getX().getTable().getTableNumber() == CustomersAdapter.this.getItem(position).getX().getTable().getTableNumber())
                            .findAny()
                            .ifPresent(c -> {
                                CustomersAdapter.this.getItem(position).getX().setName(s.toString());
                                c.getX().setName(s.toString());
                                c.setY(true);
                            });
                    }
                }
            }
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_customer, parent, false);
                final EditText editText = convertView.findViewById(R.id.customerNameEditText);
                editText.addTextChangedListener(new MyTextWatcher(editText));
            }
            final CustomerDto customer = getItem(position).getX();
            assert customer != null;
            ((TextView) convertView.findViewById(R.id.customerNameText)).setText(getString(R.string.tableShort) + customer.getWorkingTable().getTableNumber());
            final EditText customerNameEditText = convertView.findViewById(R.id.customerNameEditText);
            customerNameEditText.setTag(position);
            if (customer.getName().equals("customer" + customer.getWorkingTable().getTableNumber())) {
                customerNameEditText.setText("");
            } else {
                customerNameEditText.setText(customer.getName());
            }
            return convertView;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class ServerCustomersUploader extends FragmentNetworkingBenderAsyncTask<List<Pair<CustomerDto, Boolean>>, Empty> {

        ServerCustomersUploader(Fragment fragment) {
            super(fragment);
        }

        @Override
        protected BenderAsyncTaskResult<Empty> innerDoInBackground(List<Pair<CustomerDto, Boolean>>[] objects) {
            final List<CustomerDto> toChange = stream(objects[0])
                .filter(c -> c.getY().equals(true))
                .map(Pair::getX)
                .collect(Collectors.toUnmodifiableList());
            stream(toChange).forEach(c -> http.newSendAndReceive(CustomerDto.getUpdateCustomerRequest(c)));
            return new BenderAsyncTaskResult<>(BenderAsyncTaskResult.EMPTY_RESULT);
        }

        @Override
        protected void innerOnSuccessfulPostExecute(BenderAsyncTaskResult<Empty> result) {
            Toast.makeText(MainActivity.commonContext, "Cliente aggiornato con successo", Toast.LENGTH_SHORT).show();
            new ServerCustomersDownloader(CustomersFragment.this).execute();
        }

        @Override
        protected void innerOnUnsuccessfulPostExecute(BenderAsyncTaskResult<Empty> error) {
            Toast.makeText(MainActivity.commonContext, "Errore nell'aggiornamento dei clienti", Toast.LENGTH_LONG).show();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class ServerCustomersDownloader extends FragmentNetworkingBenderAsyncTask<Empty, List<CustomerDto>> {

        ServerCustomersDownloader(final Fragment fragment) {
            super(fragment);
        }

        @Override
        protected BenderAsyncTaskResult<List<CustomerDto>> innerDoInBackground(final Empty[] objects) {
            final List<CustomerDto> customers = http.newSendAndReceive(CustomerDto.getGetCustomerDtoRequest());
            return new BenderAsyncTaskResult<>(customers);
        }

        @Override
        protected void innerOnSuccessfulPostExecute(final BenderAsyncTaskResult<List<CustomerDto>> result) {
            updateOrders(result.getResult());
        }

        @Override
        protected void innerOnUnsuccessfulPostExecute(final BenderAsyncTaskResult<List<CustomerDto>> error) {
            updateOrders(new ArrayList<>());
        }

    }

}
