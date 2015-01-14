package java.util.concurrent;


public interface ForkJoinPoolMonitorMXBean {


    public double getArrivalRate();
    public long getNumberOfTasksSubmitted();
    public long getNumberOfTasksRetired();
    public double getAverageTimeInSystem();

    public void clear();

}
