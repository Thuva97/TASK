package TASK.consensus;

import TASK.server.ServerState;

public class ElectionCoordinatorTimeout extends Thread {
    private boolean interrupt = false;
    ServerState serverState = ServerState.getInstance();

    public void setInterrupt(boolean interrupt) {
        this.interrupt = interrupt;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(5000);
            if(!interrupt){
                new BullyElection()
                        .startWaitingForAnswerMessage();
                new BullyElection()
                        .startElection(serverState.getServerInfo(), serverState.getCandidateServerInfoList());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}