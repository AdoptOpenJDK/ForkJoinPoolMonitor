package com.kodewerk.article;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.DoubleSummaryStatistics;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPoolMonitor;
import java.util.concurrent.ForkJoinTask;

public class Main {

    public static void main(String[] args) {
        Main main = new Main();
        main.sequential( 10);
        main.overload( 10);

        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName(ForkJoinPoolMonitor.JMX_OBJECT_NAME_BASE + "0");
            try {

                Object attribute = server.getAttribute( name, "ArrivalRate");
                System.out.println("Arrival Rate : " + attribute);

                attribute = server.getAttribute( name, "NumberOfTasksSubmitted");
                System.out.println("Number of Tasks Submitted : " + attribute);

                attribute = server.getAttribute( name, "AverageTimeInSystem");
                System.out.println("Average Time In System : " + attribute);

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

    public void overload( int repeat) {

        long start;
        try {

            for ( int i = 0; i < repeat; i++) {
                start = System.nanoTime();
                ForkJoinTask<DoubleSummaryStatistics> applicationStoppedTime = ForkJoinPool.commonPool().submit(() -> new ApplicationStoppedTimeStatistics().calculate(new File("gc.log")));
                ForkJoinTask<DoubleSummaryStatistics> applicationTime = ForkJoinPool.commonPool().submit(() -> new ApplicationTimeStatistics().calculate(new File("gc.log")));
                System.out.println("\n-Concurrent-----------------------------------------------------");
                System.out.println("Concurrent Stats         : " + applicationTime.get());
                System.out.println("Stopped Stats            : " + applicationStoppedTime.get());
                long timer = System.nanoTime() - start;
                System.out.println("Tasks                    : " + ForkJoinPool.commonPool().getMonitor().getNumberOfTasksSubmitted());
                System.out.println("Run time (MXBean)        : " + ForkJoinPool.commonPool().getMonitor().getAverageTimeInSystem() / 1000000.0d + " ms");
                System.out.println("Total Time (client)      : " + ((double) timer) / 1000000.0d + " ms");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void sequential(int repeat) {

        ForkJoinTask<DoubleSummaryStatistics> applicationStoppedTime;
        ForkJoinTask<DoubleSummaryStatistics> applicationTime;

        long start;
        DoubleSummaryStatistics stats;
        double timer, totalTime = 0.0d;

        try {

            for (int i = 0; i < repeat; i++){
                start = System.nanoTime();
                applicationTime = ForkJoinPool.commonPool().submit(() -> new ApplicationTimeStatistics().calculate(new File("gc.log")));
                stats = applicationTime.get();
                timer = (double) (System.nanoTime() - start);
                totalTime += timer;
                System.out.println("\n-Sequential-----------------------------------------------------");
                System.out.println("Concurrent Stats         : " + stats);
                System.out.println("Tasks                    : " + ForkJoinPool.commonPool().getMonitor().getNumberOfTasksSubmitted());
                System.out.println("Run time (MXBean)        : " + ForkJoinPool.commonPool().getMonitor().getAverageTimeInSystem() / 1000000.0d + " ms");
                System.out.println("Concurrent Time (client) : " + timer / 1000000.0d + " ms");


                start = System.nanoTime();
                applicationStoppedTime = ForkJoinPool.commonPool().submit(() -> new ApplicationStoppedTimeStatistics().calculate(new File("gc.log")));
                stats = applicationStoppedTime.get();
                timer = (double) (System.nanoTime() - start);
                totalTime += timer;
                System.out.println("\n-Sequential-----------------------------------------------------");
                System.out.println("Stopped Stats            : " + stats);
                System.out.println("Tasks                    : " + ForkJoinPool.commonPool().getMonitor().getNumberOfTasksSubmitted());
                System.out.println("Run time (MXBean)        : " + ForkJoinPool.commonPool().getMonitor().getAverageTimeInSystem() / 1000000.0d + " ms");
                System.out.println("Stopped Time (client)    : " + timer / 1000000.0d + " ms");
            }

            System.out.println( "Total Run Time           : " + ( totalTime/ 1000000.0d) + " ms");

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
}
