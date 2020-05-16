package com.android.gscaparrotti.bendermobile.utilities;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.ProgressDialog;
import com.android.gscaparrotti.bendermobile.R;
import com.android.gscaparrotti.bendermobile.activities.MainActivity;

public abstract class FragmentNetworkingBenderAsyncTask<INPUT, OUTPUT> extends BenderAsyncTask<INPUT, OUTPUT> {

    protected String ip;
    @SuppressLint("StaticFieldLeak")
    private final Fragment fragment;
    private ProgressDialog waitDialog;

    public FragmentNetworkingBenderAsyncTask(final Fragment fragment) {
        this.fragment = fragment;
        waitDialog = new ProgressDialog(fragment.getActivity());
        waitDialog.setMessage(MainActivity.commonContext.getString(R.string.Wait));
        waitDialog.setIndeterminate(true);
        waitDialog.setCancelable(true);
    }

    @Override
    protected final void onPreExecute() {
        super.onPreExecute();
        if (fragment.isAdded()) {
            waitDialog.show();
            this.ip = fragment.getActivity().getSharedPreferences("BenderIP", 0).getString("BenderIP", "Absent");
            innerOnPreExecute();
        } else {
            this.cancel(true);
        }
    }

    @Override
    protected final BenderAsyncTaskResult<OUTPUT> doInBackground(final INPUT[] objects) {
        try {
            return innerDoInBackground(objects);
        } catch (final Exception e) {
            return new BenderAsyncTaskResult<>(e);
        }
    }

    @Override
    protected final void onPostExecute(final BenderAsyncTaskResult<OUTPUT> result) {
        super.onPostExecute(result);
        if (fragment.isAdded()) {
            if (result.isSuccess()) {
                innerOnSuccessfulPostExecute(result);
            } else {
                innerOnUnsuccessfulPostExecute(result);
            }
        }
        waitDialog.hide();
    }

    protected void innerOnPreExecute() {
    }

    protected abstract BenderAsyncTaskResult<OUTPUT> innerDoInBackground(final INPUT[] objects);

    protected abstract void innerOnSuccessfulPostExecute(final BenderAsyncTaskResult<OUTPUT> result);

    protected abstract void innerOnUnsuccessfulPostExecute(final BenderAsyncTaskResult<OUTPUT> error);

}