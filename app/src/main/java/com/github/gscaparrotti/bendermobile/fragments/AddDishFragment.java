package com.github.gscaparrotti.bendermobile.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.github.gscaparrotti.bendermobile.R;
import com.github.gscaparrotti.bendermobile.activities.MainActivity;
import com.github.gscaparrotti.bendermobile.dto.CustomerDto;
import com.github.gscaparrotti.bendermobile.dto.DishDto;
import com.github.gscaparrotti.bendermobile.dto.OrderDto;
import com.github.gscaparrotti.bendermobile.dto.TableDto;
import com.github.gscaparrotti.bendermobile.network.HttpServerInteractor;
import com.github.gscaparrotti.bendermobile.network.PendingHttpRequest;
import com.github.gscaparrotti.bendermobile.utilities.BenderAsyncTaskResult;
import com.github.gscaparrotti.bendermobile.utilities.BenderAsyncTaskResult.Empty;
import com.github.gscaparrotti.bendermobile.utilities.FragmentNetworkingBenderAsyncTask;
import com.github.gscaparrotti.bendermodel.model.Dish;
import com.github.gscaparrotti.bendermodel.model.IDish;
import com.github.gscaparrotti.bendermodel.model.Order;
import com.github.gscaparrotti.bendermodel.model.OrderedDish;
import com.github.gscaparrotti.bendermodel.model.Pair;
import com.google.gson.reflect.TypeToken;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java9.util.Comparators;
import java9.util.stream.Collectors;

