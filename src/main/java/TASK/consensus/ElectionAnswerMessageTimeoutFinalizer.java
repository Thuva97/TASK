package TASK.consensus;

import TASK.server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.*;

import java.util.concurrent.atomic.AtomicBoolean;

@DisallowConcurrentExecution
public class ElectionAnswerMessageTimeoutFinalizer implements Job, InterruptableJob {

    private ServerState serverState = ServerState.getInstance();
    private AtomicBoolean interrupted = new AtomicBoolean(false);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (!interrupted.get()) {

            new BullyElection().setupNewCoordinator(
                    serverState.getServerInfo(),
                    serverState.getSubordinateServerInfoList());

            logger.debug("Election was finalized and the new leader is : " + serverState.getServerInfo().getServerId());
        }

/*
        try {
            jobExecutionContext.getScheduler().deleteJob(jobExecutionContext.getJobDetail().getKey());
        } catch (SchedulerException e) {
            logger.error("Unable to delete the job from scheduler : " + e.getLocalizedMessage());
        }
*/
    }

    @Override
    public void interrupt() throws UnableToInterruptJobException {
        logger.debug("Job was interrupted...");
        interrupted.set(true);
    }

    private static final Logger logger = LogManager.getLogger(ElectionAnswerMessageTimeoutFinalizer.class);
}
