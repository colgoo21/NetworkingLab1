package NetworkLab;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class ServerClientHandler implements Runnable {
        // Maintain data about the client serviced by this thread
        ClientConnectionData client;

        public ServerClientHandler(ClientConnectionData client) {
            this.client = client;
        }

        /**
         * Broadcasts a message to all clients connected to the server.
         */
        public void broadcast(String msg) {
            String m = "";
            try {
                if (msg.startsWith("WELCOME")){
                    m = String.format("%s has joined", msg.substring(8));
                }
                else if (msg.startsWith("EXIT")){
                    m = String.format("%s has left", msg.substring(4));
                    broadcastMembers();
                }
                else{
                    m = msg;
                }
                System.out.println("Broadcasting -- " + msg);
                synchronized (ChatServer.clientList) {
                    for (ClientConnectionData c : ChatServer.clientList){
                        c.getOut().println(msg);
                    }
                }
            } catch (Exception ex) {
                System.out.println("broadcast caught exception: " + ex);
                ex.printStackTrace();
            }

        }

        public void broadcastChat(String msg){
            String m = "";
            try{
                ArrayList<ClientConnectionData> ccdArraryList = new ArrayList<>();
                System.out.println("Broadcasting -- " + msg);
                for(int i=0; i<ChatServer.clientList.size(); i++){
                    if (ChatServer.clientList.get(i).getUserName() != client.getUserName()){
                        ccdArraryList.add(ChatServer.clientList.get(i));
                    }
                }
                m = String.format("%s:%s", client.getUserName(), msg.substring(6+client.getUserName().length()));
                for(ClientConnectionData c: ccdArraryList){
                    c.getOut().println(m);

                }
            } catch (Exception ex){
                System.out.println("broadcast caught exception: " + ex);
            }
        }

        public void broadcastPrivate(String userName, String msg){
            String m = "";
            String chat = msg.substring(5+userName.length());
            try {

                System.out.println("Privately Broadcasting -- " + msg);
                ClientConnectionData c = new ClientConnectionData(null, null, null, null);
                for(int i=0; i<ChatServer.clientList.size(); i++){
                    if(ChatServer.clientList.get(i).getUserName().equals(userName)){
                        c = ChatServer.clientList.get(i);
                        m = String.format("%s (privately): %s", client.getUserName(), chat);
                        break;
                    }
                }
                c.getOut().println(msg);
                client.getOut().println(msg);
            } catch (Exception ex) {
                System.out.println("broadcast caught exception: " + ex);
                ex.printStackTrace();
            }
        }

    public void broadcastRing(String userName, String msg){
        String m = "";
        try {

            System.out.println("Privately Broadcasting -- " + msg);
            ClientConnectionData c = new ClientConnectionData(null, null, null, null);
            for(int i=0; i<ChatServer.clientList.size(); i++){
                if(ChatServer.clientList.get(i).getUserName().equals(userName)){
                    c = ChatServer.clientList.get(i);
                    m = String.format("\007%s rang you!", client.getUserName());
                    break;
                }
            }
            c.getOut().println(m);
        } catch (Exception ex) {
            System.out.println("broadcast caught exception: " + ex);
            ex.printStackTrace();
        }
    }
    public void broadcastClient(String thing){
        ClientConnectionData c = new ClientConnectionData(client.getSocket(), client.getInput(), client.getOut(), client.getName());
        if (thing.startsWith("SUBMITNAME")) {
            c.getOut().println("Enter your name: ");
        }
    }
    public void broadcastMembers(){
        ArrayList<String> members = new ArrayList<>();
        //getting a list of all of the usernames
        for(ClientConnectionData c: ChatServer.clientList){
            members.add(c.getUserName());
        }
        //putting all the names in a nice string
        String memberString = "Members: \n";
        for(String name : members){
            memberString += name + "\n";
        }
        //putting it on the server
        for (ClientConnectionData c: ChatServer.clientList){
            c.getOut().println(memberString);
        }
    }

        @Override
        public void run() {
            try {
                broadcastClient("SUBMITNAME");
                BufferedReader in = client.getInput();
                String str;
                while(true){
                    in = client.getInput();
                    str = "*" + in.readLine();
                    if(str.startsWith("NAME") && str.substring(4, str.length()) != null && !(ChatServer.clientList.contains(str.substring(4, str.length())))){
                        String userName = str.substring(5, str.length()).trim();
                        client.setUserName(userName);
                        break;
                    }
                    else if(str.startsWith("*") && str.substring (2, str.length()-1) != null && !(ChatServer.clientList.contains(str.substring(4, str.length())))){
                        String userName = str.substring(1, str.length()).trim();
                        client.setUserName(userName);
                        break;
                    }
                    else{
                        broadcastClient("SUBMITNAME");
                    }
                }

                //notify all that client has joined
                broadcast(String.format("WELCOME %s", client.getUserName()));
                broadcastMembers();

                String incoming = "";

                while( (incoming = in.readLine()) != null) {
                    if (incoming.startsWith("CHAT")) {
                        String chat = incoming.substring(4).trim();
                        if (chat.length() > 0) {
                            String msg = String.format("CHAT %s %s", client.getUserName(), chat);
                            broadcast(msg);
                        }
                    }
                    else if(incoming.startsWith("*")){
                        String chat = incoming.substring(1).trim();
                        if(chat.length() > 0){
                            String msg = String.format("CHAT %s %s", client.getUserName(), chat);
                            broadcast(msg);
                        }
                    }
                    else if(incoming.startsWith("PCHAT")){
                        //split incoming by @, this gives me an array of all the users to send the message to; the last element is the last person + the message
                        String[] incomingReceivers = incoming.split("@");

                        //split the last element of incomingReceivers by whitespace so that the first element is the last user and the rest are the message
                        String[] lastElement = incomingReceivers[incomingReceivers.length-1].split(" ");
                        String chat = "";

                        //store the message in chat by adding each element from lastElement starting from 1
                        for(int i=1; i<lastElement.length; i++){
                            chat = chat + lastElement[i] + " ";
                        }

                        //more efficient method of finding the list of users, previous method included extra whitespace and elements that shouldn't have been there
                        List<String> receivers = new ArrayList<String>();

                        //go through incoming until you find an "@", nested for loop starting from there until it reaches " ", add everything in between to receivers
                        for(int i=0; i<incoming.length(); i++){
                            if(incoming.substring(i, i+1).equals("@")){
                                for(int j=i+1; j<incoming.length(); j++){
                                    if(incoming.substring(j, j+1).equals(" ")){
                                        receivers.add(incoming.substring(i+1, j));
                                        break;
                                    }
                                }
                            }
                        }

                        //for loop through receivers to privately message all people that were mentioned
                        if(chat.length() > 0){
                            String msg = "";
                            for(int i=0; i<receivers.size(); i++){
                                msg = String.format("PCHAT %s %s %s", client.getUserName(), receivers.get(i), chat);
                                broadcastPrivate(receivers.get(i), msg);
                            }
                        }
                    }
                    else if(incoming.startsWith("/ring")){
                        String receiver = "";
                        for(int i=0; i< ChatServer.clientList.size(); i++){
                            if(incoming.startsWith(ChatServer.clientList.get(i).getUserName(), 6)){
                                receiver = ChatServer.clientList.get(i).getUserName();
                                break;
                            }
                        }
                        String chat = "\007";
                        String msg = String.format("PCHAT %s %s", client.getUserName(), chat);
                        broadcastRing(receiver, msg);

                    }
                    else if(incoming.startsWith("@")){

                        //delete; old code
                        String receiver = "";
                        for(int i=0; i< ChatServer.clientList.size(); i++){
                            if(incoming.startsWith(ChatServer.clientList.get(i).getUserName(), 1)){
                                receiver = ChatServer.clientList.get(i).getUserName();
                                break;
                            }
                        }

                        //split incoming by @, this gives me an array of all the users to send the message to; the last element is the last person + the message
                        String[] incomingReceivers = incoming.split("@");

                        //split the last element of incomingReceivers by whitespace so that the first element is the last user and the rest are the message
                        String[] lastElement = incomingReceivers[incomingReceivers.length-1].split(" ");
                        String chat = "";

                        //store the message in chat by adding each element from lastElement starting from 1
                        for(int i=1; i<lastElement.length; i++){
                            chat = chat + lastElement[i] + " ";
                        }

                        //more efficient method of finding the list of users, previous method included extra whitespace and elements that shouldn't have been there
                        List<String> receivers = new ArrayList<String>();

                        //go through incoming until you find an "@", nested for loop starting from there until it reaches " ", add everything in between to receivers
                        for(int i=0; i<incoming.length(); i++){
                            if(incoming.substring(i, i+1).equals("@")){
                                for(int j=i+1; j<incoming.length(); j++){
                                    if(incoming.substring(j, j+1).equals(" ")){
                                        receivers.add(incoming.substring(i+1, j));
                                        break;
                                    }
                                }
                            }
                        }

                        //for loop through receivers to privately message all people that were mentioned
                        if(chat.length() > 0){
                            String msg = "";
                            for(int i=0; i<receivers.size(); i++){
                                msg = String.format("PCHAT %s %s %s", client.getUserName(), receivers.get(i), chat);
                                broadcastPrivate(receivers.get(i), msg);
                            }
                        }
                    }
                    else if (incoming.startsWith("QUIT") || incoming.startsWith("/quit")){
                        broadcast(String.format("EXIT %s", client.getUserName()));
                        break;
                    }
                    else if (incoming.startsWith("/whoishere")){
                        broadcastMembers();
                    }
                }
            } catch (Exception ex) {
                if (ex instanceof SocketException) {
                    System.out.println("Caught socket ex for " +
                            client.getName());
                } else {
                    System.out.println(ex);
                    ex.printStackTrace();
                }
            } finally {
                //Remove client from clientList, notify all
                synchronized (ChatServer.clientList) {
                    ChatServer.clientList.remove(client);
                }
                //System.out.println(client.getName() + " has left.");
                //broadcast(String.format("EXIT %s", client.getUserName()));
                try {
                    client.getSocket().close();
                } catch (IOException ex) {}

            }
        }

    }
