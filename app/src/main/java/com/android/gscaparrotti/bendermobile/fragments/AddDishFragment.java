package com.android.gscaparrotti.bendermobile.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.gscaparrotti.bendermobile.R;
import com.android.gscaparrotti.bendermobile.activities.MainActivity;
import com.android.gscaparrotti.bendermobile.network.ServerInteractor;
import com.android.gscaparrotti.bendermobile.utilities.BenderAsyncTaskResult;
import com.android.gscaparrotti.bendermobile.utilities.BenderAsyncTaskResult.Empty;
import com.android.gscaparrotti.bendermobile.utilities.FragmentNetworkingBenderAsyncTask;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import model.IDish;
import model.IMenu;
import model.Order;
import model.OrderedDish;
import model.Pair;


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
    private int tableNumber;
    private List<IDish> list = new LinkedList<>();
    private AddDishAdapter adapter;

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
        adapter = new AddDishAdapter(getActivity(), list);
        listView.setAdapter(adapter);
        final Button manualOrderButton = (Button) view.findViewById(R.id.buttonAggiungi);
        final EditText price = (EditText) view.findViewById(R.id.editText_prezzo);
        final EditText name = (EditText) view.findViewById(R.id.editText_nome);
        manualOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String nameString = name.getText().toString();
                    Double priceDouble = Double.parseDouble(price.getText().toString());
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
            }
        });
        final Button newNameButton = (Button) view.findViewById(R.id.tableNameButton);
        final EditText newNameEditText = (EditText) view.findViewById(R.id.tableNameEditText);
        newNameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ServerNameUploader(AddDishFragment.this).execute(newNameEditText.getText().toString());
            }
        });
        new ServerMenuDownloader(AddDishFragment.this).execute();
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

    private void update(final List<IDish> newList) {
        if (list != null) {
            list.clear();
            list.addAll(newList);
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    public interface OnAddDishFragmentInteractionListener { }

    private class AddDishAdapter extends ArrayAdapter<IDish> {

        private LayoutInflater inflater;

        AddDishAdapter(Context context, List<IDish> persone) {
            super(context, 0, persone);
            inflater = LayoutInflater.from(context);
        }

        @Override
        @NonNull
        public View getView(int position, View convertView,@NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_dish_to_add, parent, false);
            }
            final IDish dish = getItem(position);
            ((TextView) convertView.findViewById(R.id.addDishName)).setText(dish.getName());
            ((TextView) convertView.findViewById(R.id.addDishPrice)).setText(String.format("%.2f", dish.getPrice()));
            final Button button = (Button) convertView.findViewById(R.id.addDishbutton);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Order order = new Order(tableNumber, new OrderedDish(dish, OrderedDish.Moments.ZERO), new Pair<>(1, 0));
                    new ServerDishUploader(AddDishFragment.this).execute(order);
                }
            });
            return convertView;
        }
    }

    private class ServerMenuDownloader extends FragmentNetworkingBenderAsyncTask<Void, List<IDish>> {

        ServerMenuDownloader(Fragment fragment) {
            super(fragment);
        }

        @Override
        protected BenderAsyncTaskResult<List<IDish>> innerDoInBackground(Void[] objects) {
            final Object input = new ServerInteractor().sendCommandAndGetResult(ip, 6789, "GET MENU");
            if (input instanceof IMenu) {
                return new BenderAsyncTaskResult<>(Arrays.asList(((IMenu) input).getDishesArray()));
            }
            return new BenderAsyncTaskResult<>(new IllegalArgumentException(MainActivity.commonContext.getString(R.string.DatiNonValidi)));
        }

        @Override
        protected void innerOnSuccessfulPostExecute(BenderAsyncTaskResult<List<IDish>> result) {
            AddDishFragment.this.update(result.getResult());
        }

        @Override
        protected void innerOnUnsuccessfulPostExecute(BenderAsyncTaskResult<List<IDish>> error) {
            Toast.makeText(MainActivity.commonContext, error.getError().getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private class ServerDishUploader extends FragmentNetworkingBenderAsyncTask<Order, Empty> {

        ServerDishUploader(Fragment fragment) {
            super(fragment);
        }

        @Override
        protected BenderAsyncTaskResult<Empty> innerDoInBackground(Order[] objects) {
            for (final Order order : objects) {
                Object result = new ServerInteractor().sendCommandAndGetResult(ip, 6789, order);
                if (result instanceof String) {
                    if (!result.equals("ORDER ADDED CORRECTLY")) {
                        return new BenderAsyncTaskResult<>(new IllegalArgumentException(MainActivity.commonContext.getString(R.string.DatiNonValidiIngresso)));
                    }
                }
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

    private class ServerNameUploader extends FragmentNetworkingBenderAsyncTask<String, Empty> {

        ServerNameUploader(Fragment fragment) {
            super(fragment);
        }

        @Override
        protected BenderAsyncTaskResult<Empty> innerDoInBackground(String[] objects) {
            for (final String name : objects) {
                Object result;
                if (name.length() > 0) {
                    result = new ServerInteractor().sendCommandAndGetResult(ip, 6789, "SET NAME " + tableNumber + " " + name);
                } else {
                    result = new ServerInteractor().sendCommandAndGetResult(ip, 6789, "REMOVE NAME " + tableNumber);
                }
                if (result instanceof String) {
                    if (!result.equals("NAME SET CORRECTLY")) {
                        return new BenderAsyncTaskResult<>(new IllegalArgumentException(MainActivity.commonContext.getString(R.string.DatiNonValidiIngresso)));
                    }
                }
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
