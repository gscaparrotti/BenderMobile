package com.android.gscaparrotti.bendermobile.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.gscaparrotti.bendermobile.R;
import com.android.gscaparrotti.bendermobile.activities.MainActivity;
import com.android.gscaparrotti.bendermobile.network.ServerInteractor;
import com.android.gscaparrotti.bendermobile.utilities.BenderAsyncTaskResult;
import com.android.gscaparrotti.bendermobile.utilities.BenderAsyncTaskResult.Empty;
import com.android.gscaparrotti.bendermobile.utilities.FragmentNetworkingBenderAsyncTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.github.gscaparrotti.bendermodel.model.Dish;
import com.github.gscaparrotti.bendermodel.model.IDish;
import com.github.gscaparrotti.bendermodel.model.Order;
import com.github.gscaparrotti.bendermodel.model.OrderedDish;
import com.github.gscaparrotti.bendermodel.model.Pair;

public class TableFragment extends Fragment {

    private static final String TABLE_NUMBER = "TABLENMBR";
    private int tableNumber;
    private List<Order> list = new LinkedList<>();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_table, container, false);
        TextView text = (TextView) view.findViewById(R.id.tableTitle);
        if (tableNumber > 0) {
            text.setText(text.getText() + " " + Integer.toString(tableNumber));
        } else if (tableNumber == 0) {
            text.setText(getString(R.string.ViewAllPendingOrders));
            Button add = (Button) view.findViewById(R.id.addToTable);
            add.setEnabled(false);
            TextView price = (TextView) view.findViewById(R.id.totalPrice);
            price.setVisibility(View.INVISIBLE);
        }
        ListView listView = (ListView) view.findViewById(R.id.dishesList);
        adapter = new DishAdapter(getActivity(), list);
        listView.setAdapter(adapter);
        Button update = (Button) view.findViewById(R.id.updateButton);
        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateAndStartTasks();
            }
        });
        Button addDish = (Button) view.findViewById(R.id.addToTable);
        addDish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onAddDishEventFired(tableNumber);
                }
            }
        });
        CheckBox filter = (CheckBox) view.findViewById(R.id.filterCheckBox);
        if (tableNumber == 0) {
            filter.setVisibility(View.VISIBLE);
        }
        filter.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (TableFragment.this.isVisible() && list != null) {
                    updateOrders(new ArrayList<>(list));
                    if (!isChecked) {
                        new ServerOrdersDownloader(TableFragment.this).execute(tableNumber);
                    }
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
    public void onAttach(Activity context) {
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
        if (list != null) {
            list.clear();
            final CheckBox filter = (CheckBox) getView().findViewById(R.id.filterCheckBox);
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
                Collections.sort(list, new Comparator<Order>() {
                    @Override
                    public int compare(Order o1, Order o2) {
                        return (o2.getAmounts().getX() - o2.getAmounts().getY()) - (o1.getAmounts().getX() - o1.getAmounts().getY());
                    }
                });
            } else {
                Collections.sort(list, new Comparator<Order>() {
                    @Override
                    public int compare(Order o1, Order o2) {
                        if (o1.getDish() instanceof OrderedDish && o2.getDish() instanceof OrderedDish) {
                            return (((OrderedDish) o1.getDish()).getTime().compareTo(((OrderedDish) o2.getDish()).getTime()));
                        } else if (o1.getDish() instanceof OrderedDish && !(o2.getDish() instanceof OrderedDish)) {
                            return -1;
                        } else if (o2.getDish() instanceof OrderedDish && !(o1.getDish() instanceof OrderedDish)){
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                });
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        double totalPrice = 0;
        for (Order o : newList) {
            totalPrice += o.getAmounts().getX() * o.getDish().getPrice();
        }
        if (getView() != null) {
            TextView price = (TextView) getView().findViewById(R.id.totalPrice);
            price.setText(getResources().getString(R.string.PrezzoTotale) + String.format("%.2f", totalPrice) + getResources().getString(R.string.valute));
        }
    }

    private void updateName(final String name) {
        if (getView() != null && tableNumber > 0) {
            TextView nameView = (TextView) getView().findViewById(R.id.tableTitle);
            String newName = name.length() > 0 ? (" - " + name) : "";
            nameView.setText(getString(R.string.tableTitle) + " " + Integer.toString(tableNumber) + newName);
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
                        MainActivity.runOnUI(new Runnable() {
                            @Override
                            public void run() {
                                new ServerOrdersDownloader(TableFragment.this).execute(tableNumber);
                            }
                        });
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

        private LayoutInflater inflater;

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
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final DishDetailFragment detail = DishDetailFragment.newInstance(order);
                    detail.show(getFragmentManager(), "Dialog");
                }
            });
            convertView.setLongClickable(true);
            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    order.getAmounts().setY(order.getAmounts().getX());
                    if (tableNumber == 0) {
                        final IDish dish = new Dish(order.getDish().getName().substring(0, order.getDish().getName().lastIndexOf(" - ")), order.getDish().getPrice(), 0);
                        final Order newOrder = new Order(order.getTable(), dish, order.getAmounts());
                        new ServerOrdersUploader(TableFragment.this).execute(newOrder);
                    } else {
                        new ServerOrdersUploader(TableFragment.this).execute(order);
                    }
                    return true;
                }
            });
            convertView.findViewById(R.id.removeButton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (tableNumber == 0) {
                        final IDish dish = new Dish(order.getDish().getName().substring(0, order.getDish().getName().lastIndexOf(" - ")), order.getDish().getPrice(), 0);
                        final Order newOrder = new Order(order.getTable(), dish, new Pair<>(-1, 1));
                        new ServerOrdersUploader(TableFragment.this).execute(newOrder);
                    } else {
                        new ServerOrdersUploader(TableFragment.this).execute(new Order(order.getTable(), order.getDish(), new Pair<>(-1, 1)));
                    }
                }
            });
            ((TextView) convertView.findViewById(R.id.dish)).setText(order.getDish().getName());
            ((TextView) convertView.findViewById(R.id.dishToServe))
                    .setText(getResources().getString(R.string.StringOrdinati) + Integer.toString(order.getAmounts().getX()));
            ((TextView) convertView.findViewById(R.id.dishServed))
                    .setText(getResources().getString(R.string.StringDaServire) + Integer.toString(order.getAmounts().getX() - order.getAmounts().getY()));
            if (!order.getAmounts().getX().equals(order.getAmounts().getY())) {
                convertView.findViewById(R.id.itemTableLayout).setBackgroundColor(Color.parseColor("#80FF5050"));
            } else {
                convertView.findViewById(R.id.itemTableLayout).setBackgroundColor(Color.parseColor("#8099FF66"));
            }
            return convertView;
        }
    }

    private class ServerOrdersUploader extends FragmentNetworkingBenderAsyncTask<Order, Empty> {

        ServerOrdersUploader(Fragment fragment) {
            super(fragment);
        }

        @Override
        protected BenderAsyncTaskResult<Empty> innerDoInBackground(Order[] objects) {
            boolean result = false;
            final Object resultFromServer = new ServerInteractor().sendCommandAndGetResult(ip, 6789, objects[0]);
            if (resultFromServer instanceof String) {
                final String stringResult = (String) resultFromServer;
                if (stringResult.equals("ORDER UPDATED CORRECTLY")) {
                    return new BenderAsyncTaskResult<>(BenderAsyncTaskResult.EMPTY_RESULT);
                }
            }
            return new BenderAsyncTaskResult<>(new IllegalArgumentException(MainActivity.commonContext.getString(R.string.DatiNonValidi)));
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

    private class ServerOrdersDownloader extends FragmentNetworkingBenderAsyncTask<Integer, Pair<List<Order>, String>> {

        ServerOrdersDownloader(final Fragment fragment) {
            super(fragment);
        }

        @Override
        protected BenderAsyncTaskResult<Pair<List<Order>, String>> innerDoInBackground(final Integer[] objects) {
            final Object receivedOrders;
            final Object receivedTableNames;
            final List<Order> outputOrders;
            final String outputName;
            if (objects[0] > 0) {
                receivedOrders = new ServerInteractor().sendCommandAndGetResult(ip, 6789, "GET TABLE " + objects[0]);
            } else if (objects[0] == 0) {
                receivedOrders = new ServerInteractor().sendCommandAndGetResult(ip, 6789, "GET PENDING ORDERS");
            } else {
                return new BenderAsyncTaskResult<>(new IllegalArgumentException(MainActivity.commonContext.getString(R.string.DatiNonValidi)));
            }
            receivedTableNames = new ServerInteractor().sendCommandAndGetResult(ip, 6789, "GET NAMES");
            if ((receivedOrders instanceof Map || receivedOrders instanceof List) && receivedTableNames instanceof Map) {
                @SuppressWarnings("unchecked")
                final Map<Integer, String> tableNames = (Map<Integer, String>) receivedTableNames;
                outputOrders = new LinkedList<>();
                if (receivedOrders instanceof Map) {
                    @SuppressWarnings("unchecked")
                    final Map<IDish, Pair<Integer, Integer>> orders = (Map<IDish, Pair<Integer, Integer>>) receivedOrders;
                    for (final Map.Entry<IDish, Pair<Integer, Integer>> entry : orders.entrySet()) {
                        outputOrders.add(new Order(TableFragment.this.tableNumber, entry.getKey(), entry.getValue()));
                    }
                    outputName = tableNames.get(objects[0]);
                } else {
                    @SuppressWarnings("unchecked")
                    final List<Order> datas = (List<Order>) receivedOrders;
                    for (final Order o : datas) {
                        final StringBuilder strbldr = new StringBuilder(o.getDish().getName());
                        strbldr.append(" - ").append(o.getTable());
                        if (tableNames.containsKey(o.getTable())) {
                            strbldr.append(" (").append(tableNames.get(o.getTable())).append(")");
                        }
                        if (o.getDish() instanceof OrderedDish) {
                            final OrderedDish originalDish = (OrderedDish) o.getDish();
                            final OrderedDish tempDish = new OrderedDish(strbldr.toString(), o.getDish().getPrice(), originalDish.getFilterValue(), originalDish);
                            outputOrders.add(new Order(o.getTable(), tempDish, o.getAmounts()));
                        } else {
                            outputOrders.add(new Order(o.getTable(), new Dish(strbldr.toString(), o.getDish().getPrice(), o.getDish().getFilterValue()), o.getAmounts()));
                        }
                    }
                    outputName = null;
                }
            } else {
                return new BenderAsyncTaskResult<>(new IllegalArgumentException(MainActivity.commonContext.getString(R.string.DatiNonValidi)));
            }
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
            commonOnPostExecute(new Pair<List<Order>, String>(errorOrder, null));
        }

        private void commonOnPostExecute(final Pair<List<Order>, String> orders) {
            updateOrders(orders.getX());
            updateName(orders.getY() != null ? orders.getY() : "");
        }
    }

}
