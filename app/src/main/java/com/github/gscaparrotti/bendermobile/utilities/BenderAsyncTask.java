package com.github.gscaparrotti.bendermobile.utilities;

import android.os.AsyncTask;

public abstract class BenderAsyncTask<A, C> extends AsyncTask<A, Void, BenderAsyncTaskResult<C>> {

    @Override
    protected abstract BenderAsyncTaskResult<C> doInBackground(A[] objects);

}
