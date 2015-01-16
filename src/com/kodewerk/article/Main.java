package com.kodewerk.article;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPoolMonitor;
import java.util.concurrent.ForkJoinTask;

public class Main {

    public static void main(String[] args) throws IOException {
        Main main = new Main();
        ArrayList<String> list = new ArrayList<>();
        Files.lines( new File( "gc.log").toPath()).forEach(element -> list.add(element));
        System.out.println("-gc.log loaded--------------------------------------------------");
//        System.out.println( "Sequential Total Run Time   : " + ((double)main.sequentialParallelStream(10, list)/ 1000000.0d) + " ms");
//        main.reportAndClear();
        System.out.println( "Concurrent Total Run Time   : " + ((double)main.concurrentParallelStream(1, list)/ 1000000.0d) + " ms");
        main.reportAndClear();
//        System.out.println("Lambda Total Run Time    : " + ((double) main.lambdaParallelStream(10, list) / 1000000.0d) + " ms");
//        main.report();

    }

    public void reportAndClear() {
        report( true);
    }

    public void report() {
        report( false);
    }

    private void report( boolean doClear) {
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName(ForkJoinPoolMonitor.JMX_OBJECT_NAME_BASE + "0");
            try {

                System.out.println("\n-MXBean---------------------------------------------------------");
                Object attribute = server.getAttribute(name, "ArrivalRate");
                System.out.println("Arrival Rate              : " + attribute);

                attribute = server.getAttribute( name, "NumberOfTasksSubmitted");
                System.out.println("Number of Tasks Submitted : " + attribute);

                attribute = server.getAttribute( name, "NumberOfTasksRetired");
                System.out.println("Number of Tasks Retired   : " + attribute);

                attribute = server.getAttribute( name, "AverageTimeInSystem");
                System.out.println("Average Time In System    : " + (Double.parseDouble(attribute.toString()) / 1000000.0d) + " ms");

                if ( doClear) {
                    server.invoke( name, "clear" , null, null);
                }

            } catch (MBeanException e) {
                e.printStackTrace();
            } catch (AttributeNotFoundException e) {
                e.printStackTrace();
            } catch (ReflectionException e) {
                e.printStackTrace();
            }
        } catch (InstanceNotFoundException e) {
            e.printStackTrace();
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
        }
    }

    public long lambdaParallelStream(int repeat, ArrayList<String> list) throws IOException {

        DoubleSummaryStatistics applicationTimeStatistics = null;
        DoubleSummaryStatistics applicationStoppedTimeStatistics = null;
        long timer = System.nanoTime();
        long applicationTimeTimer;

        for (int i = 0; i < repeat; i++) {
            applicationTimeStatistics = new ApplicationTimeStatistics().calculateParallelStream(list);
        }
        applicationTimeTimer = System.nanoTime() - timer;

        long applicationStoppedTimeTimer = System.nanoTime();
        for (int i = 0; i < repeat; i++) {
            applicationStoppedTimeStatistics = new ApplicationStoppedTimeStatistics().calculateParallelStream(list);
        }
        applicationStoppedTimeTimer = System.nanoTime() - applicationStoppedTimeTimer;
        timer = System.nanoTime() - timer;

        System.out.println("\n-Lambda---------------------------------------------------------");
        System.out.println("Concurrent Stats         : " + applicationTimeStatistics);
        System.out.println("Concurrent Time (client) : " + applicationTimeTimer / 1000000.0d + " ms");
        System.out.println("Stopped Stats            : " + applicationStoppedTimeStatistics);
        System.out.println("Stopped Time (client)    : " + applicationStoppedTimeTimer / 1000000.0d + " ms");

        return timer;
    }

    public long concurrentParallelStream( int repeat, ArrayList<String> logEntries) {

        ForkJoinTask<DoubleSummaryStatistics> applicationTime;
        ForkJoinTask<DoubleSummaryStatistics> applicationStoppedTime;
        DoubleSummaryStatistics applicationStoppedTimeStatistics = null;
        DoubleSummaryStatistics applicationTimeStatistics = null;
        long timer = System.nanoTime();

        try {

            for ( int i = 0; i < repeat; i++) {
                applicationTime = ForkJoinPool.commonPool().submit(() -> new ApplicationTimeStatistics().calculateParallelStream(logEntries));
                applicationStoppedTime = ForkJoinPool.commonPool().submit(() -> new ApplicationStoppedTimeStatistics().calculateParallelStream(logEntries));
                applicationTimeStatistics = applicationTime.get();
                applicationStoppedTimeStatistics = applicationStoppedTime.get();
            }

            timer = System.nanoTime() - timer;
            System.out.println("\n-Concurrent-----------------------------------------------------");
            System.out.println("Concurrent Stats         : " + applicationTimeStatistics);
            System.out.println("Stopped Stats            : " + applicationStoppedTimeStatistics);
            System.out.println("Combined Time (client)   : " + ((double) timer) / 1000000.0d + " ms");

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return timer;
    }

    public long sequentialParallelStream(int repeat, ArrayList<String> logEntries) {

        DoubleSummaryStatistics applicationStoppedTimeStatistics = null;
        DoubleSummaryStatistics applicationTimeStatistics = null;

        long applicationTimeTimer, applicationStoppedTimeTimer = 0, timer = System.nanoTime();

        try {

            applicationTimeTimer = timer;
            for (int i = 0; i < repeat; i++) {
                applicationTimeStatistics = ForkJoinPool.commonPool().submit(() -> new ApplicationTimeStatistics().calculateParallelStream(logEntries)).get();
            }
            applicationTimeTimer = System.nanoTime() - applicationTimeTimer;

            applicationStoppedTimeTimer = System.nanoTime();
            for (int i = 0; i < repeat; i++) {
                applicationStoppedTimeStatistics = ForkJoinPool.commonPool().submit(() -> new ApplicationStoppedTimeStatistics().calculateParallelStream(logEntries)).get();
            }
            applicationStoppedTimeTimer = System.nanoTime() - applicationStoppedTimeTimer;
            timer = System.nanoTime() - timer;

            System.out.println("\n-Sequential-----------------------------------------------------");
            System.out.println("Concurrent Stats         : " + applicationTimeStatistics);
            System.out.println("Concurrent Time (client) : " + applicationTimeTimer / 1000000.0d + " ms");
            System.out.println("Stopped Stats            : " + applicationStoppedTimeStatistics);
            System.out.println("Stopped Time (client)    : " + applicationStoppedTimeTimer / 1000000.0d + " ms");

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return timer;

    }
}
