package TASK.consensus;

import TASK.server.ServerState;

public class ElectionCoordinatorTimeout extends Thread {
    static boolean interrupt = false;
    ServerState serverState = ServerState.getInstance();

    public static void setInterrupt(boolean interrupt) {
        ElectionCoordinatorTimeout.interrupt = interrupt;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(10000);
            if(!interrupt){
                new BullyElection()
                        .startWaitingForAnswerMessage(serverState.getServerInfo(), serverState.getElectionAnswerTimeout());
                new BullyElection()
                        .startElection(serverState.getServerInfo(), serverState.getCandidateServerInfoList());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}