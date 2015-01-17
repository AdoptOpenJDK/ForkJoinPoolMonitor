package java.util.concurrent;


import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicInteger;
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
    private volatile long numberOfTasksSubmitted = 0L;
    private volatile double timeInSystem = 0.0d;
    private volatile long taskRetiredCount = 0L;

    public ForkJoinPoolMonitor() {}

    /*****
     *
     * Monitoring methods
     *
     */

    public void submitTask(ForkJoinTask task) {
        //System.out.println("Registering : " + Thread.currentThread().getName());
        monitoredTasks.put( task, System.nanoTime());
        numberOfTasksSubmitted++;
    }

    public void retireTask( ForkJoinTask task) {
        //System.out.println("Retiring : " + Thread.currentThread().getName());
        try {
            long submitTime = monitoredTasks.remove(task);
            this.timeInSystem += (double)(System.nanoTime() - submitTime);
            this.taskRetiredCount++;
        } catch (NullPointerException npe) {/*silly but NPE is throws if element isn't in map */}
    }

    @Override
    public long getNumberOfTasksSubmitted() {
        return numberOfTasksSubmitted;
    }

    @Override
    public long getNumberOfTasksRetired() { return taskRetiredCount; }

    @Override
    public double getArrivalIntervalInSeconds() {
        System.out.println( (System.nanoTime() - startTime)/ 1000000000L);
        return (double)numberOfTasksSubmitted / (((double)( System.nanoTime() - startTime)) / 1000000000.0d);
    }

    @Override
    public double getAverageTimeInSystem() {
        double localTasksRetiredCount = (double)this.taskRetiredCount;
        double localTimeInSystem = this.timeInSystem;
        if ( localTasksRetiredCount == 0L)
            return 0.0d;
        return localTimeInSystem / localTasksRetiredCount;
    }

    @Override
    public void clear() {
        this.startTime = System.nanoTime();
        this.numberOfTasksSubmitted = 0L;
        this.timeInSystem = 0.0d;
        this.taskRetiredCount = 0L;
        this.startTime = System.nanoTime();
        monitoredTasks.clear();
    }
}
