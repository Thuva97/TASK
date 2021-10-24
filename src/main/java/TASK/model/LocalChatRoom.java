package TASK.model;

import TASK.client.Client;

import java.util.ArrayList;
import java.util.List;

public class LocalChatRoom extends ChatRoom {
    private String owner;
    private List<Client> clients = new ArrayList<>();

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public synchronized void addMember(Client client) {
        clients.add(client);
    }

    public synchronized void removeMember(Client client) {
        clients.remove(client);
    }

    public synchronized List<String> getMembers() {
        List<String> members = new ArrayList<>();
        for ( Client c : clients ){
            members.add(c.getClientID());
        }
        return members;
    }
}
