import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

/**
 * On crée un Thread qui gère tout avec le client
 * comme ca si y a trop de données le serveur n'est pas bloqué pour accepter de
 * nouveaux clients
 *
 * @author mathieu.fabre
 */
public class Worker extends Thread {

    boolean running = true;
    private SocketChannel clientSocketChannel;
    private List<SocketChannel> listeClients;
    private ServerUI serverUI;
    private Server server;

    /**
     * Constructeur du Worker
     *
     * @param new_clientSocketChannel
     * @param new_listeClients
     * @param new_serverUI
     * @param new_server
     */
    public Worker(SocketChannel new_clientSocketChannel, List<SocketChannel> new_listeClients, ServerUI new_serverUI,
            Server new_server) {

        clientSocketChannel = new_clientSocketChannel;
        listeClients = new_listeClients;
        serverUI = new_serverUI;
        server = new_server;
    }

    /**
     * lancer par le serveur avce le .start()
     *
     * @param client
     */
    public void run() {

        try {
            while (running) {

                ByteBuffer buffer = ByteBuffer.allocate(1024);
                int bytesRead = clientSocketChannel.read(buffer);
                if (bytesRead == 0) { // si c'est égale à 0 c'est qu'il n'y a pas de message donc on fait pas la suite

                    continue;

                }

                if (bytesRead == -1) {// Le client a fermé la connexion

                    clientSocketChannel.close();
                    server.remove_client(clientSocketChannel);
                    serverUI.log("Un client se déconnecte");
                    running = false;

                } else {

                    // Décode les données en texte
                    buffer.flip();
                    Charset charset = StandardCharsets.UTF_8;
                    String message = charset.decode(buffer).toString().trim();

                    // Diffuse le message à tous les clients connectés
                    for (SocketChannel sc : listeClients) {

                        ByteBuffer messageBuffer = charset.encode(message);
                        if (sc.isOpen()) {// On vérifie que le client est connecté, meme si la liste est une liste de
                                          // clients connectés. ca fait une double verif et evite au serveur une
                                          // exception

                            sc.write(messageBuffer);

                        }
                    }
                }
            }
        } catch (IOException ioe) {

            ioe.printStackTrace();

        }
    }
}
