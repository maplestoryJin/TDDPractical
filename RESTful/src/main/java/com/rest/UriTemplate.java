package com.rest;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

interface UriTemplate {
    interface MatchResult extends Comparable<MatchResult> {
        String getMatched();

        String getRemaining();

        Map<String, String> getMatchedPathParameters();
    }

    Optional<MatchResult> match(String path);
}

class UriTemplateString implements UriTemplate {

    private static final String leftBracket = "\\{";
    private static final String rightBracket = "}";
    private static final String variableName = "\\w[\\w\\.-]*";
    private static final String nonBrackets = "[^\\{}]*";

    private static final Pattern VARIABLE = Pattern.compile(leftBracket + group(variableName) +
            group(":" + group(nonBrackets)) + "?" + rightBracket);
    private static final int variableNameGroup = 1;
    private static final int variablePatternGroup = 3;
    public static final String defaultVariablePattern = "([^/]+?)";

    private final Pattern pattern;
    private final List<String> variables = new ArrayList<>();
    private final int variableGroupStarFrom;

    public UriTemplateString(String template) {
        pattern = Pattern.compile(group(variable(template)) + "(/.*)?");
        variableGroupStarFrom = 2;
    }


    private String variable(String template) {
        return VARIABLE.matcher(template).replaceAll(result -> {
            String variableName = result.group(variableNameGroup);
            String variablePattern = result.group(variablePatternGroup);

            if (variables.contains(variableName)) throw new IllegalArgumentException("duplicate variable " + variableName);
            variables.add(variableName);
            return variablePattern == null ? defaultVariablePattern : group(variablePattern);
        });
    }

    @Override
    public Optional<MatchResult> match(String path) {
        Matcher matcher = pattern.matcher(path);
        if (!matcher.matches()) return Optional.empty();
        return Optional.of(new PathMatchResult(matcher));
    }

    private class PathMatchResult implements MatchResult {
        private final Matcher matcher;
        private final int count;
        private final Map<String, String> parameters = new HashMap<>();

        public PathMatchResult(Matcher matcher) {
            this.matcher = matcher;
            this.count =  matcher.groupCount();
            for (int i = 0; i < variables.size(); i++) {
                this.parameters.put((variables.get(i)), matcher.group(variableGroupStarFrom + i));
            }
        }

        @Override
        public String getMatched() {
            return matcher.group(1);
        }

        @Override
        public String getRemaining() {
            return matcher.group(count);
        }

        @Override
        public Map<String, String> getMatchedPathParameters() {
            return parameters;
        }

        @Override
        public int compareTo(MatchResult o) {
            return 0;
        }
    }

    private static String group(String pattern) {
        return "(" + pattern + ")";
    }
}
