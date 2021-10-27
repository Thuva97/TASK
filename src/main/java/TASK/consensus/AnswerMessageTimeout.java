package TASK.consensus;

import TASK.server.ServerState;

public class AnswerMessageTimeout extends Thread {
    static boolean interrupt = false;
    ServerState serverState = ServerState.getInstance();

    public static void setInterrupt(boolean interrupt) {
        AnswerMessageTimeout.interrupt = interrupt;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(10000);
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
