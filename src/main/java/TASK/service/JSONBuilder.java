package TASK.service;

import TASK.server.ServerState;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import TASK.server.ServerInfo;
import TASK.model.ChatRoom;
import TASK.model.LocalChatRoom;

import java.time.Instant;
import java.util.HashMap;
import java.util.stream.Collectors;

public class JSONBuilder {

    private static JSONBuilder instance = null;

    private JSONBuilder() {
    }

    public static synchronized JSONBuilder getInstance() {
        if (instance == null) instance = new JSONBuilder();
        return instance;
    }

    public String serverChange(String approved, String serverId) {
        // {"type" : "serverchange", "approved" : "true", "serverid" : "s2"}
        JSONObject jj = new JSONObject();
        jj.put("type", "serverchange");
        jj.put("approved", approved);
        jj.put("serverid", serverId);
        return jj.toJSONString();
    }

    public String route(String joiningRoomId, String host, Integer port) {
        // {"type" : "route", "roomid" : "jokes", "host" : "122.134.2.4", "port" : "4445"}
        JSONObject jj = new JSONObject();
        jj.put("type", "route");
        jj.put("roomid", joiningRoomId);
        jj.put("host", host);
        jj.put("port", port.toString());
        return jj.toJSONString();
    }


    public String message(String identity, String content) {
        // {"type" : "message", "identity" : "Adel", "content" : "Hi there!"}
        JSONObject jj = new JSONObject();
        jj.put("type", "message");
        jj.put("identity", identity);
        jj.put("content", content);
        return jj.toJSONString();
    }

    public String deleteRoom(String roomId, String approved) {
        // {"type" : "deleteroom", "roomid" : "jokes", "approved" : "true"}
        JSONObject jj = new JSONObject();
        jj.put("type", "deleteroom");
        jj.put("roomid", roomId);
        jj.put("approved", approved);
        return jj.toJSONString();
    }

    public String deleteRoomPeers(String roomId) {
        // {"type" : "deleteroom", "serverid" : "s1", "roomid" : "jokes"}
        JSONObject jj = new JSONObject();
        jj.put("type", "deleteroom");
        jj.put("serverid", ServerState.getInstance().getServerInfo().getServerId());
        jj.put("roomid", roomId);
        return jj.toJSONString();
    }

    public String who(String room) {
        JSONObject jj = new JSONObject();
        //{ "type" : "roomcontents", "roomid" : "jokes", "identities" : ["Adel","Chenhao","Maria"], "owner" : "Adel" }
        jj.put("type", "roomcontents");
        jj.put("roomid", room);
        LocalChatRoom localChatRoom = ServerState.getInstance().getLocalChatRooms().get(room);
        JSONArray ja = new JSONArray();
        ja.addAll(localChatRoom.getMembers());
        jj.put("identities", ja);
        jj.put("owner", localChatRoom.getOwner());
        return jj.toJSONString();
    }

    public String listRooms() {
        //{ "type" : "roomlist", "rooms" : ["MainHall-s1", "MainHall-s2", "jokes"] }
        JSONObject jj = new JSONObject();
        jj.put("type", "roomlist");

        JSONArray ja = ServerState.getInstance().getLocalChatRooms().values().stream()
                .map(ChatRoom::getChatRoomId)
                .collect(Collectors.toCollection(JSONArray::new));

        ja.addAll(ServerState.getInstance().getGlobalChatRooms().values().stream()
                .map(ChatRoom::getChatRoomId)
                .collect(Collectors.toList()));

        jj.put("rooms", ja);

        return jj.toJSONString();
    }


    public String newIdentityResp(String approve) {
        JSONObject jj = new JSONObject();
        jj.put("type", "newidentity");
        jj.put("approved", approve);
        return jj.toJSONString();
    }

    public String roomChange(String former, String roomId, String identity) {
        // {"type" : "roomchange", "identity" : "Maria", "former" : "jokes", "roomid" : "jokes"}
        JSONObject jj = new JSONObject();
        jj.put("type", "roomchange");
        jj.put("identity", identity);
        jj.put("former", former);
        jj.put("roomid", roomId);
        return jj.toJSONString();
    }

    // Server to server
    public static JSONObject getElection(String source) {
        // {"option": "election", "source": "s1"}
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("option", "election");
        jsonObject.put("source", source);
        return jsonObject;
    }

    //send who is leader
    public static JSONObject getCoordinator(String leader) {
        // {"option": "coordinator", "leader": "s3"}
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("option", "coordinator");
        jsonObject.put("leader", leader);
        return jsonObject;
    }

    //ok message
    public static JSONObject getOk(String sender) {
        // {"option": "ok", "sender": "s1"}
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("option", "ok");
        jsonObject.put("sender", sender);
        return jsonObject;
    }

    //heart beat to leader
    public static JSONObject getHeartbeat( String sender) {
        // {"option": "heartbeat", "sender": "s1"}
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("option", "heartbeat");
        jsonObject.put("sender", sender);
        return jsonObject;
    }




