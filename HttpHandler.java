import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

class HttpHandler extends Thread {
    InputStream input;
    OutputStream output;
    private final BufferedReader reader;
    private DataOutputStream outToClient;
    private final String CRLF = "\r\n" ;
    FileInputStream fInput;

    public HttpHandler(Socket socket) throws IOException {
        input = socket.getInputStream();
        output = socket.getOutputStream();
        reader = new BufferedReader(new InputStreamReader(input));
        outToClient =  new DataOutputStream(output);
    }

    @Override
    public void run() {
        try {

            // Lese die Request-Zeile
            String requestLine = reader.readLine();
            System.err.println(requestLine);
            if (requestLine == null || !requestLine.startsWith("GET")) {
                sendErrorResponse(400, "Bad Request");
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
            headers.forEach((key, value) -> System.err.println(key + ": " + value));

            // Überprüfe den User-Agent
            String userAgent = headers.get("User-Agent");
            if (userAgent == null || !userAgent.contains("Firefox")) {
                sendErrorResponse(406, "Not Acceptable");
                return;
            }

            // Verarbeite den Pfad der angeforderten Ressource
            String[] requestParts = requestLine.split(" ");
            if (requestParts.length < 2) {
                sendErrorResponse(400, "Bad Request");
                return;
            }

            String filePath = requestParts[1];
            switch (filePath) {
                case "/" -> filePath = "/index.html";
                case "/date" -> sendDateResponse();
                case "/time" -> sendTimeResponse();
                case "/yunus" -> sendYunusResponse("yunuske");
                //default -> sendErrorResponse(writer,404,"Not Found");
            }

            filePath = "Testweb" + filePath;

            File file = new File(filePath);
            if (!file.exists()) {
                sendErrorResponse(404, "Not Found");
                return;
            }

            // Bestimme den Content-Type
            String contentType = getContentType(filePath);
            writeToClient("HTTP/1.0 200 OK");
            writeToClient("Content-Type: " + contentType);
            writeToClient("Content-Length: " + file.length());
            writeToClient("");
            fInput = new FileInputStream(filePath);
            // Sende die Antwort
            byte[] fileContent = new byte[4096];
            int len;
            while ((len = fInput.read(fileContent)) > 0) {
                output.write(fileContent, 0, len);
            }
           // byte[] fileContent = Files.readAllBytes(file.toPath()); //readAllBytes problematisch, da Dateien zu groß werden z.B. Viedeo mit 30gb

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendYunusResponse(String response) throws IOException {

        writeToClient("HTTP/1.0 200 OK");
        writeToClient("Content-Type: text/plain");
        writeToClient("Content-Length: " + response.length());
        writeToClient("");
        writeToClient(response);
    }

    private void sendErrorResponse(int statusCode, String message) throws IOException {
        writeToClient("HTTP/1.0 " + statusCode + " " + message);
        writeToClient("Content-Type: text/html");
        writeToClient("");
        writeToClient("<html><body><h1>" + statusCode + " " + message + "</h1></body></html>");
    }

    private String getContentType(String filePath) {

        String extension = filePath.substring(filePath.lastIndexOf("."));
        return switch (extension) {
            case ".html" -> "text/html";
            case ".jpg" -> "image/jpeg";
            case ".gif" -> "image/gif";
            case ".pdf" -> "application/pdf";
            case ".ico" -> "image/x-icon";
            default -> "application/octet-stream";
        };
    }

    private void sendTimeResponse() throws IOException {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        writeToClient("HTTP/1.0 200 OK");
        writeToClient("Content-Type: text/plain");
        writeToClient("Content-Length: " + time.length());
        writeToClient("");
        writeToClient(time);
    }

    private void sendDateResponse() throws IOException {
        String date = new SimpleDateFormat("dd.MM.yyyy").format(new Date());
        writeToClient("HTTP/1.0 200 OK");
        writeToClient("Content-Type: text/plain");
        writeToClient("Content-Length: " + date.length());
        writeToClient("");
        writeToClient(date);
    }

    private void writeToClient(String line) throws IOException {
        /* Sende eine Antwortzeile zum Client
         * ALLE Antworten an den Client müssen über diese Methode gesendet werden ("Sub-Layer") */
        outToClient.write((line + CRLF).getBytes());
        System.err.println("WebServer " + this.getName() + " has written the message: " + line);
    }
}
