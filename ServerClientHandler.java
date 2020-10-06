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

        @Override
        public void run() {
            try {
                //original code
                BufferedReader in = client.getInput();
                String userName = in.readLine().trim();
                client.setUserName(userName);

                //this is what i'm working on rn (check trello)
                /*broadcast("SUBMIT NAME");
                //get userName, first message from user
                while(true){
                    if(in.readLine().startsWith("NAME") && in.readLine().substring(4, in.readLine().length()) != null && !(ChatServer.clientList.contains(in.readLine().substring(4, in.readLine().length())))){
                        String userName = in.readLine().substring(5, in.readLine().length()).trim();
                        System.out.println("oogly boogly");
                        client.setUserName(userName);
                        break;
                    }
                    else{
                        broadcast("SUBMITNAME");
                    }
                }*/

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
                    } else if (incoming.startsWith("QUIT")){
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
