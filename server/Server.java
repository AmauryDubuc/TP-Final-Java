package server;

import ITchat.ITchat;

import javafx.application.Platform;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
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
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);// OP_ACCEPT accepte les demandes de connexion entrante

    }

    public void start() {
        try {
            while (true) {
                int ready = selector.select(); // retourne les channel qui sont pret pour operation sous forme de int !!!!!!!! operation bloquante !!!!!!!
                if (ready == 0) {
                    continue;
                }

                Set<SelectionKey> cles = selector.selectedKeys();
                Iterator<SelectionKey> iterator = cles.iterator();
                
                while (iterator.hasNext()) {
                    SelectionKey cle = iterator.next();

                    if (cle.isAcceptable() && cle.channel() == serverSocketChannel) { //cle.isAcceptable() methode qui test si la cle est prete a recevoir une nouvelle connexion  de socket
                        // gerer connexion entrante
                        SocketChannel clientSocketChannel = serverSocketChannel.accept();
                        clientSocketChannel.configureBlocking(false);
                        clientSocketChannel.register(selector, SelectionKey.OP_READ); //OP_READ pour lire dans le channel


                    } else if (cle.isReadable()) {

                        SocketChannel client = (SocketChannel) cle.channel();// retourne un socket chanel
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        int bytesRead = client.read(buffer);

                         if (bytesRead == -1) {
                            // Le client a fermé la connexion
                            cle.cancel();
                            client.close();
                        } else {
                            // Décoder les données en texte
                            buffer.flip();
                            StringBuilder messageBuilder = new StringBuilder();
                        
                            while (buffer.hasRemaining()) {
                                char c = buffer.getChar();
                                if (c == '\n') { // fin de message
                                    String message = messageBuilder.toString().trim();
                                    // Diffuser le message à tous les clients connectés

                                    for (SelectionKey key : selector.keys()) {
                                        if (key.isValid() && key.channel() instanceof SocketChannel) {
                                            SocketChannel clientChannel = (SocketChannel) key.channel();
                                            if (clientChannel.equals(serverSocketChannel)) {
                                                ByteBuffer messageBuffer = ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));
                                                clientChannel.write(messageBuffer);
                                            }
                                        }
                                    }

                                    // Réinitialisez le messageBuilder pour le prochain message
                                    messageBuilder.setLength(0);
                                } else {
                                    messageBuilder.append(c);
                                }
                            }

                        iterator.remove();
                        }
                    }
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
    //     Platform.runLater(() -> serverUI.log(message));
    // }

}
