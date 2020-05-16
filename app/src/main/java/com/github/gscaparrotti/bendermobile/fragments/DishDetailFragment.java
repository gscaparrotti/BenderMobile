package com.github.gscaparrotti.bendermobile.fragments;

import android.content.res.Resources;
import android.os.Bundle;
import android.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.gscaparrotti.bendermobile.R;

import com.github.gscaparrotti.bendermodel.model.Order;

/**
 * Created by gscap_000 on 26/04/2016.
 */
public class DishDetailFragment extends DialogFragment {

    private static String ORDER_BUNDLE = "ORDER";
    private Order order;

    static DishDetailFragment newInstance(final Order order) {
        DishDetailFragment f = new DishDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(DishDetailFragment.ORDER_BUNDLE, order);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        order = (Order) getArguments().getSerializable(DishDetailFragment.ORDER_BUNDLE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_dish_detail, container, false);
        final Resources r = getResources();
        ((TextView) v.findViewById(R.id.dettaglio_dish)).setText(order.getDish().getName());
        ((TextView) v.findViewById(R.id.dettaglio_ordinati)).setText(r.getString(R.string.StringOrdinati) + order.getAmounts().getX());
        ((TextView) v.findViewById(R.id.dettaglio_da_servire)).setText(r.getString(R.string.StringDaServire) + (order.getAmounts().getX() - order.getAmounts().getY()));
        ((TextView) v.findViewById(R.id.dettaglio_costo_unitario)).setText(r.getString(R.string.StringCostoUnitario) + String.format("%.2f", order.getDish().getPrice()));
        ((TextView) v.findViewById(R.id.dettaglio_costo_ordinati)).setText(r.getString(R.string.StringCostoServiti) + String.format("%.2f", order.getAmounts().getY() * order.getDish().getPrice()));
        ((TextView) v.findViewById(R.id.dettaglio_costo_totale)).setText(r.getString(R.string.StringCOstoTotale) + String.format("%.2f", order.getAmounts().getX() * order.getDish().getPrice()));
        return v;
    }
}
