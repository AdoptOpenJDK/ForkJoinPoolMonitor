package org.adoptopenjdk.management;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ApplicationStoppedTimeStatistics {

    private static Pattern applicationStoppedTimePattern = Pattern.compile("(\\d+\\.\\d+): Total time for which application threads were stopped: (\\d+\\.\\d+)");

    //public ApplicationStoppedTimeStatistics() {}

    public DoubleSummaryStatistics calculateParallel( List<String> logEntries) throws IOException {
        return  logEntries.parallelStream().
                map(applicationStoppedTimePattern::matcher).
                filter(Matcher::find).
                mapToDouble(matcher -> Double.parseDouble(matcher.group(2))).
                summaryStatistics();
    }

    public DoubleSummaryStatistics calculateSerial( List<String> logEntries) throws IOException {
        return  logEntries.stream().
                map(applicationStoppedTimePattern::matcher).
                filter(Matcher::find).
                mapToDouble(matcher -> Double.parseDouble(matcher.group(2))).
                summaryStatistics();
    }

    public DoubleSummaryStatistics calculateParallel( Path path) throws IOException {
        return  Files.lines(path).parallel().
                        map(applicationStoppedTimePattern::matcher).
                        filter(Matcher::find).
                        mapToDouble(matcher -> Double.parseDouble(matcher.group(2))).
                        summaryStatistics();
    }

    public DoubleSummaryStatistics calculateSerial( Path path) throws IOException {
        return  Files.lines(path).
                map(applicationStoppedTimePattern::matcher).
                filter(Matcher::find).
                mapToDouble(matcher -> Double.parseDouble(matcher.group(2))).
                summaryStatistics();
    }
}
