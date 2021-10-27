package TASK.consensus;

import TASK.server.ServerState;

public class AnswerMessageTimeout extends Thread {
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
                new BullyElection().setupNewCoordinator(
                        serverState.getServerInfo(),
                        serverState.getSubordinateServerInfoList());

                System.out.println("Election was finalized and the new leader is : " + serverState.getServerInfo().getServerId());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
