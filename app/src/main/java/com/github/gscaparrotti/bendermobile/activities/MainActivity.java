package com.github.gscaparrotti.bendermobile.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Toolbar;
import com.github.gscaparrotti.bendermobile.R;
import com.github.gscaparrotti.bendermobile.fragments.AddDishFragment;
import com.github.gscaparrotti.bendermobile.fragments.MainFragment;
import com.github.gscaparrotti.bendermobile.fragments.SettingsFragment;
import com.github.gscaparrotti.bendermobile.fragments.TableFragment;

public class MainActivity extends FragmentActivity implements TableFragment.OnTableFragmentInteractionListener, MainFragment.OnMainFragmentInteractionListener, AddDishFragment.OnAddDishFragmentInteractionListener {

    /*
        Il warning è un falso positivo: se commonContext non venisse riassegnato
        nel metodo onCreate e contenesse, ad esempio, una Activity, che tendenzialmente
        ha un ciclo vitale più corto di quello dell'applicazione, allora ci sarebbe
        un riferimento statico ad un oggetto che verrebbe altrimenti eliminato dal
        garbage collector, ma in questo caso:
        1) il riferimento viene inizializzato con l'applicationContext, che è sempre
        lo stesso a prescindere dalle varie Activity che si susseguono
        2) il riferimento, se anche fosse all'Activity, sarebbe comunque a quella corrente
        e non a quelle da buttare via, perchè viene aggiornato nel metodo onCreate
     */
    @SuppressLint("StaticFieldLeak")
    public static Context commonContext;
    public static Handler UIHandler;

    static {
        UIHandler = new Handler(Looper.getMainLooper());
    }

    public static void runOnUI(Runnable runnable) {
        UIHandler.post(runnable);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        commonContext = this.getApplicationContext();
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setActionBar(myToolbar);
        myToolbar.setTitleTextColor(Color.WHITE);
        if (savedInstanceState != null) {
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        replaceFragment(MainFragment.newInstance(), false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.items, menu);
        menu.findItem(R.id.settings_menu).setOnMenuItemClickListener(item -> {
            replaceFragment(new SettingsFragment(), true);
            return true;
        });
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("STOP", "STOP");
    }

    @Override
    public void onBackPressed() {
        toggleLoadingLabel(false);
        if (getSupportFragmentManager().getBackStackEntryCount() > 0)
            getSupportFragmentManager().popBackStack();
        else
            super.onBackPressed();

    }

    private void replaceFragment(Fragment fragment, boolean back) {
        toggleLoadingLabel(false);
        FragmentManager manager = this.getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(R.id.container, fragment);
        if (back)
            transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onTablePressedEventFired(int tableNumber) {
        replaceFragment(TableFragment.newInstance(tableNumber), true);
    }


    @Override
    public void onAddDishEventFired(final int tableNumber) {
        replaceFragment(AddDishFragment.newInstance(tableNumber), true);
    }

    public void toggleLoadingLabel(final boolean visible) {
        findViewById(R.id.loading_message).setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

}
