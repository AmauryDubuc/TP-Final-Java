import ITchat.ITchat;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Processus serveur qui ecoute les connexion entrantes,
 * les messages entrant et les rediffuse au clients connectes
 *
 * @author mathieu.fabre
 */
public class Server extends Thread implements ITchat {

    private boolean running = true;
    private ServerUI serverUI;
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private List<Worker> listeWorker = new ArrayList<>();
    private List<SocketChannel> listeClients = new ArrayList<>(); // Je synchronise pour
                                                                  // que les clients ne
                                                                  // peuvent pas acceder a
                                                                  // cette liste en meme
                                                                  // temps

    /**
     * Constructeur du Serveur
     *
     * @param ip
     * @param port
     * @param new_serverUI
     */
    public Server(String ip, int port, ServerUI new_serverUI) throws IOException {

        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress(ip, port)); // si il y a une exception de type
                                                                            // "BindException: Address already in use:
                                                                            // bind", il faut attendre un peu. Je n'aoi
                                                                            // pas réussi a déterminer cb de temps, mais
                                                                            // 1 minutes fonctionne
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);// OP_ACCEPT accepte les demandes de connexion
                                                                       // entrante
        serverUI = new_serverUI;

    }

    /**
     * Arret du serveur avec arret proprement des worker, socketclient et de celui
     * du serveur
     *
     */
    public void stop_server() {
        try {

            for (Worker worker : listeWorker) {

                worker.running = false;

            }

            while (!listeClients.isEmpty()) {

                listeClients.get(0).close();
                remove_client(listeClients.get(0));

            }
            running = false;
            serverSocketChannel.close();
            serverUI.log("Le Serveur est déconnecté !");

        } catch (IOException ioe) {

            ioe.printStackTrace();

        }
    }

    /**
     * ajout client dans la liste
     *
     * @param client
     */
    public synchronized void add_client(SocketChannel client) {

        listeClients.add(client);

    }

    /**
     * supprime un client dans la liste
     *
     * @param client
     */
    public synchronized void remove_client(SocketChannel client) {

        listeClients.remove(client);

    }

    /**
     * lancer par le .start() dans le ServerUI
     *
     */
    public void run() {
        serverUI.log("Le Serveur est connecté !");
        try {

            while (running) {
                int ready = selector.select(); // retourne les channel qui sont pret pour operation sous forme de int
                                               // !!!!!!!! operation bloquante !!!!!!!
                if (ready == 0) {
                    continue;
                }
                Set<SelectionKey> cles = selector.selectedKeys();
                Iterator<SelectionKey> iterator = cles.iterator();

                while (iterator.hasNext()) {

                    SelectionKey cle = iterator.next();
                    // Si la cle n'est pas valide on l'a cancel sinon le else if on aura une
                    // exception car la cle n'est âs valide
                    if (!cle.isValid()) {
                        cle.cancel();
                        continue;
                        // cle.isAcceptable() methode qui test si la cle est prete a recevoir une
                        // nouvelle connexion de socket
                    } else if (cle.isAcceptable() && cle.channel() == serverSocketChannel) {
                        SocketChannel clientSocketChannel = serverSocketChannel.accept();
                        serverUI.log("Un client se connecte");
                        clientSocketChannel.configureBlocking(false);
                        clientSocketChannel.register(selector, SelectionKey.OP_READ); // OP_READ pour lire dans le
                                                                                      // channel
                        add_client(clientSocketChannel); // Ajoutez le client à la liste
                        Worker worker = new Worker(clientSocketChannel, listeClients, serverUI, this);
                        listeWorker.add(worker);
                        worker.start();
                    }
                    iterator.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
