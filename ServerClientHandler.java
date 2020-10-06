import java.io.BufferedReader;
import java.io.IOException;
import java.net.SocketException;

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
            try {
                System.out.println("Broadcasting -- " + msg);
                synchronized (ChatServer.clientList) {
                    for (ClientConnectionData c : ChatServer.clientList){
                        c.getOut().println(msg);
                        // c.getOut().flush();
                    }
                }
            } catch (Exception ex) {
                System.out.println("broadcast caught exception: " + ex);
                ex.printStackTrace();
            }

        }

        public void broadcastPrivate(String userName, String msg){
            System.out.println(userName);
            System.out.println(ChatServer.clientList.toString());
            try {

                System.out.println("Privately Broadcasting -- " + msg);
                ClientConnectionData c = new ClientConnectionData(null, null, null, null);
                for(int i=0; i<ChatServer.clientList.size(); i++){
                    if(ChatServer.clientList.get(i).getUserName().equals(userName)){
                        c = ChatServer.clientList.get(i);
                        break;
                    }
                }
                c.getOut().println(msg);
            } catch (Exception ex) {
                System.out.println("broadcast caught exception: " + ex);
                ex.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                broadcast("SUBMIT NAME");
                BufferedReader in = client.getInput();
                String str = in.readLine();
                while(true){
                    in = client.getInput();
                    str = in.readLine();
                    if(str.startsWith("NAME") && str.substring(4, str.length()) != null && !(ChatServer.clientList.contains(str.substring(4, str.length())))){
                        String userName = str.substring(5, str.length()).trim();
                        client.setUserName(userName);
                        break;
                    }
                    else{
                        broadcast("SUBMITNAME");
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
                            broadcast(msg);
                        }
                    }
                    else if(incoming.startsWith("PCHAT")){
                        String receiver = "";
                        for(int i=0; i< ChatServer.clientList.size(); i++){
                            if(incoming.startsWith(ChatServer.clientList.get(i).getUserName(), 6)){
                                receiver = ChatServer.clientList.get(i).getUserName();
                                break;
                            }
                        }
                        String chat = incoming.substring(6+receiver.length()).trim();
                        if(chat.length() > 0){
                            String msg = String.format("PCHAT %s %s", client.getUserName(), chat);
                            broadcastPrivate(receiver, msg);
                        }
                    }
                    else if (incoming.startsWith("QUIT")){
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
