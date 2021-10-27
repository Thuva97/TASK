package TASK.consensus;

import TASK.server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.*;

import java.util.concurrent.atomic.AtomicBoolean;

@DisallowConcurrentExecution
public class ElectionCoordinatorMessageTimeoutFinalizer implements Job, InterruptableJob {

    private ServerState serverState = ServerState.getInstance();
    private AtomicBoolean interrupted = new AtomicBoolean(false);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (!interrupted.get()) {
            // no coordinator message was received from a higher priority server
            // therefore restart the election
            new BullyElection()
                    .startElection(serverState.getServerInfo(), serverState.getCandidateServerInfoList());

            new BullyElection()
                    .startWaitingForAnswerMessage(serverState.getServerInfo(), serverState.getElectionAnswerTimeout());
        }

    }

    @Override
    public void interrupt() throws UnableToInterruptJobException {
        System.out.println("Job was interrupted...");
        interrupted.set(true);
    }

//    private static final Logger logger = LogManager.getLogger(ElectionCoordinatorMessageTimeoutFinalizer.class);
}
