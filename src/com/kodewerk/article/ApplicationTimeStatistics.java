package com.kodewerk.article;

import java.io.IOException;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ApplicationTimeStatistics {

    //2.037: Application time: 1.9571120 seconds
    private static Pattern applicationTimePattern = Pattern.compile("(\\d+\\.\\d+): Application time: (\\d+\\.\\d+)");

    public ApplicationTimeStatistics() {}

    public DoubleSummaryStatistics calculate( ArrayList<String> logEntries) throws IOException {
        return  logEntries.parallelStream().
                map(applicationTimePattern::matcher).
                filter(Matcher::find).
                mapToDouble(matcher -> Double.parseDouble(matcher.group(2))).
                summaryStatistics();
    }
}
