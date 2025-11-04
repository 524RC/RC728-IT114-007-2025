package M4.Part1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class Server {
    private int port = 3000;
    
    public String flipCoin(String username) {
        String[] options = { "Heads", "Tails" };
        Random rand = new Random();
        String result = options[rand.nextInt(2)];
        return username + " flipped a coin and got " + result;
    }

    public void relay(String message) {
        System.out.println(message);
    }
    private void start(int port) {
        this.port = port;
        System.out.println("Listening on port " + this.port);
        // server listening
        try (ServerSocket serverSocket = new ServerSocket(port);
                // client wait
                Socket client = serverSocket.accept(); // blocking;

                // read from client
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));) {

            System.out.println("Client connected, waiting for message");
            String fromClient = "";
            while ((fromClient = in.readLine()) != null) {
                if ("/kill server".equalsIgnoreCase(fromClient)) {
                    // normally you wouldn't have a remote kill command, this is just for example
                    // sake
                    System.out.println("Client killed server");
                    break;
                }else if ("/flip".equalsIgnoreCase(fromClient)) {
                    String resultMessage = flipCoin("Client"); // replace "Client" with username if you have one
                    relay(resultMessage);
                } else {
                    System.out.println("From client: " + fromClient);
                }
            }
        } catch (IOException e) {
            System.out.println("Exception from start()");
            e.printStackTrace();
        } finally {
            System.out.println("closing server socket");
        }
    }

    public static void main(String[] args) {
        System.out.println("Server Starting");
        Server server = new Server();
        int port = 3000;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            // can ignore, will either be index out of bounds or type mismatch
            // will default to the defined value prior to the try/catch
        }
        server.start(port);
        System.out.println("Server Stopped");
    }
}