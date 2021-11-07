package com.github.gscaparrotti.bendermobile.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.github.gscaparrotti.bendermobile.R;
import com.github.gscaparrotti.bendermobile.activities.MainActivity;
import com.github.gscaparrotti.bendermobile.dto.CustomerDto;
import com.github.gscaparrotti.bendermobile.dto.DishDto;
import com.github.gscaparrotti.bendermobile.dto.OrderDto;
import com.github.gscaparrotti.bendermobile.network.HttpServerInteractor;
import com.github.gscaparrotti.bendermobile.utilities.BenderAsyncTaskResult;
import com.github.gscaparrotti.bendermobile.utilities.BenderAsyncTaskResult.Empty;
import com.github.gscaparrotti.bendermobile.utilities.FragmentNetworkingBenderAsyncTask;
import com.github.gscaparrotti.bendermodel.model.Dish;
import com.github.gscaparrotti.bendermodel.model.IDish;
import com.github.gscaparrotti.bendermodel.model.Order;
import com.github.gscaparrotti.bendermodel.model.OrderedDish;
import com.github.gscaparrotti.bendermodel.model.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java9.util.Maps;
import java9.util.stream.Collectors;

import static com.github.gscaparrotti.bendermobile.utilities.StreamUtils.stream;

public class TableFragment extends Fragment {

    private static final String TABLE_NUMBER = "TABLENMBR";
    private static final HttpServerInteractor http = HttpServerInteractor.getInstance();
    private int tableNumber;
    private final List<Order> list = new LinkedList<>();
    private DishAdapter adapter;
    private Timer timer;

    private OnTableFragmentInteractionListener mListener;

    public TableFragment() {
    }

