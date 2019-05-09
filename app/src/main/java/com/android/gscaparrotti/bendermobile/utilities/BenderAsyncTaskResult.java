package com.android.gscaparrotti.bendermobile.utilities;

public class BenderAsyncTaskResult<A> {

    public static final Empty EMPTY_RESULT = Empty.getInstance();

    private final boolean success;
    private final A result;
    private final Exception error;

    public BenderAsyncTaskResult(final A result) {
        this.success = true;
        this.result = result;
        this.error = null;
    }

    public BenderAsyncTaskResult(final Exception error) {
        this.success = false;
        this.error = error;
        this.result = null;
    }

    public boolean isSuccess() {
        return success;
    }

    public A getResult() {
        if (success) {
            return result;
        }
        throw new IllegalStateException("Result not present: " + error);
    }

    public Exception getError() {
        if (!success) {
            return this.error;
        }
        throw new IllegalStateException("Error not present: " + error);
    }

    public static final class Empty {

        private static Empty instance = new Empty();

        private static Empty getInstance() {
            return instance;
        }

        private Empty() {}
    }

}