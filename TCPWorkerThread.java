import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class TCPWorkerThread extends Thread {
    /*
     * Arbeitsthread, der eine existierende Socket-Verbindung zur Bearbeitung
     * erhaelt
     */
    /* Protokoll-Codierung des Zeilenendes: CRLF */
    private final String CRLF = "\r\n" ;

    private int name;
    private Socket socket;
    private TCPServer server;
    private BufferedReader inFromClient;
    private DataOutputStream outToClient;
    boolean workerServiceRequested = true; // Arbeitsthread beenden?

    public TCPWorkerThread(int num, Socket sock, TCPServer server) {
        /* Konstruktor */
        this.name = num;
        this.socket = sock;
        this.server = server;
    }

    public void run() {
        String capitalizedSentence;

        System.err.println("TCP Worker Thread " + name +
                " is running until QUIT is received!");

        try {
            /* Socket-Basisstreams durch spezielle Streams filtern */
            inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outToClient = new DataOutputStream(socket.getOutputStream());

            while (workerServiceRequested) {
                /* String vom Client empfangen und in Grossbuchstaben umwandeln */
                capitalizedSentence = readFromClient().toUpperCase();

                /* Modifizierten String an Client senden */
                writeToClient(capitalizedSentence);

                /* Test, ob Arbeitsthread beendet werden soll */
                if (capitalizedSentence.startsWith("QUIT")) {
                    workerServiceRequested = false;
                }
            }

            /* Socket-Streams schliessen --> Verbindungsabbau */
            socket.close();
        } catch (IOException e) {
            System.err.println("Connection aborted by client!");
        } finally {
            System.err.println("TCP Worker Thread " + name + " stopped!");
            /* Platz fuer neuen Thread freigeben */
            server.workerThreadsSem.release();
        }
    }

    private String readFromClient() throws IOException {
        /* Lies die naechste Anfrage-Zeile (request) vom Client
         * ALLE Anfragen vom Client müssen über diese Methode empfangen werden ("Sub-Layer") */
        String request = inFromClient.readLine();
        System.err.println("TCP Worker Thread " + name + " detected job: " + request);

        return request;
    }

    private void writeToClient(String line) throws IOException {
        /* Sende eine Antwortzeile zum Client
         * ALLE Antworten an den Client müssen über diese Methode gesendet werden ("Sub-Layer") */
        outToClient.write((line + CRLF).getBytes());
        System.err.println("TCP Worker Thread " + name + " has written the message: " + line);
    }
}