    // Heartbeat

//    public String notifyServerDownMessage(String serverId) {
//        // {"type":"notifyserverdown", "serverid":"s2"}
//        JSONObject jj = new JSONObject();
//        jj.put(Protocol.type.toString(), Protocol.notifyserverdown.toString());
//        jj.put(Protocol.serverid.toString(), serverId);
//        return jj.toJSONString();
//    }
//
//    public String serverUpMessage() {
//        JSONObject jj = new JSONObject();
//        jj.put(Protocol.type.toString(), Protocol.serverup.toString());
//        jj.put(Protocol.serverid.toString(), serverInfo.getServerId());
//        jj.put(Protocol.address.toString(), serverInfo.getAddress());
//        jj.put(Protocol.port.toString(), serverInfo.getPort());
//        jj.put(Protocol.managementport.toString(), serverInfo.getManagementPort());
//        return jj.toJSONString();
//    }
//
//    public String startElectionMessage(String serverId, String serverAddress, Long serverPort, Long
//            serverManagementPort) {
//        JSONObject jj = new JSONObject();
//        jj.put(Protocol.type.toString(), Protocol.startelection.toString());
//        jj.put(Protocol.serverid.toString(), serverId);
//        jj.put(Protocol.address.toString(), serverAddress);
//        jj.put(Protocol.port.toString(), String.valueOf(serverPort));
//        jj.put(Protocol.managementport.toString(), String.valueOf(serverManagementPort));
//        return jj.toJSONString();
//    }
//
//    public String electionAnswerMessage(String serverId, String serverAddress, Integer serverPort, Integer
//            serverManagementPort) {
//        JSONObject jj = new JSONObject();
//        jj.put(Protocol.type.toString(), Protocol.answerelection.toString());
//        jj.put(Protocol.serverid.toString(), serverId);
//        jj.put(Protocol.address.toString(), serverAddress);
//        jj.put(Protocol.port.toString(), String.valueOf(serverPort));
//        jj.put(Protocol.managementport.toString(), String.valueOf(serverManagementPort));
//        return jj.toJSONString();
//    }
//
//    public String setCoordinatorMessage(String serverId, String serverAddress, Integer serverPort, Integer
//            serverManagementPort) {
//        JSONObject jj = new JSONObject();
//        jj.put(Protocol.type.toString(), Protocol.coordinator.toString());
//        jj.put(Protocol.serverid.toString(), serverId);
//        jj.put(Protocol.address.toString(), serverAddress);
//        jj.put(Protocol.port.toString(), String.valueOf(serverPort));
//        jj.put(Protocol.managementport.toString(), String.valueOf(serverManagementPort));
//        return jj.toJSONString();
//    }
//
//    public String gossipMessage(String serverId, HashMap<String, Integer> heartbeatCountList) {
//        // {"type":"gossip","serverid":"1","heartbeatcountlist":{"1":0,"2":1,"3":1,"4":2}}
//        JSONObject jj = new JSONObject();
//        jj.put(Protocol.type.toString(), Protocol.gossip.toString());
//        jj.put(Protocol.serverid.toString(), serverId);
//        jj.put(Protocol.heartbeatcountlist.toString(), heartbeatCountList);
//        return jj.toJSONString();
//    }
//
//    public String startVoteMessage(String serverId, String suspectServerId) {
//        JSONObject jj = new JSONObject();
//        jj.put(Protocol.type.toString(), Protocol.startvote.toString());
//        jj.put(Protocol.serverid.toString(), serverId);
//        jj.put(Protocol.suspectserverid.toString(), suspectServerId);
//        return jj.toJSONString();
//    }
//
//    public String answerVoteMessage(String suspectServerId, String vote, String votedBy){
//        // {"type":"answervote","suspectserverid":"1","vote":"YES", "votedby":"1"}
//        JSONObject jj = new JSONObject();
//        jj.put(Protocol.type.toString(), Protocol.answervote.toString());
//        jj.put(Protocol.suspectserverid.toString(), suspectServerId);
//        jj.put(Protocol.votedby.toString(), votedBy);
//        jj.put(Protocol.vote.toString(), vote);
//        return jj.toJSONString();
//    }
//
//    public String iAmUpMessage(String serverId, String serverAddress, Integer serverPort, Integer
//            serverManagementPort) {
//        // {"type":"iamup", "serverid":"1", "address":"localhost", "port":"4444", "managementport":"5555"}
//        JSONObject jj = new JSONObject();
//        jj.put(Protocol.type.toString(), Protocol.iamup.toString());
//        jj.put(Protocol.serverid.toString(), serverId);
//        jj.put(Protocol.address.toString(), serverAddress);
//        jj.put(Protocol.port.toString(), String.valueOf(serverPort));
//        jj.put(Protocol.managementport.toString(), String.valueOf(serverManagementPort));
//        return jj.toJSONString();
//    }
//
//    public String viewMessage(String coordinatorId, String coordinatorAddress, Integer coordinatorPort, Integer
//            coordinatorManagementPort) {
//        // {"type":"viewelection", "currentcoordinatorid":"1", "currentcoordinatoraddress":"localhost",
//        //      "currentcoordinatorport":"4444", "currentcoordinatormanagementport":"5555"}
//        JSONObject jj = new JSONObject();
//        jj.put(Protocol.type.toString(), Protocol.viewelection.toString());
//        jj.put(Protocol.currentcoordinatorid.toString(), coordinatorId);
//        jj.put(Protocol.currentcoordinatoraddress.toString(), coordinatorAddress);
//        jj.put(Protocol.currentcoordinatorport.toString(), String.valueOf(coordinatorPort));
//        jj.put(Protocol.currentcoordinatormanagementport.toString(), String.valueOf(coordinatorManagementPort));
//        return jj.toJSONString();
//    }
//
//    public String nominationMessage() {
//        JSONObject jj = new JSONObject();
//        jj.put(Protocol.type.toString(), Protocol.nominationelection.toString());
//        return jj.toJSONString();
//    }

}

