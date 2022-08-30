package com.rest;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

interface UriHandler {
    UriTemplate getUriTemplate();
}

class UriHandlers {

    public static <T extends UriHandler> Optional<T> match(String path, List<T> handlers, Function<UriTemplate.MatchResult, Boolean> matchFunction) {
        return matched(path, handlers, matchFunction).map(Result::handler);
    }

    public static <T extends UriHandler, R> Optional<R> mapMatched(String path, List<T> handlers,
                                                              BiFunction<Optional<UriTemplate.MatchResult>, T, Optional<R>> mapper) {
        return matched(path, handlers, r -> true).flatMap(r -> mapper.apply(r.matched(), r.handler()));
    }

    private static <T extends UriHandler> Optional<Result<T>> matched(String path, List<T> handlers, Function<UriTemplate.MatchResult, Boolean> matchFunction) {
        return handlers.stream().map(m -> new Result<>(m.getUriTemplate().match(path), m, matchFunction))
                .filter(Result::isMatched).sorted()
                .findFirst();
    }

    public static <T extends UriHandler> Optional<T> match(String path, List<T> handlers) {
        return match(path, handlers, r -> true);
    }

    private record Result<T extends UriHandler>(Optional<UriTemplate.MatchResult> matched,
                                               T handler, Function<UriTemplate.MatchResult, Boolean> matchFunction) implements Comparable<Result<T>> {

        public boolean isMatched() {
            return matched.map(matchFunction::apply).orElse(false);
        }

        @Override
        public int compareTo(Result<T> o) {
            return matched.flatMap(x -> o.matched.map(x::compareTo)).orElse(0);
        }
    }
}
