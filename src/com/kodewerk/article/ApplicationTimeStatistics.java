package com.kodewerk.article;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.DoubleSummaryStatistics;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ApplicationTimeStatistics {

    //2.037: Application time: 1.9571120 seconds
    private static Pattern applicationTimePattern = Pattern.compile("(\\d+\\.\\d+): Application time: (\\d+\\.\\d+)");

    public ApplicationTimeStatistics() {}

    public DoubleSummaryStatistics calculate( File gcLogFile) throws IOException {
        return Files.lines(gcLogFile.toPath()).parallel().
                map(applicationTimePattern::matcher).
                filter(Matcher::find).
                mapToDouble(matcher -> Double.parseDouble(matcher.group(2))).
                summaryStatistics();
    }
}
