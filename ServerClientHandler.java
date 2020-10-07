import java.io.BufferedReader;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;

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
            }
            else{
                m = msg;
            }
            System.out.println("Broadcasting -- " + msg);
            synchronized (ChatServer.clientList) {
                for (ClientConnectionData c : ChatServer.clientList){
                    c.getOut().println(m);
                    // c.getOut().flush();
                }
            }
        } catch (Exception ex) {
            System.out.println("broadcast caught exception: " + ex);
            ex.printStackTrace();
        }

    }

    public void broadcastChat(String msg){
        String m = "";
        System.out.println(ChatServer.clientList.toString());
        try {
            ArrayList<ClientConnectionData> ccdArrayList = new ArrayList<>(); // meant to add every ccd that isn't the client's
            System.out.println("Broadcasting -- " + msg);
            for(int i=0; i<ChatServer.clientList.size(); i++){
                if(ChatServer.clientList.get(i).getUserName() != client.getUserName()){
                    ccdArrayList.add(ChatServer.clientList.get(i));
                }
            }
            m = String.format("%s:%s", client.getUserName(), msg.substring(6+client.getUserName().length())); // chat (4) plus the client username plus the two spaces
            for (ClientConnectionData c: ccdArrayList) {
                c.getOut().println(m);
            }
        } catch (Exception ex) {
            System.out.println("broadcast caught exception: " + ex);
            ex.printStackTrace();
        }
    }

    public void broadcastPrivate(String userName, String msg){
        String m = "";
        System.out.println(userName);
        System.out.println(ChatServer.clientList.toString());
        try {

            System.out.println("Privately Broadcasting -- " + msg);
            ClientConnectionData c = new ClientConnectionData(null, null, null, null);
            for(int i=0; i<ChatServer.clientList.size(); i++){
                if(ChatServer.clientList.get(i).getUserName().equals(userName)){
                    c = ChatServer.clientList.get(i);
                    m = String.format("%s (private): %s", userName, msg);
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

    @Override
    public void run() {
        try {
            broadcastClient("SUBMITNAME");
            BufferedReader in = client.getInput();
            String str;
            while(true){
                in = client.getInput();
                str = in.readLine();
                if(str.startsWith("NAME") && str.substring(4, str.length()) != null && !(ChatServer.clientList.contains(str.substring(4, str.length())))){
                    String userName = str.substring(5, str.length()).trim();
                    client.setUserName(userName);
                    break;
                }
                else if (str.startsWith("*") && str.substring (2,str.length()-1) != null && !(ChatServer.clientList.contains(str.substring(4, str.length())))){
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


            String incoming = "";

            while( (incoming = in.readLine()) != null) {
                if (incoming.startsWith("CHAT")) {
                    String chat = incoming.substring(4).trim();
                    if (chat.length() > 0) {
                        String msg = String.format("CHAT %s %s", client.getUserName(), chat);
                        broadcastChat(msg);
                    }
                }
                else if (incoming.startsWith("*")){
                    String chat = incoming.substring(1).trim();
                    if (chat.length() > 0) {
                        String msg = String.format("CHAT %s %s", client.getUserName(), chat);
                        broadcastChat(msg);
                    }
                }
                else if(incoming.startsWith("PCHAT")) {
                    String receiver = "";
                    for (int i = 0; i < ChatServer.clientList.size(); i++) {
                        if (incoming.startsWith(ChatServer.clientList.get(i).getUserName(), 6)) {
                            receiver = ChatServer.clientList.get(i).getUserName();
                            break;
                        }
                    }
                    String chat = incoming.substring(6 + receiver.length()).trim();
                    if (chat.length() > 0) {
                        String msg = String.format("PCHAT %s %s", client.getUserName(), chat);
                        broadcastPrivate(receiver, msg);
                    }
                }
                else if (incoming.startsWith("QUIT") || incoming.startsWith("/quit")){
                    broadcast(String.format("EXIT %s", client.getUserName()));
                    break;
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
            System.out.println(client.getName() + " has left.");
            broadcast(String.format("EXIT %s", client.getUserName()));
            try {
                client.getSocket().close();
            } catch (IOException ex) {}

        }
    }

}
