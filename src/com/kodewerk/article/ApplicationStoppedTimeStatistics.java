package com.kodewerk.article;

import java.io.IOException;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ApplicationStoppedTimeStatistics {

    private static Pattern applicationStoppedTimePattern = Pattern.compile("(\\d+\\.\\d+): Total time for which application threads were stopped: (\\d+\\.\\d+)");

    public ApplicationStoppedTimeStatistics() {}

    public DoubleSummaryStatistics calculate( ArrayList<String> logEntries) throws IOException {
        return  logEntries.parallelStream().
                map(applicationStoppedTimePattern::matcher).
                filter(Matcher::find).
                mapToDouble(matcher -> Double.parseDouble(matcher.group(2))).
                summaryStatistics();
    }
}
