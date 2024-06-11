import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

class HttpHandler extends Thread {
    private Socket socket;

    public HttpHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (InputStream input = socket.getInputStream();
             OutputStream output = socket.getOutputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(input));
             PrintWriter writer = new PrintWriter(output, true)) {

            // Lese die Request-Zeile
            String requestLine = reader.readLine();
            if (requestLine == null || !requestLine.startsWith("GET")) {
                sendErrorResponse(writer, 400, "Bad Request");
                return;
            }

            // Lese die Header-Felder
            Map<String, String> headers = new HashMap<>();
            String line;
            while (!(line = reader.readLine()).isEmpty()) {
                String[] header = line.split(": ", 2);
                if (header.length == 2) {
                    headers.put(header[0], header[1]);
                }
            }

            // Protokolliere die Header-Felder
            headers.forEach((key, value) -> System.out.println(key + ": " + value));

            // Überprüfe den User-Agent
            String userAgent = headers.get("User-Agent");
            if (userAgent == null || !userAgent.contains("Firefox")) {
                sendErrorResponse(writer, 406, "Not Acceptable");
                return;
            }

            // Verarbeite den Pfad der angeforderten Ressource
            String[] requestParts = requestLine.split(" ");
            if (requestParts.length < 2) {
                sendErrorResponse(writer, 400, "Bad Request");
                return;
            }

            String filePath = requestParts[1];
            switch(filePath){
                case "/" -> filePath = "/index.html";
                case "/date" -> sendDateResponse(writer);
                case "/time" -> sendTimeResponse(writer);
                case "/yunus" -> sendYunusResponse(writer,"yunuske");
            }

            filePath = "Testweb" + filePath;

            File file = new File(filePath);
            if (!file.exists()) {
                sendErrorResponse(writer, 404, "Not Found");
                return;
            }

            // Bestimme den Content-Type
            String contentType = getContentType(filePath);

            // Sende die Antwort
            byte[] fileContent = Files.readAllBytes(file.toPath());
            writer.println("HTTP/1.0 200 OK");
            writer.println("Content-Type: " + contentType);
            writer.println("Content-Length: " + fileContent.length);
            writer.println();
            output.write(fileContent);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendYunusResponse(PrintWriter writer, String response) {

        writer.println("HTTP/1.0 200 OK");
        writer.println("Content-Type: text/plain");
        writer.println("Content-Length: " + response.length());
        writer.println();
        writer.println(response);
    }

    private void sendErrorResponse(PrintWriter writer, int statusCode, String message) {
        writer.println("HTTP/1.0 " + statusCode + " " + message);
        writer.println("Content-Type: text/html");
        writer.println();
        writer.println("<html><body><h1>" + statusCode + " " + message + "</h1></body></html>");
    }

    private String getContentType(String filePath) {
        if (filePath.endsWith(".html")) return "text/html";
        if (filePath.endsWith(".jpg")) return "image/jpeg";
        if (filePath.endsWith(".gif")) return "image/gif";
        if (filePath.endsWith(".pdf")) return "application/pdf";
        if (filePath.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }

    private void sendTimeResponse(PrintWriter writer) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        writer.println("HTTP/1.0 200 OK");
        writer.println("Content-Type: text/plain");
        writer.println("Content-Length: " + time.length());
        writer.println();
        writer.println(time);
    }

    private void sendDateResponse(PrintWriter writer) {
        String date = new SimpleDateFormat("dd.MM.yyyy").format(new Date());
        writer.println("HTTP/1.0 200 OK");
        writer.println("Content-Type: text/plain");
        writer.println("Content-Length: " + date.length());
        writer.println();
        writer.println(date);
    }
}