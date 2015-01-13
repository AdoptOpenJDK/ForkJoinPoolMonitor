package java.util.concurrent;


public interface ForkJoinPoolMonitorMXBean {


    public double getArrivalRate();
    public long getNumberOfTasksSubmitted();
    public double getAverageTimeInSystem();

    public void clear();

}
