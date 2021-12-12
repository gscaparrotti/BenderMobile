package com.github.gscaparrotti.bendermobile.utilities;

import android.annotation.SuppressLint;
import android.support.v4.app.Fragment;
import com.github.gscaparrotti.bendermobile.activities.MainActivity;

public abstract class FragmentNetworkingBenderAsyncTask<INPUT, OUTPUT> extends BenderAsyncTask<INPUT, OUTPUT> {

    @SuppressLint("StaticFieldLeak")
    private final Fragment fragment;

    public FragmentNetworkingBenderAsyncTask(final Fragment fragment) {
        this.fragment = fragment;
    }

    @Override
    protected final void onPreExecute() {
        super.onPreExecute();
        if (fragment.isAdded()) {
            assert fragment.getActivity() != null;
            if (fragment.getActivity() instanceof MainActivity) {
                ((MainActivity) fragment.getActivity()).toggleLoadingLabel(true);
            }
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
            if (fragment.getActivity() instanceof MainActivity) {
                ((MainActivity) fragment.getActivity()).toggleLoadingLabel(false);
            }
        }
    }

    protected void innerOnPreExecute() {
    }

    protected abstract BenderAsyncTaskResult<OUTPUT> innerDoInBackground(final INPUT[] objects);

    protected abstract void innerOnSuccessfulPostExecute(final BenderAsyncTaskResult<OUTPUT> result);

    protected abstract void innerOnUnsuccessfulPostExecute(final BenderAsyncTaskResult<OUTPUT> error);

}
