package java.lang.management;


public interface ForkJoinPoolMonitorMXBean {


    public double getArrivalIntervalInSeconds();
    public long getNumberOfTasksSubmitted();
    public long getNumberOfTasksRetired();
    public double getAverageTimeInSystem();
    public double averageNumberOfTasksInSystem();

    public void clear();

}
