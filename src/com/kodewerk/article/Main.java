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
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPoolMonitor;
import java.util.concurrent.ForkJoinTask;

public class Main {

    public static void main(String[] args) throws IOException {
        Main main = new Main();
        main.runWithLogEntriesPreloaded();
        main.runWithoutLogEntriesPreloaded();

    }

    private void runWithLogEntriesPreloaded() throws IOException {
        ArrayList<String> list = new ArrayList<>();
        System.out.println("-gc.log preloading----------------------------------------------");
        Files.lines( new File( "gc.log").toPath()).forEach(element -> list.add(element));
        System.out.println("-gc.log preloaded-----------------------------------------------");

        System.out.print("Lambda Parallel Total Run Time               : " + ((double) lambdaParallelStream(10, list) / 1000000.0d) + " ms");
        reportAndClear();
        System.out.print("Lambda Sequential Total Run Time          : " + ((double) lambdaSerialStream(10, list) / 1000000.0d) + " ms");
        reportAndClear();
        System.out.print("Stream Sequential Parallel Total Run Time : " + ((double) sequentialParallelStream(10, list) / 1000000.0d) + " ms");
        reportAndClear();
        System.out.print("Stream Concurrent Parallel Total Run Time : " + ((double) concurrentParallelStream(10, list) / 1000000.0d) + " ms");
        reportAndClear();
        System.out.print("Flood Concurrent Parallel                  : " + ((double) floodParallel(10, list) / 1000000.0d) + " ms");
        reportAndClear();
        System.out.print("Flood Serial Parallel                      : " + ((double) floodSerial(10, list) / 1000000.0d) + " ms");
        reportAndClear();
    }

    private void runWithoutLogEntriesPreloaded() throws IOException {
        System.out.println("-gc.log not preloaded-------------------------------------------\");");
        System.out.print("Lambda Parallel Total Run Time             : " + ((double) lambdaParallelStream(10, "gc.log") / 1000000.0d) + " ms");
        reportAndClear();
        System.out.print("Lambda Sequential Total Run Time          : " + ((double) lambdaSerialStream(10, "gc.log") / 1000000.0d) + " ms");
        reportAndClear();
        System.out.print("Stream Sequential Parallel Total Run Time : " + ((double) sequentialParallelStream(10, "gc.log") / 1000000.0d) + " ms");
        reportAndClear();
        System.out.print("Stream Concurrent Parallel Total Run Time : " + ((double) concurrentParallelStream(10, "gc.log") / 1000000.0d) + " ms");
        reportAndClear();
        System.out.print("Flood Concurrent Parallel                  : " + ((double) floodParallel(10, "gc.log") / 1000000.0d) + " ms");
        reportAndClear();
        System.out.print("Flood Serial Parallel                      : " + ((double) floodSerial(10, "gc.log") / 1000000.0d) + " ms");
        reportAndClear();
    }

    public void reportAndClear() {
        report( true, true, "0");
    }

    public void reportAndClear( String base) {
        report( true, true, base);
    }

    public void report() {
        report( true, false, "0");
    }
    public void report( String base) {
        report( true, false, base);
    }

    public void clear() {
        report( false, true, "0");
    }

    public void clear( String base) {
        report( false, true, base);
    }

