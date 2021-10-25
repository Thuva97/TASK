package TASK.server;

import TASK.model.RemoteChatRoom;
import TASK.model.LocalChatRoom;
import TASK.model.RemoteUserInfo;
import TASK.model.UserInfo;
import TASK.service.ServerPriorityComparator;

import java.rmi.Remote;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerState {
    private static ServerState instance;
    private ServerInfo serverInfo;
    private ConcurrentMap<String, LocalChatRoom> localChatRooms;
    private ConcurrentMap<String, RemoteChatRoom> remoteChatRooms;
    private ConcurrentMap<String, UserInfo> connectedClients;
    private ConcurrentMap<String, RemoteUserInfo> remoteClients;
    private ServerInfo coordinator;
    private AtomicBoolean ongoingElection;
    private ConcurrentMap<String, ServerInfo> serverInfoMap;
    private ConcurrentNavigableMap<String, ServerInfo> candidateServerInfoMap;
    private Map<String, ServerInfo> subordinateServerInfoMap;
    private ConcurrentMap<String, Integer> aliveMap;
    private AtomicBoolean stopRunning;
    private Long electionAnswerTimeout;
    private Long electionCoordinatorTimeout;

    private ServerState() {
        aliveMap = new ConcurrentHashMap<>();
        connectedClients = new ConcurrentHashMap<>();
        localChatRooms = new ConcurrentHashMap<>();
        remoteChatRooms = new ConcurrentHashMap<>();
        serverInfoMap = new ConcurrentHashMap<>();
        candidateServerInfoMap = new ConcurrentSkipListMap<>(new ServerPriorityComparator());
        subordinateServerInfoMap = new ConcurrentHashMap<>();
        ongoingElection = new AtomicBoolean(false);
        stopRunning = new AtomicBoolean(false);
    }
    public static synchronized ServerState getInstance() {
        if (instance == null) {
            instance = new ServerState();
        }
        return instance;
    }

    public synchronized void initServerState(String serverId) {
        serverInfo = serverInfoMap.get(serverId);
/*
        serverInfo = serverInfoList.stream()
                .filter(e -> e.getServerId().equalsIgnoreCase(serverId))
                .findFirst()
                .get();
*/
    }

    public synchronized ServerInfo getServerInfoById(String serverId) {
        return serverInfoMap.get(serverId);
/*
        return serverInfoList.stream()
                .filter(e -> e.getServerId().equalsIgnoreCase(serverId))
                .findFirst()
                .get();
*/
    }

    public synchronized ServerInfo getServerInfo() {
        return serverInfo;
    }

    public synchronized List<ServerInfo> getServerInfoList() {
        //return serverInfoList;
        return new ArrayList<>(serverInfoMap.values());
    }

    public synchronized List<ServerInfo> getCandidateServerInfoList() {
        return new ArrayList<>(candidateServerInfoMap.values());
    }

    public synchronized List<ServerInfo> getSubordinateServerInfoList() {
        return new ArrayList<>(subordinateServerInfoMap.values());
    }

    public synchronized void setServerInfoList(List<ServerInfo> serverInfoList) {
        //this.serverInfoList = serverInfoList;
        for (ServerInfo serverInfo : serverInfoList) {
            addServer(serverInfo);
        }
    }

    public synchronized void addServer(ServerInfo serverInfo) {
        ServerInfo me = getServerInfo();
        if (null != serverInfo) {
            if (null != me) {
                if (new ServerPriorityComparator().compare(me.getServerId(), serverInfo.getServerId()) > 0) {
                    candidateServerInfoMap.put(serverInfo.getServerId(), serverInfo);
                } else if (new ServerPriorityComparator().compare(me.getServerId(), serverInfo.getServerId()) < 0) {
                    subordinateServerInfoMap.put(serverInfo.getServerId(), serverInfo);
                }
            }
            serverInfoMap.put(serverInfo.getServerId(), serverInfo);
        }


/*
        for (int i = 0; i < serverInfoList.size(); i++) {
            ServerInfo s = serverInfoList.get(i);
            if (s.getServerId().equalsIgnoreCase(serverInfo.getServerId())) {
                logger.info("Server " + serverInfo.getServerId() + " already exist.");
            } else {
                if (!Objects.equals(s.getPort(), serverInfo.getPort())) {
                    logger.info("Adding server " + serverInfo.getServerId() + " to server list.");
                    serverInfoList.add(serverInfo);
                }
            }
        }
*/
    }

    public synchronized void setupConnectedServers() {
        for (ServerInfo server : getServerInfoList()) {
            addServer(server);
        }
    }

    public synchronized void removeServer(String serverId) {
        serverInfoMap.remove(serverId);
    }

    public ConcurrentMap<String, Integer> getAliveMap() {
        return aliveMap;
    }


    public ConcurrentMap<String, LocalChatRoom> getLocalChatRooms() {
        return localChatRooms;
    }

    public ConcurrentMap<String, RemoteChatRoom> getRemoteChatRooms() {
        return remoteChatRooms;
    }

    public boolean isUserExisted(String userId) {
        return connectedClients.containsKey(userId) || remoteChatRooms.containsKey(userId) ;
    }

    public boolean isRoomExistedGlobally(String roomId) {
        return localChatRooms.containsKey(roomId) || remoteChatRooms.containsKey(roomId);
    }

    public boolean isRoomExistedLocally(String roomId) {
        return localChatRooms.containsKey(roomId);
    }

    public boolean isRoomExistedRemotely(String roomId) {
        return remoteChatRooms.containsKey(roomId);
    }

    public void stopRunning(boolean state) {
        stopRunning.set(state);
    }

    public boolean isStopRunning() {
        return stopRunning.get();
    }

    public synchronized ServerInfo getCoordinator() {
        return coordinator;
    }

    public synchronized void setCoordinator(ServerInfo coordinator) {
        addServer(coordinator);
        this.coordinator = coordinator;
    }


    public boolean isOngoingElection() {
        return ongoingElection.get();
    }

    public void setOngoingElection(boolean ongoingElection) {
        this.ongoingElection.set(ongoingElection);
    }

    public ConcurrentMap<String, UserInfo> getConnectedClients() {
        return connectedClients;
    }

    public ConcurrentMap<String, RemoteUserInfo> getRemoteClients() {
        return remoteClients;
    }

    public Long getElectionAnswerTimeout() {
        return electionAnswerTimeout;
    }

    public void setElectionAnswerTimeout(Long electionAnswerTimeout) {
        this.electionAnswerTimeout = electionAnswerTimeout;
    }

    public Long getElectionCoordinatorTimeout() {
        return electionCoordinatorTimeout;
    }

    public void setElectionCoordinatorTimeout(Long electionCoordinatorTimeout) {
        this.electionCoordinatorTimeout = electionCoordinatorTimeout;
    }

    public void removeRemoteChatRoomsByServerId(String serverId) {
        for (String entry : remoteChatRooms.keySet()) {
            RemoteChatRoom remoteChatRoom = remoteChatRooms.get(entry);
            if (remoteChatRoom.getManagingServer().equalsIgnoreCase(serverId)) {
                remoteChatRooms.remove(entry);
            }
        }
    }
}
