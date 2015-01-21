package java.util.concurrent;


import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ForkJoinPoolMonitor implements ForkJoinPoolMonitorMXBean {

    private static Logger LOGGER = Logger.getLogger( ForkJoinPoolMonitor.class.getName());
    private static AtomicInteger instanceCount = new AtomicInteger(0);

    public static String JMX_OBJECT_NAME_BASE = "com.jclarity:type=forkjoinpool,instance=";

    {
        try {
            ObjectName name = new ObjectName( JMX_OBJECT_NAME_BASE + instanceCount.getAndIncrement());
            ManagementFactory.getPlatformMBeanServer().registerMBean( this, name);

        } catch (InstanceAlreadyExistsException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        } catch (MBeanRegistrationException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        } catch (NotCompliantMBeanException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        } catch (MalformedObjectNameException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }
    }

    private ConcurrentHashMap<ForkJoinTask,Long> monitoredTasks = new ConcurrentHashMap<>();
    private long startTime = System.nanoTime();
    private LongAdder numberOfTasksSubmitted = new LongAdder();
    private LongAdder taskRetiredCount = new LongAdder();
    private volatile double timeInSystem = 0.0d;

    public ForkJoinPoolMonitor() {}

    /*****
     *
     * Monitoring methods
     *
     */
    public void submitTask(ForkJoinTask task) {
        monitoredTasks.put(task, System.nanoTime());
        numberOfTasksSubmitted.increment();
    }

    public void retireTask( ForkJoinTask task) {
        try {
            this.timeInSystem += (double)(System.nanoTime() - monitoredTasks.remove(task));
            this.taskRetiredCount.increment();
        } catch (NullPointerException npe) {/*silly but NPE is throws if element isn't in map */}
    }

    @Override
    public long getNumberOfTasksSubmitted() {
        return numberOfTasksSubmitted.longValue();
    }

    @Override
    public long getNumberOfTasksRetired() { return taskRetiredCount.longValue(); }

    @Override
    public double getArrivalIntervalInSeconds() {
        return (((double)( System.nanoTime() - startTime)) / 1000000000.0d) / numberOfTasksSubmitted.doubleValue();
    }

    @Override
    public double getAverageTimeInSystem() {
        double localTasksRetiredCount = this.taskRetiredCount.doubleValue();
        double localTimeInSystem = this.timeInSystem;
        if ( localTasksRetiredCount == 0L)
            return 0.0d;
        return localTimeInSystem / localTasksRetiredCount;
    }

    @Override
    public double averageNumberOfTasksInSystem() {
        double localAverageTimeInSystem = getAverageTimeInSystem();
        double localArrivalIntervalInSeconds = getArrivalIntervalInSeconds();
        if ( localArrivalIntervalInSeconds == 0.0d) {
            return 0.0d;
        }
        return localAverageTimeInSystem / localArrivalIntervalInSeconds;
    }

    @Override
    public void clear() {
        this.numberOfTasksSubmitted.reset();
        this.taskRetiredCount.reset();
        this.timeInSystem = 0.0d;
        this.startTime = System.nanoTime();
        monitoredTasks.clear();
    }
}