    public static TableFragment newInstance(int param1) {
        TableFragment fragment = new TableFragment();
        Bundle args = new Bundle();
        args.putInt(TABLE_NUMBER, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tableNumber = getArguments().getInt(TABLE_NUMBER);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_table, container, false);
        TextView text = view.findViewById(R.id.tableTitle);
        if (tableNumber > 0) {
            text.setText(text.getText() + " " + tableNumber);
        } else if (tableNumber == 0) {
            text.setText(getString(R.string.ViewAllPendingOrders));
            Button add = view.findViewById(R.id.addToTable);
            add.setEnabled(false);
            TextView price = view.findViewById(R.id.totalPrice);
            price.setVisibility(View.INVISIBLE);
        }
        ListView listView = view.findViewById(R.id.dishesList);
        adapter = new DishAdapter(requireActivity(), list);
        listView.setAdapter(adapter);
        Button update = view.findViewById(R.id.updateButton);
        update.setOnClickListener(v -> updateAndStartTasks());
        Button addDish = view.findViewById(R.id.addToTable);
        addDish.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onAddDishEventFired(tableNumber);
            }
        });
        CheckBox filter = view.findViewById(R.id.filterCheckBox);
        if (tableNumber == 0) {
            filter.setVisibility(View.VISIBLE);
        }
        filter.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (TableFragment.this.isVisible()) {
                updateOrders(new ArrayList<>(list));
                if (!isChecked) {
                    new ServerOrdersDownloader(TableFragment.this).execute(tableNumber);
                }
            }
        });
        if (tableNumber == 0) {
            addDish.setClickable(false);
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("FRAGMENT ON RESUME", "FRAGMENT ON RESUME");
        updateAndStartTasks();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnTableFragmentInteractionListener) {
            mListener = (OnTableFragmentInteractionListener) context;
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

    @Override
    public void onStop() {
        super.onStop();
        super.onDestroyView();
        Log.d("FRAGMENT STOP", "FRAGMENT STOP");
        stopTasks();
    }

    private void updateOrders(final List<Order> newList) {
        assert this.getView() != null;
        list.clear();
        final CheckBox filter = getView().findViewById(R.id.filterCheckBox);
        if (filter.isChecked()) {
            for (final Order o : newList) {
                if (o.getDish().getFilterValue() != 0) {
                    list.add(o);
                }
            }
        } else {
            list.addAll(newList);
        }
        if (tableNumber != 0) {
            Collections.sort(list, (o1, o2) -> (o2.getAmounts().getX() - o2.getAmounts().getY()) - (o1.getAmounts().getX() - o1.getAmounts().getY()));
        } else {
            Collections.sort(list, (o1, o2) -> {
                if (o1.getDish() instanceof OrderedDish && o2.getDish() instanceof OrderedDish) {
                    return (((OrderedDish) o1.getDish()).getTime().compareTo(((OrderedDish) o2.getDish()).getTime()));
                } else if (o1.getDish() instanceof OrderedDish && !(o2.getDish() instanceof OrderedDish)) {
                    return -1;
                } else if (o2.getDish() instanceof OrderedDish && !(o1.getDish() instanceof OrderedDish)){
                    return 1;
                } else {
                    return 0;
                }
            });
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        double totalPrice = 0;
        for (Order o : newList) {
            totalPrice += o.getAmounts().getX() * o.getDish().getPrice();
        }
        if (getView() != null) {
            TextView price = getView().findViewById(R.id.totalPrice);
            price.setText(getResources().getString(R.string.PrezzoTotale) + String.format("%.2f", totalPrice) + getResources().getString(R.string.valute));
        }
    }

    private void updateName(final String name) {
        if (getView() != null && tableNumber > 0 && !name.equals("customer" + tableNumber)) {
            TextView nameView = getView().findViewById(R.id.tableTitle);
            String newName = name.length() > 0 ? (" - " + name) : "";
            nameView.setText(getString(R.string.tableTitle) + " " + tableNumber + newName);
        }
    }

    private synchronized void updateAndStartTasks() {
        //if timer is running, then just update, otherwise create timer and start it
        if (timer != null) {
            new ServerOrdersDownloader(this).execute(tableNumber);
        } else {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                        MainActivity.runOnUI(() -> new ServerOrdersDownloader(TableFragment.this).execute(tableNumber));
                }
            }, 0, 6000);
        }
    }

    private synchronized void stopTasks() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
        }
        timer = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnTableFragmentInteractionListener {
        void onAddDishEventFired(final int tableNumber);
    }

    private class DishAdapter extends ArrayAdapter<Order> {

        private final LayoutInflater inflater;

        DishAdapter(Context context, List<Order> persone) {
            super(context, 0, persone);
            inflater = LayoutInflater.from(context);
        }

        @Override
        @NonNull
        public View getView(int position, View convertView,@NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_dish, parent, false);
            }
            final Order order = getItem(position);
            assert order != null;
            convertView.setOnClickListener(v -> {
                final DishDetailFragment detail = DishDetailFragment.newInstance(order);
                detail.show(getFragmentManager(), "Dialog");
            });
            convertView.setLongClickable(true);
            convertView.setOnLongClickListener(v -> {
                if (order.getAmounts().getX().equals(0)) {
                    return false;
                }
                order.getAmounts().setY(order.getAmounts().getX());
                if (tableNumber == 0) {
                    final IDish dish = new Dish(order.getDish().getName().substring(0, order.getDish().getName().lastIndexOf(" - ")), order.getDish().getPrice(), 0);
                    final Order newOrder = new Order(order.getTable(), dish, order.getAmounts());
                    new ServerOrdersUploader(TableFragment.this).execute(newOrder);
                } else {
                    new ServerOrdersUploader(TableFragment.this).execute(order);
                }
                return true;
            });
            convertView.findViewById(R.id.removeButton).setOnClickListener(v -> {
                if (order.getAmounts().getX().equals(0)) {
                    return;
                }
                if (tableNumber == 0) {
                    final IDish dish = new Dish(order.getDish().getName().substring(0, order.getDish().getName().lastIndexOf(" - ")), order.getDish().getPrice(), 0);
                    final Order newOrder = new Order(order.getTable(), dish, new Pair<>(-1, 1));
                    new ServerOrdersUploader(TableFragment.this).execute(newOrder);
                } else {
                    new ServerOrdersUploader(TableFragment.this).execute(new Order(order.getTable(), order.getDish(), new Pair<>(-1, 1)));
                }
            });
            ((TextView) convertView.findViewById(R.id.dish)).setText(order.getDish().getName());
            ((TextView) convertView.findViewById(R.id.dishToServe))
                    .setText(getResources().getString(R.string.StringOrdinati) + order.getAmounts().getX());
            ((TextView) convertView.findViewById(R.id.dishServed))
                    .setText(getResources().getString(R.string.StringDaServire) + (order.getAmounts().getX() - order.getAmounts().getY()));
            if (!order.getAmounts().getX().equals(order.getAmounts().getY())) {
                convertView.findViewById(R.id.itemTableLayout).setBackgroundColor(Color.parseColor("#80FF5050"));
            } else {
                convertView.findViewById(R.id.itemTableLayout).setBackgroundColor(Color.parseColor("#8099FF66"));
            }
            return convertView;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class ServerOrdersUploader extends FragmentNetworkingBenderAsyncTask<Order, Empty> {

        ServerOrdersUploader(Fragment fragment) {
            super(fragment);
        }

        @Override
        protected BenderAsyncTaskResult<Empty> innerDoInBackground(Order[] objects) {
            if (objects[0].getAmounts().getX() < 0) {
                final List<CustomerDto> customers = http.newSendAndReceive(CustomerDto.getGetCustomerDtoRequest(objects[0].getTable()));
                stream(customers)
                    .filter(c -> c.getWorkingTable() != null)
                    .map(CustomerDto::getName)
                    .forEach(customerName -> http.newSendAndReceive(OrderDto.getDeleteOrderDtoRequest(objects[0].getDish().getName(), customerName)));
            } else {
                final List<OrderDto> orders = http.newSendAndReceive(OrderDto.getGetOrderDtoRequest(objects[0].getTable()));
                stream(orders)
                    .filter(o -> o.getDish().getName().equals(objects[0].getDish().getName()) && !o.isServed())
                    .peek(o -> o.setServed(true))
                    .forEach(o -> http.newSendAndReceive(OrderDto.getUpdateOrderDtoRequest(o).addQueryParam("served", "true")));
            }
            return new BenderAsyncTaskResult<>(BenderAsyncTaskResult.EMPTY_RESULT);
        }

        @Override
        protected void innerOnSuccessfulPostExecute(BenderAsyncTaskResult<Empty> result) {
            Toast.makeText(MainActivity.commonContext, MainActivity.commonContext.getString(R.string.UpdateSuccess), Toast.LENGTH_SHORT).show();
            new ServerOrdersDownloader(TableFragment.this).execute(tableNumber);
        }

        @Override
        protected void innerOnUnsuccessfulPostExecute(BenderAsyncTaskResult<Empty> error) {
            final List<Order> errors = new ArrayList<>(1);
            errors.add(new Order(TableFragment.this.tableNumber, new Dish(error.getError().getMessage(), 0, 1), new Pair<>(0, 1)));
            updateOrders(errors);
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class ServerOrdersDownloader extends FragmentNetworkingBenderAsyncTask<Integer, Pair<List<Order>, String>> {

        ServerOrdersDownloader(final Fragment fragment) {
            super(fragment);
        }

        @Override
        protected BenderAsyncTaskResult<Pair<List<Order>, String>> innerDoInBackground(final Integer[] objects) {
            assert objects[0] >= 0;
            final String outputName;
            final List<Order> outputOrders;
            if (objects[0] > 0) {
                final List<CustomerDto> customers = http.newSendAndReceive(CustomerDto.getGetCustomerDtoRequest(objects[0]));
                outputName = stream(customers)
                    .filter(c -> c.getWorkingTable() != null)
                    .map(CustomerDto::getName)
                    .findAny()
                    .orElse(null);
            } else {
                outputName = null;
            }
            final List<OrderDto> ordersDto = http.newSendAndReceive(OrderDto.getGetOrderDtoRequest(objects[0] > 0 ? objects[0] : null));
            Collections.sort(ordersDto, (first, second) -> {
                if (Boolean.compare(first.isServed(), second.isServed()) != 0) {
                    return Boolean.compare(first.isServed(), second.isServed());
                }
                return new Date(first.getTime()).compareTo(new Date(second.getTime()));
            });
            // key: table number, value: map where key: dish, value; earliest order for that dish but with all the amounts
            // the first order to be inserted will always be the earliest because they've been sorted
            final Map<Integer, Map<IDish, Order>> tablesDishesMap = new HashMap<>();
            for (final OrderDto orderDto : ordersDto) {
                if (orderDto.getCustomer().getWorkingTable() != null) {
                    final DishDto dishDto = orderDto.getDish();
                    final String customerName = orderDto.getCustomer().getName();
                    final int tableNumber = orderDto.getCustomer().getWorkingTable().getTableNumber();
                    String dishName = dishDto.getName();
                    if (objects[0] == 0) {
                        dishName = dishName + " - " + tableNumber;
                        if (!customerName.equals("customer" + tableNumber)) {
                            dishName = dishName + " (" + customerName + ")";
                        }
                    }
                    final OrderedDish dish = new OrderedDish(dishName, dishDto.getPrice(), dishDto.getFilter(), new Date(orderDto.getTime()));
                    final Pair<Integer, Integer> amounts = new Pair<>(orderDto.getAmount(), orderDto.isServed() ? orderDto.getAmount() : 0);
                    final Order currentOrder = new Order(tableNumber, dish, amounts);
                    final Map<IDish, Order> dishesMap = Maps.computeIfAbsent(tablesDishesMap, tableNumber, integer -> new HashMap<>());
                    Maps.merge(dishesMap, dish, currentOrder, (a, b) -> {
                        final Pair<Integer, Integer> currentAmounts = a.getAmounts();
                        currentAmounts.setX(currentAmounts.getX() + b.getAmounts().getX());
                        currentAmounts.setY(currentAmounts.getY() + b.getAmounts().getY());
                        return a;
                    });
                }
            }
            outputOrders = stream(tablesDishesMap.entrySet())
                .flatMap(e -> stream(e.getValue().values()))
                .filter(o -> (objects[0] == 0 && o.getAmounts().getX() > o.getAmounts().getY()) || objects[0] > 0)
                .collect(Collectors.toUnmodifiableList());
            return new BenderAsyncTaskResult<>(new Pair<>(outputOrders, outputName));
        }

        @Override
        protected void innerOnSuccessfulPostExecute(final BenderAsyncTaskResult<Pair<List<Order>, String>> result) {
            commonOnPostExecute(result.getResult());
        }

        @Override
        protected void innerOnUnsuccessfulPostExecute(final BenderAsyncTaskResult<Pair<List<Order>, String>> error) {
            final List<Order> errorOrder = new ArrayList<>(1);
            errorOrder.add(new Order(TableFragment.this.tableNumber, new Dish(error.getError().getMessage(), 0, 1), new Pair<>(0, 1)));
            stopTasks();
            commonOnPostExecute(new Pair<>(errorOrder, null));
        }

        private void commonOnPostExecute(final Pair<List<Order>, String> orders) {
            updateOrders(orders.getX());
            updateName(orders.getY() != null ? orders.getY() : "");
        }
    }

}
