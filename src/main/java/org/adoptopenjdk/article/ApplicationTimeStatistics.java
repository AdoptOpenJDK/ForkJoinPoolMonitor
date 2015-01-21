package org.adoptopenjdk.article;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ApplicationTimeStatistics {

    //2.037: Application time: 1.9571120 seconds
    private static Pattern applicationTimePattern = Pattern.compile("(\\d+\\.\\d+): Application time: (\\d+\\.\\d+)");

    public ApplicationTimeStatistics() {}

    public DoubleSummaryStatistics calculateParallel( List<String> logEntries) throws IOException {
        return  logEntries.parallelStream().
                           map(applicationTimePattern::matcher).
                           filter(Matcher::find).
                           mapToDouble(matcher -> Double.parseDouble(matcher.group(2))).
                           summaryStatistics();
    }

    public DoubleSummaryStatistics calculateSerial( List<String> logEntries) throws IOException {
        return  logEntries.stream().
                           map(applicationTimePattern::matcher).
                           filter(Matcher::find).
                           mapToDouble(matcher -> Double.parseDouble(matcher.group(2))).
                           summaryStatistics();
    }

    public DoubleSummaryStatistics calculateParallel( Path path) throws IOException {
        return  Files.lines(path).parallel().
                map(applicationTimePattern::matcher).
                filter(Matcher::find).
                mapToDouble(matcher -> Double.parseDouble(matcher.group(2))).
                summaryStatistics();
    }

    public DoubleSummaryStatistics calculateSerial( Path path) throws IOException {
        return  Files.lines(path).
                map(applicationTimePattern::matcher).
                filter(Matcher::find).
                mapToDouble(matcher -> Double.parseDouble(matcher.group(2))).
                summaryStatistics();
    }
}
