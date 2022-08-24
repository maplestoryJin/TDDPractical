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

class PathTemplate implements UriTemplate {


    private final Pattern pattern;
    private final PathVariables pathVariables = new PathVariables();
    private final int variableGroupStarFrom;

    public PathTemplate(String template) {
        pattern = Pattern.compile(group(pathVariables.template(template)) + "(/.*)?");
        variableGroupStarFrom = 2;
    }


    @Override
    public Optional<MatchResult> match(String path) {
        Matcher matcher = pattern.matcher(path);
        if (!matcher.matches()) return Optional.empty();
        return Optional.of(new PathMatchResult(matcher, pathVariables));
    }

    class PathVariables implements Comparable<PathVariables> {
        private static final String leftBracket = "\\{";
        private static final String rightBracket = "}";
        private static final String variableName = "\\w[\\w\\.-]*";
        private static final String nonBrackets = "[^\\{}]*";

        private static final Pattern VARIABLE = Pattern.compile(leftBracket + group(variableName) +
                group(":" + group(nonBrackets)) + "?" + rightBracket);
        private static final int variableNameGroup = 1;
        private static final int variablePatternGroup = 3;
        private static final String defaultVariablePattern = "([^/]+?)";


        private final List<String> variables = new ArrayList<>();
        private int specificPatternCount = 0;

        private String template(String template) {
            return VARIABLE.matcher(template).replaceAll(this::replace);
        }

        private String replace(java.util.regex.MatchResult result) {
            String variableName = result.group(variableNameGroup);
            String variablePattern = result.group(variablePatternGroup);

            if (variables.contains(variableName))
                throw new IllegalArgumentException("duplicate variable " + variableName);

            variables.add(variableName);
            if (variablePattern != null) {
                specificPatternCount++;
                return group(variablePattern);
            }
            return defaultVariablePattern;
        }

        public Map<String, String> extract(Matcher matcher) {
            Map<String, String> parameters = new HashMap<>();
            for (int i = 0; i < variables.size(); i++) {
                parameters.put((variables.get(i)), matcher.group(variableGroupStarFrom + i));
            }
            return parameters;
        }


        @Override
        public int compareTo(PathVariables o) {
            if (variables.size() > o.variables.size()) return -1;
            if (variables.size() < o.variables.size()) return 1;
            return Integer.compare(o.specificPatternCount, specificPatternCount);
        }
    }


    private class PathMatchResult implements MatchResult {
        private final Matcher matcher;
        private final Map<String, String> parameters;
        private int matchLiteralCount;
        private PathVariables pathVariables;

        public PathMatchResult(Matcher matcher, PathVariables pathVariables) {
            this.matcher = matcher;
            this.pathVariables = pathVariables;
            this.parameters = pathVariables.extract(matcher);
            matchLiteralCount = matcher.group(1).length() - parameters.values().stream().map(String::length)
                    .reduce(0, Integer::sum);
        }

        @Override
        public String getMatched() {
            return matcher.group(1);
        }

        @Override
        public String getRemaining() {
            return matcher.group(matcher.groupCount());
        }

        @Override
        public Map<String, String> getMatchedPathParameters() {
            return parameters;
        }

        @Override
        public int compareTo(MatchResult o) {
            PathMatchResult result = (PathMatchResult) o;
            if (matchLiteralCount > result.matchLiteralCount) return -1;
            if (matchLiteralCount < result.matchLiteralCount) return 1;
            return pathVariables.compareTo(result.pathVariables);
        }
    }

    private static String group(String pattern) {
        return "(" + pattern + ")";
    }
}