    private void report( boolean doReport, boolean doClear, String base) {
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName(ForkJoinPoolMonitor.JMX_OBJECT_NAME_BASE + base);
            try {

                if ( doReport) {
                    System.out.println("\n-MXBean Report--------------------------------------------------");
                    Object attribute = server.getAttribute(name, "NumberOfTasksSubmitted");
                    System.out.println("Number of Tasks Submitted : " + attribute);

                    attribute = server.getAttribute(name, "NumberOfTasksRetired");
                    System.out.println("Number of Tasks Retired   : " + attribute);

                    double arrivalInterval = Double.parseDouble(server.getAttribute(name, "ArrivalIntervalInSeconds").toString());
                    System.out.println("Arrival Interval          : " + arrivalInterval + " seconds.");

                    double serviceTime = Double.parseDouble(server.getAttribute(name, "AverageTimeInSystem").toString()) / 1000000000.0d;
                    System.out.println("Average Time In System    : " + serviceTime + " seconds.");

                    System.out.println("Expected number of tasks  : " + serviceTime / arrivalInterval);
                }

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


    //
    public long lambdaParallelStream(int repeat, String name) throws IOException {

        DoubleSummaryStatistics applicationTimeStatistics = null;
        DoubleSummaryStatistics applicationStoppedTimeStatistics = null;
        long timer = System.nanoTime();
        long applicationTimeTimer;

        for (int i = 0; i < repeat; i++) {
            applicationTimeStatistics = new ApplicationTimeStatistics().calculateParallel(new File(name).toPath());
        }
        applicationTimeTimer = System.nanoTime() - timer;

        long applicationStoppedTimeTimer = System.nanoTime();
        for (int i = 0; i < repeat; i++) {
            applicationStoppedTimeStatistics = new ApplicationStoppedTimeStatistics().calculateParallel(new File(name).toPath());
        }
        applicationStoppedTimeTimer = System.nanoTime() - applicationStoppedTimeTimer;
        timer = System.nanoTime() - timer;

        System.out.println("\n-Lambda Concurrent----------------------------------------------");
        System.out.println("Concurrent Stats         : " + applicationTimeStatistics);
        System.out.println("Concurrent Time (client) : " + applicationTimeTimer / 1000000.0d + " ms");
        System.out.println("Stopped Stats            : " + applicationStoppedTimeStatistics);
        System.out.println("Stopped Time (client)    : " + applicationStoppedTimeTimer / 1000000.0d + " ms");

        return timer;
    }

    public long lambdaSerialStream(int repeat, String name) throws IOException {

        DoubleSummaryStatistics applicationTimeStatistics = null;
        DoubleSummaryStatistics applicationStoppedTimeStatistics = null;
        long timer = System.nanoTime();
        long applicationTimeTimer;

        for (int i = 0; i < repeat; i++) {
            applicationTimeStatistics = new ApplicationTimeStatistics().calculateSerial(new File(name).toPath());
        }
        applicationTimeTimer = System.nanoTime() - timer;

        long applicationStoppedTimeTimer = System.nanoTime();
        for (int i = 0; i < repeat; i++) {
            applicationStoppedTimeStatistics = new ApplicationStoppedTimeStatistics().calculateSerial(new File(name).toPath());
        }
        applicationStoppedTimeTimer = System.nanoTime() - applicationStoppedTimeTimer;
        timer = System.nanoTime() - timer;

        System.out.println("\n-Lambda Serial--------------------------------------------------");
        System.out.println("Concurrent Stats         : " + applicationTimeStatistics);
        System.out.println("Concurrent Time (client) : " + applicationTimeTimer / 1000000.0d + " ms");
        System.out.println("Stopped Stats            : " + applicationStoppedTimeStatistics);
        System.out.println("Stopped Time (client)    : " + applicationStoppedTimeTimer / 1000000.0d + " ms");

        return timer;
    }

    public long concurrentParallelStream( int repeat, String fileName) {

        ForkJoinTask<DoubleSummaryStatistics> applicationTime;
        ForkJoinTask<DoubleSummaryStatistics> applicationStoppedTime;
        DoubleSummaryStatistics applicationStoppedTimeStatistics = null;
        DoubleSummaryStatistics applicationTimeStatistics = null;
        long timer = System.nanoTime();

        try {

            for ( int i = 0; i < repeat; i++) {
                applicationTime = ForkJoinPool.commonPool().submit(() -> new ApplicationTimeStatistics().calculateParallel(new File(fileName).toPath()));
                applicationStoppedTime = ForkJoinPool.commonPool().submit(() -> new ApplicationStoppedTimeStatistics().calculateParallel(new File(fileName).toPath()));
                applicationTimeStatistics = applicationTime.get();
                applicationStoppedTimeStatistics = applicationStoppedTime.get();
            }

            timer = System.nanoTime() - timer;
            System.out.println("\n-Concurrent Parallel--------------------------------------------");
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

    public long sequentialParallelStream(int repeat, String fileName) {

        DoubleSummaryStatistics applicationStoppedTimeStatistics = null;
        DoubleSummaryStatistics applicationTimeStatistics = null;

        long applicationTimeTimer, applicationStoppedTimeTimer = 0, timer = System.nanoTime();

        try {

            applicationTimeTimer = timer;
            for (int i = 0; i < repeat; i++)
                applicationTimeStatistics = ForkJoinPool.commonPool().submit(() -> new ApplicationTimeStatistics().calculateParallel(new File(fileName).toPath())).get();
            applicationTimeTimer = System.nanoTime() - applicationTimeTimer;

            applicationStoppedTimeTimer = System.nanoTime();
            for (int i = 0; i < repeat; i++) {
                applicationStoppedTimeStatistics = ForkJoinPool.commonPool().submit(() -> new ApplicationStoppedTimeStatistics().calculateParallel(new File(fileName).toPath())).get();
            }
            applicationStoppedTimeTimer = System.nanoTime() - applicationStoppedTimeTimer;
            timer = System.nanoTime() - timer;

            System.out.println("\n-Sequential Parallel--------------------------------------------");
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

    public long floodParallel( int repeat, String fileName) {

        ForkJoinTask<DoubleSummaryStatistics>[] applicationTime = new ForkJoinTask[10];
        ForkJoinTask<DoubleSummaryStatistics>[] applicationStoppedTime = new ForkJoinTask[10];
        DoubleSummaryStatistics[] applicationStoppedTimeStatistics = new DoubleSummaryStatistics[10];
        DoubleSummaryStatistics[] applicationTimeStatistics = new DoubleSummaryStatistics[10];
        long timer = System.nanoTime();

        try {

            for ( int i = 0; i < repeat; i++) {
                applicationTime[i] = ForkJoinPool.commonPool().submit(() -> new ApplicationTimeStatistics().calculateParallel(new File(fileName).toPath()));
                applicationStoppedTime[i] = ForkJoinPool.commonPool().submit(() -> new ApplicationStoppedTimeStatistics().calculateParallel(new File(fileName).toPath()));
            }

            for ( int i = 0; i < repeat; i++) {
                applicationTimeStatistics[i] = applicationTime[i].get();
                applicationStoppedTimeStatistics[i] = applicationStoppedTime[i].get();
            }

            timer = System.nanoTime() - timer;
            System.out.println("\n-Concurrent Flood Parallel--------------------------------------");
            System.out.println("Combined Time (client)   : " + ((double) timer) / 1000000.0d + " ms");

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return timer;
    }

    public long floodSerial( int repeat, String fileName) {

        //ForkJoinPool smallerPool = new ForkJoinPool(4);
        ForkJoinTask<DoubleSummaryStatistics>[] applicationTime = new ForkJoinTask[10];
        ForkJoinTask<DoubleSummaryStatistics>[] applicationStoppedTime = new ForkJoinTask[10];
        DoubleSummaryStatistics[] applicationStoppedTimeStatistics = new DoubleSummaryStatistics[10];
        DoubleSummaryStatistics[] applicationTimeStatistics = new DoubleSummaryStatistics[10];
        long timer = System.nanoTime();

        try {

            for ( int i = 0; i < repeat; i++) {
                applicationTime[i] = ForkJoinPool.commonPool().submit(() -> new ApplicationTimeStatistics().calculateSerial(new File(fileName).toPath()));
                applicationStoppedTime[i] = ForkJoinPool.commonPool().submit(() -> new ApplicationStoppedTimeStatistics().calculateSerial(new File(fileName).toPath()));
            }

            for ( int i = 0; i < repeat; i++) {
                applicationTimeStatistics[i] = applicationTime[i].get();
                applicationStoppedTimeStatistics[i] = applicationStoppedTime[i].get();
            }

            timer = System.nanoTime() - timer;
            System.out.println("\n-Concurrent Flood Serial----------------------------------------");
            System.out.println("Combined Time (client)   : " + ((double) timer) / 1000000.0d + " ms");

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return timer;
    }

    //using list
    //
    public long lambdaParallelStream(int repeat, List logEntries) throws IOException {

        DoubleSummaryStatistics applicationTimeStatistics = null;
        DoubleSummaryStatistics applicationStoppedTimeStatistics = null;
        long timer = System.nanoTime();
        long applicationTimeTimer;

        for (int i = 0; i < repeat; i++) {
            applicationTimeStatistics = new ApplicationTimeStatistics().calculateParallel(logEntries);
        }
        applicationTimeTimer = System.nanoTime() - timer;

        long applicationStoppedTimeTimer = System.nanoTime();
        for (int i = 0; i < repeat; i++) {
            applicationStoppedTimeStatistics = new ApplicationStoppedTimeStatistics().calculateParallel(logEntries);
        }
        applicationStoppedTimeTimer = System.nanoTime() - applicationStoppedTimeTimer;
        timer = System.nanoTime() - timer;

        System.out.println("\n-Lambda Concurrent----------------------------------------------");
        System.out.println("Concurrent Stats         : " + applicationTimeStatistics);
        System.out.println("Concurrent Time (client) : " + applicationTimeTimer / 1000000.0d + " ms");
        System.out.println("Stopped Stats            : " + applicationStoppedTimeStatistics);
        System.out.println("Stopped Time (client)    : " + applicationStoppedTimeTimer / 1000000.0d + " ms");

        return timer;
    }

    public long lambdaSerialStream(int repeat, List logEntries) throws IOException {

        DoubleSummaryStatistics applicationTimeStatistics = null;
        DoubleSummaryStatistics applicationStoppedTimeStatistics = null;
        long timer = System.nanoTime();
        long applicationTimeTimer;

        for (int i = 0; i < repeat; i++) {
            applicationTimeStatistics = new ApplicationTimeStatistics().calculateSerial( logEntries);
        }
        applicationTimeTimer = System.nanoTime() - timer;

        long applicationStoppedTimeTimer = System.nanoTime();
        for (int i = 0; i < repeat; i++) {
            applicationStoppedTimeStatistics = new ApplicationStoppedTimeStatistics().calculateSerial(logEntries);
        }
        applicationStoppedTimeTimer = System.nanoTime() - applicationStoppedTimeTimer;
        timer = System.nanoTime() - timer;

        System.out.println("\n-Lambda Serial--------------------------------------------------");
        System.out.println("Concurrent Stats         : " + applicationTimeStatistics);
        System.out.println("Concurrent Time (client) : " + applicationTimeTimer / 1000000.0d + " ms");
        System.out.println("Stopped Stats            : " + applicationStoppedTimeStatistics);
        System.out.println("Stopped Time (client)    : " + applicationStoppedTimeTimer / 1000000.0d + " ms");

        return timer;
    }

    public long concurrentParallelStream( int repeat, List logEntries) {

        ForkJoinTask<DoubleSummaryStatistics> applicationTime;
        ForkJoinTask<DoubleSummaryStatistics> applicationStoppedTime;
        DoubleSummaryStatistics applicationStoppedTimeStatistics = null;
        DoubleSummaryStatistics applicationTimeStatistics = null;
        long timer = System.nanoTime();

        try {

            for ( int i = 0; i < repeat; i++) {
                applicationTime = ForkJoinPool.commonPool().submit(() -> new ApplicationTimeStatistics().calculateParallel(logEntries));
                applicationStoppedTime = ForkJoinPool.commonPool().submit(() -> new ApplicationStoppedTimeStatistics().calculateParallel(logEntries));
                applicationTimeStatistics = applicationTime.get();
                applicationStoppedTimeStatistics = applicationStoppedTime.get();
            }

            timer = System.nanoTime() - timer;
            System.out.println("\n-Concurrent Parallel--------------------------------------------");
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

    public long sequentialParallelStream(int repeat, List logEntries) {

        DoubleSummaryStatistics applicationStoppedTimeStatistics = null;
        DoubleSummaryStatistics applicationTimeStatistics = null;

        long applicationTimeTimer, applicationStoppedTimeTimer = 0, timer = System.nanoTime();

        try {

            applicationTimeTimer = timer;

            for (int i = 0; i < repeat; i++)
                applicationTimeStatistics = (DoubleSummaryStatistics)ForkJoinPool.commonPool().submit(() -> new ApplicationTimeStatistics().calculateParallel(logEntries)).get();
            applicationTimeTimer = System.nanoTime() - applicationTimeTimer;

            applicationStoppedTimeTimer = System.nanoTime();
            for (int i = 0; i < repeat; i++) {
                applicationStoppedTimeStatistics = (DoubleSummaryStatistics)ForkJoinPool.commonPool().submit(() -> new ApplicationStoppedTimeStatistics().calculateParallel(logEntries)).get();
            }

            applicationStoppedTimeTimer = System.nanoTime() - applicationStoppedTimeTimer;
            timer = System.nanoTime() - timer;

            System.out.println("\n-Sequential Parallel--------------------------------------------");
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

    public long floodParallel( int repeat, List logEntries) {

        ForkJoinTask<DoubleSummaryStatistics>[] applicationTime = new ForkJoinTask[10];
        ForkJoinTask<DoubleSummaryStatistics>[] applicationStoppedTime = new ForkJoinTask[10];
        DoubleSummaryStatistics[] applicationStoppedTimeStatistics = new DoubleSummaryStatistics[10];
        DoubleSummaryStatistics[] applicationTimeStatistics = new DoubleSummaryStatistics[10];
        long timer = System.nanoTime();

        try {

            for ( int i = 0; i < repeat; i++) {
                applicationTime[i] = ForkJoinPool.commonPool().submit(() -> new ApplicationTimeStatistics().calculateParallel(logEntries));
                applicationStoppedTime[i] = ForkJoinPool.commonPool().submit(() -> new ApplicationStoppedTimeStatistics().calculateParallel(logEntries));
            }

            for ( int i = 0; i < repeat; i++) {
                applicationTimeStatistics[i] = applicationTime[i].get();
                applicationStoppedTimeStatistics[i] = applicationStoppedTime[i].get();
            }

            timer = System.nanoTime() - timer;
            System.out.println("\n-Concurrent Flood Parallel--------------------------------------");
            System.out.println("Combined Time (client)   : " + ((double) timer) / 1000000.0d + " ms");

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return timer;
    }

    public long floodSerial( int repeat, List<String> logEntries) {

        //ForkJoinPool smallerPool = new ForkJoinPool(4);
        ForkJoinTask<DoubleSummaryStatistics>[] applicationTime = new ForkJoinTask[10];
        ForkJoinTask<DoubleSummaryStatistics>[] applicationStoppedTime = new ForkJoinTask[10];
        DoubleSummaryStatistics[] applicationStoppedTimeStatistics = new DoubleSummaryStatistics[10];
        DoubleSummaryStatistics[] applicationTimeStatistics = new DoubleSummaryStatistics[10];
        long timer = System.nanoTime();

        try {

            for ( int i = 0; i < repeat; i++) {
                applicationTime[i] = ForkJoinPool.commonPool().submit(() -> new ApplicationTimeStatistics().calculateSerial(logEntries));
                applicationStoppedTime[i] = ForkJoinPool.commonPool().submit(() -> new ApplicationStoppedTimeStatistics().calculateSerial(logEntries));
            }

            for ( int i = 0; i < repeat; i++) {
                applicationTimeStatistics[i] = applicationTime[i].get();
                applicationStoppedTimeStatistics[i] = applicationStoppedTime[i].get();
            }

            timer = System.nanoTime() - timer;
            System.out.println("\n-Concurrent Flood Serial----------------------------------------");
            System.out.println("Combined Time (client)   : " + ((double) timer) / 1000000.0d + " ms");

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return timer;
    }
}
