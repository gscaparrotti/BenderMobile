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
import android.widget.CompoundButton.OnCheckedChangeListener;
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
import java9.util.function.Function;
import java9.util.stream.Collectors;

import static com.github.gscaparrotti.bendermobile.utilities.StreamUtils.stream;

@SuppressWarnings("deprecation")
@SuppressLint("SetTextI18n")
public class TableFragment extends Fragment {

    private static final String TABLE_NUMBER = "TABLENMBR";
    private static final String NAME_SEPARATOR = " ~ ";
    private static final HttpServerInteractor http = HttpServerInteractor.getInstance();
    private int tableNumber;
    private final List<Order> list = new LinkedList<>();
    private boolean filter = false;
    private boolean aggregate = false;
    private boolean aggregateAll = false;
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
        if (this.getArguments() != null) {
            this.tableNumber = this.getArguments().getInt(TABLE_NUMBER);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_table, container, false);
        ListView listView = view.findViewById(R.id.dishesList);
        this.adapter = new DishAdapter(this.requireActivity(), this.list);
        listView.setAdapter(this.adapter);
        final TextView text = view.findViewById(R.id.tableTitle);
        final Button add = view.findViewById(R.id.addToTable);
        final TextView price = view.findViewById(R.id.totalPrice);
        final CheckBox filter = view.findViewById(R.id.filterCheckBox);
        final CheckBox aggregate = view.findViewById(R.id.aggregationCheckBox);
        final CheckBox aggregateAll = view.findViewById(R.id.aggregationAllCheckBox);
        final Button update = view.findViewById(R.id.updateButton);
        if (this.tableNumber > 0) {
            text.setText(text.getText() + " " + this.tableNumber);
        } else if (this.tableNumber == 0) {
            text.setText(this.getString(R.string.ViewAllPendingOrders));
            add.setVisibility(View.INVISIBLE);
            price.setVisibility(View.INVISIBLE);
            filter.setVisibility(View.VISIBLE);
            aggregate.setVisibility(View.VISIBLE);
            aggregateAll.setVisibility(View.VISIBLE);
            aggregateAll.setEnabled(false);
        }
        update.setOnClickListener(v -> this.updateAndStartTasks());
        add.setOnClickListener(v -> {
            if (this.mListener != null) {
                this.mListener.onAddDishEventFired(this.tableNumber);
            }
        });
        final OnCheckedChangeListener refreshOrdersOnCheckChange = (buttonView, isChecked) -> {
            if (TableFragment.this.isVisible()) {
                new ServerOrdersDownloader(TableFragment.this).execute(new ServerOrdersDownloaderParams(this.tableNumber, this.aggregate && this.aggregateAll));
            }
        };
        filter.setOnCheckedChangeListener((buttonView, isChecked) -> {
            this.filter = isChecked;
            TableFragment.this.updateOrders(new ArrayList<>(this.list));
            refreshOrdersOnCheckChange.onCheckedChanged(buttonView, isChecked);
        });
        aggregate.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            this.aggregate = isChecked;
            aggregateAll.setEnabled(isChecked);
            TableFragment.this.updateOrders(new ArrayList<>());
            refreshOrdersOnCheckChange.onCheckedChanged(buttonView, isChecked);
        }));
        aggregateAll.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            this.aggregateAll = isChecked;
            TableFragment.this.updateOrders(new ArrayList<>());
            refreshOrdersOnCheckChange.onCheckedChanged(buttonView, isChecked);
        }));
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("FRAGMENT ON RESUME", "FRAGMENT ON RESUME");
        this.updateAndStartTasks();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnTableFragmentInteractionListener) {
            this.mListener = (OnTableFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnMainFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.mListener = null;
    }

    @Override
    public void onStop() {
        super.onStop();
        super.onDestroyView();
        Log.d("FRAGMENT STOP", "FRAGMENT STOP");
        this.stopTasks();
    }

    @SuppressLint("DefaultLocale")
    private void updateOrders(final List<Order> rawOrders) {
        assert this.getView() != null;
        this.list.clear();
        final List<Order> orders = stream(rawOrders)
            .filter(o -> !this.filter || o.getDish().getFilterValue() != 0)
            .filter(o -> this.tableNumber > 0 || this.aggregate || o.getAmounts().getX() > o.getAmounts().getY())
            .collect(Collectors.toUnmodifiableList());
        if (this.aggregate) {
            final Map<IDish, Order> ordersByDish = stream(orders)
                .map(o -> {
                    final IDish oldDish = o.getDish();
                    final int nameSeparatorIndex = oldDish.getName().lastIndexOf(NAME_SEPARATOR);
                    final String newName = nameSeparatorIndex < 0 ? oldDish.getName() : oldDish.getName().substring(0, nameSeparatorIndex);
                    final IDish newDish = new Dish(newName, oldDish.getPrice(), oldDish.getFilterValue());
                    return new Order(-1, newDish, o.getAmounts());
                })
                .collect(Collectors.toMap(Order::getDish, Function.identity(), (o1, o2) -> {
                    final Pair<Integer, Integer> newAmounts = new Pair<>(0, 0);
                    assert o1.getDish().equals(o2.getDish());
                    newAmounts.setX(o1.getAmounts().getX() + o2.getAmounts().getX());
                    newAmounts.setY(o1.getAmounts().getY() + o2.getAmounts().getY());
                    return new Order(-1, o1.getDish(), newAmounts);
                }));
            final List<Order> aggregatedOrders = stream(ordersByDish.values())
                .filter(o -> o.getAmounts().getX() > o.getAmounts().getY())
                .collect(Collectors.toUnmodifiableList());
            this.list.addAll(aggregatedOrders);
        } else {
            this.list.addAll(orders);
        }
        if (this.tableNumber != 0) {
            Collections.sort(this.list, (o1, o2) -> (o2.getAmounts().getX() - o2.getAmounts().getY()) - (o1.getAmounts().getX() - o1.getAmounts().getY()));
        } else {
            Collections.sort(this.list, (o1, o2) -> {
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
        if (this.adapter != null) {
            this.adapter.notifyDataSetChanged();
        }
        double totalPrice = 0;
        for (Order o : orders) {
            totalPrice += o.getAmounts().getX() * o.getDish().getPrice();
        }
        if (this.getView() != null) {
            TextView price = this.getView().findViewById(R.id.totalPrice);
            price.setText(this.getResources().getString(R.string.PrezzoTotale) + String.format("%.2f", totalPrice) + this.getResources().getString(R.string.valute));
        }
    }

    private void updateName(final String name) {
        if (this.getView() != null && this.tableNumber > 0 && !name.equals("customer" + this.tableNumber)) {
            TextView nameView = this.getView().findViewById(R.id.tableTitle);
            String newName = name.length() > 0 ? (NAME_SEPARATOR + name) : "";
            nameView.setText(this.getString(R.string.tableTitle) + " " + this.tableNumber + newName);
        }
    }

    private synchronized void updateAndStartTasks() {
        //if timer is running, then just update, otherwise create timer and start it
        if (this.timer != null) {
            new ServerOrdersDownloader(this).execute(new ServerOrdersDownloaderParams(this.tableNumber, this.aggregate && this.aggregateAll));
        } else {
            this.timer = new Timer();
            this.timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    MainActivity.runOnUI(() -> new ServerOrdersDownloader(TableFragment.this).execute(new ServerOrdersDownloaderParams(TableFragment.this.tableNumber, TableFragment.this.aggregate && TableFragment.this.aggregateAll)));
                }
            }, 0, 6000);
        }
    }

    private synchronized void stopTasks() {
        if (this.timer != null) {
            this.timer.cancel();
            this.timer.purge();
        }
        this.timer = null;
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
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = this.inflater.inflate(R.layout.item_dish, parent, false);
            }
            final Order order = this.getItem(position);
            assert order != null;
            convertView.setOnClickListener(v -> {
                final DishDetailFragment detail = DishDetailFragment.newInstance(order);
                detail.show(TableFragment.this.getFragmentManager(), "Dialog");
            });
            convertView.setLongClickable(true);
            convertView.setOnLongClickListener(v -> {
                if (order.getAmounts().getX().equals(0) || TableFragment.this.aggregate) {
                    return false;
                }
                order.getAmounts().setY(order.getAmounts().getX());
                if (TableFragment.this.tableNumber == 0) {
                    final IDish dish = new Dish(order.getDish().getName().substring(0, order.getDish().getName().lastIndexOf(NAME_SEPARATOR)), order.getDish().getPrice(), 0);
                    final Order newOrder = new Order(order.getTable(), dish, order.getAmounts());
                    new ServerOrdersUploader(TableFragment.this).execute(newOrder);
                } else {
                    new ServerOrdersUploader(TableFragment.this).execute(order);
                }
                return true;
            });
            final Button removeButton = convertView.findViewById(R.id.removeButton);
            removeButton.setVisibility((order.getTable() >= 0 && order.getAmounts().getY() >= 0) ? View.VISIBLE : View.INVISIBLE);
            removeButton.setOnClickListener(v -> {
                if (order.getAmounts().getX().equals(0)) {
                    return;
                }
                if (TableFragment.this.tableNumber == 0) {
                    final IDish dish = new Dish(order.getDish().getName().substring(0, order.getDish().getName().lastIndexOf(NAME_SEPARATOR)), order.getDish().getPrice(), 0);
                    final Order newOrder = new Order(order.getTable(), dish, new Pair<>(-1, 1));
                    new ServerOrdersUploader(TableFragment.this).execute(newOrder);
                } else {
                    new ServerOrdersUploader(TableFragment.this).execute(new Order(order.getTable(), order.getDish(), new Pair<>(-1, 1)));
                }
            });
            ((TextView) convertView.findViewById(R.id.dish)).setText(order.getDish().getName());
            final TextView dishToServe = convertView.findViewById(R.id.dishToServe);
            final TextView dishServed = convertView.findViewById(R.id.dishServed);
            if (order.getAmounts().getY() >= 0) {
                dishToServe.setVisibility(View.VISIBLE);
                dishServed.setVisibility(View.VISIBLE);
                dishToServe.setText(TableFragment.this.getResources().getString(R.string.StringOrdinati) + order.getAmounts().getX());
                dishServed.setText(TableFragment.this.getResources().getString(R.string.StringDaServire) + (order.getAmounts().getX() - order.getAmounts().getY()));
            } else {
                dishToServe.setVisibility(View.INVISIBLE);
                dishServed.setVisibility(View.INVISIBLE);
            }
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
            new ServerOrdersDownloader(TableFragment.this).execute(new ServerOrdersDownloaderParams(TableFragment.this.tableNumber, TableFragment.this.aggregate && TableFragment.this.aggregateAll));
        }

        @Override
        protected void innerOnUnsuccessfulPostExecute(BenderAsyncTaskResult<Empty> error) {
            final List<Order> errors = new ArrayList<>(1);
            errors.add(new Order(TableFragment.this.tableNumber, new Dish(error.getError().getMessage(), 0, 1), new Pair<>(0, 1)));
            TableFragment.this.updateOrders(errors);
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class ServerOrdersDownloader extends FragmentNetworkingBenderAsyncTask<ServerOrdersDownloaderParams, Pair<List<Order>, String>> {

        ServerOrdersDownloader(final Fragment fragment) {
            super(fragment);
        }

        @Override
        protected BenderAsyncTaskResult<Pair<List<Order>, String>> innerDoInBackground(final ServerOrdersDownloaderParams[] objects) {
            final int requestedTableNumber = objects[0].tableNumber;
            final boolean includeAllOrders = objects[0].includeAllOrders;
            assert requestedTableNumber >= 0;
            final String outputName;
            final List<Order> outputOrders;
            if (requestedTableNumber > 0) {
                final List<CustomerDto> customers = http.newSendAndReceive(CustomerDto.getGetCustomerDtoRequest(requestedTableNumber));
                outputName = stream(customers)
                    .filter(c -> c.getWorkingTable() != null)
                    .map(CustomerDto::getName)
                    .findAny()
                    .orElse(null);
            } else {
                outputName = null;
            }
            final List<OrderDto> ordersDto = http.newSendAndReceive(OrderDto.getGetOrderDtoRequest(requestedTableNumber > 0 ? requestedTableNumber : null));
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
                if (orderDto.getCustomer().getWorkingTable() == null && !includeAllOrders) {
                    continue;
                }
                final DishDto dishDto = orderDto.getDish();
                final String customerName = orderDto.getCustomer().getName();
                final int tableNumber = orderDto.getCustomer().getTable().getTableNumber();
                String dishName = dishDto.getName();
                if (requestedTableNumber == 0) {
                    dishName = dishName + NAME_SEPARATOR + tableNumber;
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
            outputOrders = stream(tablesDishesMap.entrySet())
                .flatMap(e -> stream(e.getValue().values()))
                .collect(Collectors.toUnmodifiableList());
            return new BenderAsyncTaskResult<>(new Pair<>(outputOrders, outputName));
        }

        @Override
        protected void innerOnSuccessfulPostExecute(final BenderAsyncTaskResult<Pair<List<Order>, String>> result) {
            this.commonOnPostExecute(result.getResult());
        }

        @Override
        protected void innerOnUnsuccessfulPostExecute(final BenderAsyncTaskResult<Pair<List<Order>, String>> error) {
            final List<Order> errorOrder = new ArrayList<>(1);
            errorOrder.add(new Order(TableFragment.this.tableNumber, new Dish(error.getError().getMessage(), 0, 1), new Pair<>(0, -1)));
            TableFragment.this.stopTasks();
            this.commonOnPostExecute(new Pair<>(errorOrder, null));
        }

        private void commonOnPostExecute(final Pair<List<Order>, String> orders) {
            TableFragment.this.updateOrders(orders.getX());
            TableFragment.this.updateName(orders.getY() != null ? orders.getY() : "");
        }
    }

    private static class ServerOrdersDownloaderParams {

        final int tableNumber;
        final boolean includeAllOrders;

        public ServerOrdersDownloaderParams(int tableNumber, boolean includeAllOrders) {
            this.tableNumber = tableNumber;
            this.includeAllOrders = includeAllOrders;
        }

    }

}
