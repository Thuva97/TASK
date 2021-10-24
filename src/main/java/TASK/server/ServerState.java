package TASK.server;

import TASK.client.Client;
import TASK.client.ClientThread;
import TASK.model.GlobalChatRoom;
import TASK.model.LocalChatRoom;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerState {
    private static ServerState instance;
    private ServerInfo serverInfo;
    private int coordinationPort;
    private ConcurrentMap<String, LocalChatRoom> localChatRooms;
    private ConcurrentMap<String, GlobalChatRoom> globalChatRooms;
    private ConcurrentMap<String, ClientThread> connectedClients;
    private LocalChatRoom mainHall;
    private ServerInfo coordinator;
    private AtomicBoolean ongoingElection;
    private ConcurrentMap<String, ServerInfo> servers;


    public static synchronized ServerState getInstance() {
        if (instance == null) {
            instance = new ServerState();
        }
        return instance;
    }

    public ConcurrentMap<String, ClientThread> getConnectedClients() {
        return connectedClients;
    }

    public ConcurrentMap<String, LocalChatRoom> getLocalChatRooms() {
        return localChatRooms;
    }

    public ConcurrentMap<String, GlobalChatRoom> getGlobalChatRooms() {
        return globalChatRooms;
    }

    public boolean isClientExisted(String Id) {
        return connectedClients.containsKey(Id);
    }

    public boolean isRoomExistedGlobally(String roomId) {
        return localChatRooms.containsKey(roomId) || globalChatRooms.containsKey(roomId);
    }

    public boolean isRoomExistedLocally(String roomId) {
        return localChatRooms.containsKey(roomId);
    }

    public boolean isRoomExistedRemotely(String roomId) {
        return globalChatRooms.containsKey(roomId);
    }

    public ServerInfo getServerInfo() {
        return serverInfo;
    }

    public void setServerInfo(ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }

    public LocalChatRoom getMainHall() {
        return mainHall;
    }

    public void setMainHall(LocalChatRoom mainHall) {
        this.mainHall = mainHall;
    }

    public ServerInfo getCoordinator() {
        return coordinator;
    }

    public void setCoordinator(ServerInfo coordinator) {
        this.coordinator = coordinator;
    }

    public boolean isOngoingElection() {
        return ongoingElection.get();
    }

    public void setOngoingElection(boolean ongoingElection) {
        this.ongoingElection.set(ongoingElection);
    }

    public ConcurrentMap<String, ServerInfo> getServers() {
        return servers;
    }

    public void setServers(ConcurrentMap<String, ServerInfo> servers) {
        this.servers = servers;
    }
}
