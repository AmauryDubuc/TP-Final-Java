import ITchat.ITchat;

import javafx.application.Platform;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

/**
 * Processus serveur qui ecoute les connexion entrantes,
 * les messages entrant et les rediffuse au clients connectes
 *
 * @author mathieu.fabre
 */
public class Server extends Thread implements ITchat {

    private Selector selector;
    private ServerSocketChannel serverSocketChannel;

    public Server(String ip, int port) throws IOException {

        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress(ip, port));
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);// OP_ACCEPT accepte les demandes de connexion
                                                                       // entrante

    }

    public void run() {
        try {
            while (true) {
                int ready = selector.select(); // retourne les channel qui sont pret pour operation sous forme de int
                                               // !!!!!!!! operation bloquante !!!!!!!
                if (ready == 0) {
                    continue;
                }
                Set<SelectionKey> cles = selector.selectedKeys();
                Iterator<SelectionKey> iterator = cles.iterator();

                while (iterator.hasNext()) {
                    SelectionKey cle = iterator.next();

                    if (cle.isAcceptable() && cle.channel() == serverSocketChannel) { // cle.isAcceptable() methode qui
                                                                                      // test si la cle est prete a
                                                                                      // recevoir une nouvelle connexion
                                                                                      // de socket
                        // gerer connexion entrante
                        SocketChannel clientSocketChannel = serverSocketChannel.accept();
                        clientSocketChannel.configureBlocking(false);
                        clientSocketChannel.register(selector, SelectionKey.OP_READ); // OP_READ pour lire dans le
                                                                                      // channel

                    } else if (cle.isReadable()) {

                        SocketChannel client = (SocketChannel) cle.channel(); // retourne un socket channel
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        int bytesRead = client.read(buffer);

                        if (bytesRead == -1) {

                            // Le client a fermé la connexion
                            cle.cancel();
                            client.close();

                        } else {

                            // Décoder les données en texte
                            buffer.flip();
                            Charset charset = StandardCharsets.UTF_8;
                            String message = charset.decode(buffer).toString().trim();
                            // Diffuse le message à tous les clients connectés

                            for (SelectionKey selKey : selector.keys()) {
                                if (selKey.isValid() && selKey.channel() instanceof SocketChannel) {
                                    SocketChannel clientChannel = (SocketChannel) selKey.channel();
                                    if (!clientChannel.equals(serverSocketChannel)) { // On s'assure de ne pas l'envoyer
                                                                                      // au serveur
                                        ByteBuffer messageBuffer = charset.encode(message);
                                        clientChannel.write(messageBuffer);
                                    }
                                }
                            }
                        }
                    }
                    iterator.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Envoi un message de log a l'IHM
     */
    // public void sendLogToUI(String message) {
    // Platform.runLater(() -> serverUI.log(message));
    // }

}
