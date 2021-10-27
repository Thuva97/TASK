package TASK.consensus;

import TASK.server.ServerInfo;
import TASK.server.ServerState;
import TASK.service.JSONBuilder;
import TASK.service.ServerCommunication;
import org.quartz.Scheduler;

import java.util.List;

public class BullyElection {

//    private static final Logger logger = LogManager.getLogger(BullyElection.class);
    protected JSONBuilder jsonBuilder;
    protected ServerCommunication serverCommunication;
    protected ServerState serverState;
    protected Scheduler scheduler;

    public BullyElection() {
        this.jsonBuilder = JSONBuilder.getInstance();
        this.serverCommunication = new ServerCommunication();
        this.serverState = ServerState.getInstance();
    }

    public void startElection(ServerInfo proposingCoordinator, List<ServerInfo> candidatesList) {
        System.out.println("Starting election...");
        serverState.setOngoingElection(true);
        String proposingCoordinatorServerId = proposingCoordinator.getServerId();
        String proposingCoordinatorAddress = proposingCoordinator.getAddress();
        Long proposingCoordinatorPort = Long.valueOf(proposingCoordinator.getClientPort());
        Long proposingCoordinatorManagementPort = Long.valueOf(proposingCoordinator.getServerPort());
        String startElectionMessage = jsonBuilder
                .startElectionMessage(proposingCoordinatorServerId, proposingCoordinatorAddress,
                        proposingCoordinatorPort, proposingCoordinatorManagementPort);
        serverCommunication.relaySelectedPeers(candidatesList, startElectionMessage);
    }

    public void startWaitingForCoordinatorMessage() {

        ElectionCoordinatorTimeout electionCoordinatorTimeout = new ElectionCoordinatorTimeout();
        serverState.setElectionCoordinatorTimeoutFinalizer(electionCoordinatorTimeout);
        serverState.getAnswerMessageTimeoutFinalizer().start();

    }

    public void startWaitingForAnswerMessage() {
        AnswerMessageTimeout answerMessageTimeout = new AnswerMessageTimeout();
        serverState.setAnswerMessageTimeoutFinalizer(answerMessageTimeout);
        serverState.getAnswerMessageTimeoutFinalizer().start();

    }

    public void replyAnswerForElectionMessage(ServerInfo requestingCandidate, ServerInfo me) {
        System.out.println("Replying answer for the election start message from : " + requestingCandidate.getServerId());
        String electionAnswerMessage = jsonBuilder
                .electionAnswerMessage(me.getServerId(), me.getAddress(), me.getClientPort(), me.getServerPort());
        serverCommunication.commPeerOneWay(requestingCandidate, electionAnswerMessage);
    }

    public void setupNewCoordinator(ServerInfo newCoordinator, List<ServerInfo> subordinateServerInfoList) {
        System.out.println("Informing subordinates about the new coordinator...");
        // inform subordinates about the new coordinator
        String newCoordinatorServerId = newCoordinator.getServerId();
        String newCoordinatorAddress = newCoordinator.getAddress();
        Integer newCoordinatorServerPort = newCoordinator.getClientPort();
        Integer newCoordinatorServerManagementPort = newCoordinator.getServerPort();
        String setCoordinatorMessage = jsonBuilder
                .setCoordinatorMessage(newCoordinatorServerId, newCoordinatorAddress, newCoordinatorServerPort,
                        newCoordinatorServerManagementPort);
        serverCommunication.relaySelectedPeers(subordinateServerInfoList, setCoordinatorMessage);

        // accept the new coordinator
        acceptNewCoordinator(newCoordinator);
    }

    public void acceptNewCoordinator(ServerInfo newCoordinator) {
        serverState.setCoordinator(newCoordinator);
        serverState.setOngoingElection(false);
        System.out.println("Accepting new coordinator : " + newCoordinator.getServerId());
    }

    public void stopWaitingForCoordinatorMessage() {
        serverState.getElectionCoordinatorTimeoutFinalizer().setInterrupt(true);
    }

    public void stopWaitingForAnswerMessage() {
        serverState.getAnswerMessageTimeoutFinalizer().setInterrupt(true);
    }

    public void stopElection(ServerInfo stoppingServer) {
        System.out.println("Stopping election...");
        stopWaitingForAnswerMessage();
        stopWaitingForCoordinatorMessage();
    }


}
