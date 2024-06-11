import java.io.*;
import java.net.*;

public class HttpServer {
    public static void main(String[] args) {
        int port = 8080;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                new HttpHandler(socket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
