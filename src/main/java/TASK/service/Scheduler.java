package TASK.service;

import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

public class Scheduler {

    private static Scheduler instance;
    private static org.quartz.Scheduler scheduler;

    private Scheduler() {
    }

    public static synchronized Scheduler getInstance() {
        if (instance == null) {
            instance = new Scheduler();
        }
        return instance;
    }

    public synchronized org.quartz.Scheduler getScheduler() {
        if (scheduler == null) {
            try {
                scheduler = StdSchedulerFactory.getDefaultScheduler();
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
        }
        return scheduler;
    }
}