import static com.github.gscaparrotti.bendermobile.utilities.StreamUtils.stream;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnAddDishFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link AddDishFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AddDishFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static HttpServerInteractor http = HttpServerInteractor.getInstance();
    private int tableNumber;
    private List<IDish> originalList = new LinkedList<>();
    private AddDishAdapter addDishAdapter;
    private ArrayAdapter<String> namesAdapter;
    private OnAddDishFragmentInteractionListener mListener;

    public AddDishFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param tableNumber Parameter 1.
     * @return A new instance of fragment AddDishFragment.
     */
    public static AddDishFragment newInstance(int tableNumber) {
        AddDishFragment fragment = new AddDishFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PARAM1, tableNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tableNumber = getArguments().getInt(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_dish, container, false);
        ListView listView = (ListView) view.findViewById(R.id.addDishListView);
        addDishAdapter = new AddDishAdapter(getActivity(), new LinkedList<>());
        listView.setAdapter(addDishAdapter);
        final Button manualOrderButton = (Button) view.findViewById(R.id.buttonAggiungi);
        final EditText price = (EditText) view.findViewById(R.id.editText_prezzo);
        final EditText name = (EditText) view.findViewById(R.id.editText_nome);
        manualOrderButton.setOnClickListener(v -> {
            try {
                String nameString = name.getText().toString();
                double priceDouble = Double.parseDouble(price.getText().toString());
                IDish newDish;
                if (nameString.endsWith("*")) {
                    newDish = new OrderedDish(nameString, priceDouble, OrderedDish.Moments.ZERO, 1);
                } else {
                    newDish = new OrderedDish(nameString, priceDouble, OrderedDish.Moments.ZERO, 0);
                }
                Order newOrder = new Order(tableNumber, newDish, new Pair<>(1, 0));
                new ServerDishUploader(AddDishFragment.this).execute(newOrder);
            } catch (NumberFormatException e) {
                if (AddDishFragment.this.getActivity() != null) {
                    Toast.makeText(AddDishFragment.this.getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                addDishAdapter.clear();
                for (final IDish dish : originalList) {
                    if (dish.getName().toLowerCase().contains(s.toString().toLowerCase())) {
                        addDishAdapter.add(dish);
                    }
                }
            }
        });
        final Button newNameButton = (Button) view.findViewById(R.id.tableNameButton);
        final EditText newNameEditText = (EditText) view.findViewById(R.id.tableNameEditText);
        Spinner namesSpinner = view.findViewById(R.id.nameSpinner);
        namesAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item);
        namesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        namesSpinner.setAdapter(namesAdapter);
        namesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                newNameEditText.setText(namesAdapter.getItem(pos));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                newNameEditText.setText("");
            }
        });
        newNameButton.setOnClickListener(v -> new ServerNameUploader(AddDishFragment.this).execute(newNameEditText.getText().toString()));
        new ServerMenuDownloader(AddDishFragment.this).execute();
        new ServerNamesDownloader(AddDishFragment.this).execute(tableNumber);
        return view;
    }

    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        if (context instanceof OnAddDishFragmentInteractionListener) {
            mListener = (OnAddDishFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnAddDishFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void updateMenu(final List<IDish> newList) {
        if (originalList != null) {
            originalList.clear();
            originalList.addAll(newList);
        }
        addDishAdapter.clear();
        addDishAdapter.addAll(newList);
    }

    private void updateNames(final List<String> names) {
        namesAdapter.clear();
        namesAdapter.addAll(names);
    }

    public interface OnAddDishFragmentInteractionListener { }

    private class AddDishAdapter extends ArrayAdapter<IDish> {

        private LayoutInflater inflater;

        AddDishAdapter(Context context, List<IDish> dishes) {
            super(context, 0, dishes);
            inflater = LayoutInflater.from(context);
        }

        @Override
        @NonNull
        public View getView(int position, View convertView,@NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_dish_to_add, parent, false);
            }
            final IDish dish = Objects.requireNonNull(getItem(position));
            ((TextView) convertView.findViewById(R.id.addDishName)).setText(dish.getName());
            ((TextView) convertView.findViewById(R.id.addDishPrice)).setText(String.format(Locale.ITALIAN, "%.2f", dish.getPrice()));
            final Button button = (Button) convertView.findViewById(R.id.addDishbutton);
            button.setOnClickListener(v -> {
                Order order = new Order(tableNumber, new OrderedDish(dish, OrderedDish.Moments.ZERO), new Pair<>(1, 0));
                new ServerDishUploader(AddDishFragment.this).execute(order);
            });
            return convertView;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class ServerMenuDownloader extends FragmentNetworkingBenderAsyncTask<Void, List<IDish>> {

        ServerMenuDownloader(Fragment fragment) {
            super(fragment);
        }

        @Override
        protected BenderAsyncTaskResult<List<IDish>> innerDoInBackground(Void[] objects) {
            final PendingHttpRequest dishesRequest = new PendingHttpRequest()
                .setMethod(HttpServerInteractor.Method.GET)
                .setEndpoint("menu")
                .setReturnType(new TypeToken<List<DishDto>>(){}.getType());
            final List<DishDto> dishes = http.newSendAndReceive(dishesRequest);
            final List<IDish> menu = stream(dishes)
                .map(d -> new Dish(d.getName(), d.getPrice(), d.getFilter()))
                .sorted(Comparators.comparing(Dish::getName))
                .collect(Collectors.toUnmodifiableList());
            return new BenderAsyncTaskResult<>(menu);
        }

        @Override
        protected void innerOnSuccessfulPostExecute(BenderAsyncTaskResult<List<IDish>> result) {
            AddDishFragment.this.updateMenu(result.getResult());
        }

        @Override
        protected void innerOnUnsuccessfulPostExecute(BenderAsyncTaskResult<List<IDish>> error) {
            Toast.makeText(MainActivity.commonContext, error.getError().getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class ServerDishUploader extends FragmentNetworkingBenderAsyncTask<Order, Empty> {

        ServerDishUploader(Fragment fragment) {
            super(fragment);
        }

        @Override
        protected BenderAsyncTaskResult<Empty> innerDoInBackground(Order[] objects) {
            final PendingHttpRequest customersArrayRequest = new PendingHttpRequest()
                .setMethod(HttpServerInteractor.Method.GET)
                .setEndpoint("customers")
                .addQueryParam("tableNumber", Integer.toString(objects[0].getTable()))
                .setReturnType(new TypeToken<List<CustomerDto>>(){}.getType());
            final List<CustomerDto> customers = http.newSendAndReceive(customersArrayRequest);
            final String name = stream(customers)
                .filter(e -> e.getWorkingTable() != null)
                .map(CustomerDto::getName)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(MainActivity.commonContext.getString(R.string.DatiNonValidiIngresso)));
            for (final Order order : objects) {
                final OrderDto orderDto = new OrderDto();
                final DishDto dishDto = new DishDto();
                final CustomerDto customerDto = new CustomerDto();
                dishDto.setName(order.getDish().getName());
                dishDto.setPrice(order.getDish().getPrice());
                dishDto.setType(order.getDish().getFilterValue() == 0 ? ".Drink" : ".Food");
                customerDto.setName(name);
                orderDto.setDish(dishDto);
                orderDto.setCustomer(customerDto);
                orderDto.setAmount(1);
                orderDto.setServed(false);
                http.newSendAndReceive(OrderDto.getUpdateOrderDtoRequest(orderDto));
            }
            return new BenderAsyncTaskResult<>(BenderAsyncTaskResult.EMPTY_RESULT);
        }

        @Override
        protected void innerOnSuccessfulPostExecute(BenderAsyncTaskResult<Empty> result) {
            Toast.makeText(MainActivity.commonContext, MainActivity.commonContext.getString(R.string.orderAddSuccess), Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void innerOnUnsuccessfulPostExecute(BenderAsyncTaskResult<Empty> error) {
            Toast.makeText(MainActivity.commonContext, error.getError().getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class ServerNamesDownloader extends FragmentNetworkingBenderAsyncTask<Integer, List<String>> {

        public ServerNamesDownloader(Fragment fragment) {
            super(fragment);
        }

        @Override
        protected BenderAsyncTaskResult<List<String>> innerDoInBackground(Integer[] objects) {
            final List<CustomerDto> customers = http.newSendAndReceive(CustomerDto.getGetCustomerDtoRequest());
            final List<String> names = stream(customers)
                .filter(e -> e.getTable().getTableNumber() == objects[0])
                .map(CustomerDto::getName)
                .sorted()
                .collect(Collectors.toUnmodifiableList());
            return new BenderAsyncTaskResult<>(names);
        }

        @Override
        protected void innerOnSuccessfulPostExecute(BenderAsyncTaskResult<List<String>> result) {
            AddDishFragment.this.updateNames(result.getResult());
        }

        @Override
        protected void innerOnUnsuccessfulPostExecute(BenderAsyncTaskResult<List<String>> error) {
            Toast.makeText(MainActivity.commonContext, error.getError().getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class ServerNameUploader extends FragmentNetworkingBenderAsyncTask<String, Empty> {

        ServerNameUploader(Fragment fragment) {
            super(fragment);
        }

        @Override
        protected BenderAsyncTaskResult<Empty> innerDoInBackground(String[] objects) {
            for (final String name : objects) {
                final TableDto tableDto = new TableDto();
                final CustomerDto customerDto = new CustomerDto();
                tableDto.setTableNumber(tableNumber);
                customerDto.setName(name);
                customerDto.setWorkingTable(tableDto);
                customerDto.setTable(tableDto);
                http.newSendAndReceive(CustomerDto.getUpdateCustomerRequest(customerDto));
            }
            return new BenderAsyncTaskResult<>(BenderAsyncTaskResult.EMPTY_RESULT);
        }

        @Override
        protected void innerOnSuccessfulPostExecute(BenderAsyncTaskResult<Empty> result) {
            Toast.makeText(MainActivity.commonContext, MainActivity.commonContext.getString(R.string.NameUpdateSuccess), Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void innerOnUnsuccessfulPostExecute(BenderAsyncTaskResult<Empty> error) {
            Toast.makeText(MainActivity.commonContext, error.getError().getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

}
