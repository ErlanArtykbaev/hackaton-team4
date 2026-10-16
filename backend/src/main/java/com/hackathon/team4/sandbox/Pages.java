package com.hackathon.team4.sandbox;

import com.hackathon.team4.sandbox.generated.model.Page;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * Reads every page of a paged sandbox list.
 */
public final class Pages {

    public static final int MAX_SIZE = 200;

    private Pages() {
    }

    public static <R, T> List<T> all(IntFunction<R> fetchPage, Function<R, List<T>> elements, Function<R, Page> metadata) {
        List<T> result = new ArrayList<>();
        int page = 0;
        while (true) {
            R response = fetchPage.apply(page);
            result.addAll(elements.apply(response));
            Page meta = metadata.apply(response);
            if (meta == null || meta.getTotalPages() == null || page + 1 >= meta.getTotalPages()) {
                return result;
            }
            page++;
        }
    }
}
