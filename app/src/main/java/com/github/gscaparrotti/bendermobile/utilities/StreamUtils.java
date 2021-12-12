package com.github.gscaparrotti.bendermobile.utilities;

import java9.util.Spliterators;
import java9.util.stream.Stream;
import java9.util.stream.StreamSupport;

public class StreamUtils {

    public static <T> Stream<T> stream(final Iterable<T> iterable) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterable.iterator(), 0), false);
    }
}
