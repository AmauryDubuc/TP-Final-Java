
import ITchat.ITchat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

/**
 * Client de tchat
 */
public class Client extends Thread implements ITchat {

	private SocketChannel socketChannel;
	private Selector selector;
	private ClientUI clientUI;

	/**
	 * Constructeur du Client
	 *
	 * @param ip
	 * @param port
	 * @param new_clientUI
	 */
	public Client(String ip, int port, ClientUI new_clientUI) throws IOException {

		clientUI = new_clientUI;
		selector = Selector.open();
		socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false);
		socketChannel.connect(new InetSocketAddress(ip, port));
		socketChannel.register(selector, SelectionKey.OP_CONNECT);// OP_ACCEPT accepte les demandes de connexion
																	// entrante
	}

	/**
	 * methodes pour envoyer un message au serveur
	 *
	 * @param message
	 */
	public void envoie_message(String message) {
		try {
			if (socketChannel.isConnected()) {

				ByteBuffer messageBuffer = ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));
				socketChannel.write(messageBuffer);

			} else {

				System.err.println("La connexion n'est pas encore établie.");

			}

		} catch (IOException ioe) {

			ioe.printStackTrace();

		}
	}

	/**
	 * fermer la connexion au serveur proprement
	 *
	 */
	public void close_connexion_server() {

		try {

			if (socketChannel != null && socketChannel.isOpen()) {

				socketChannel.close();

			}
		} catch (IOException ioe) {

			ioe.printStackTrace();

		}
	}

	/**
	 * Lancer par le ClientUI avce le .start()
	 *
	 */
	public void run() {

		try {

			boolean running = true;

			while (running) {

				int ready = selector.select();
				if (ready == 0) {

					continue;
				}
				Set<SelectionKey> cles = selector.selectedKeys();
				Iterator<SelectionKey> iterator = cles.iterator();

				while (iterator.hasNext()) {

					SelectionKey cle = iterator.next();
					if (!cle.isValid()) {
						cle.cancel();
						continue;
					} else if (cle.isConnectable()) {

						if (socketChannel.finishConnect()) {

							System.out.println("Connexion au server réussi");
							socketChannel.register(selector, SelectionKey.OP_READ); // maintenant que l'on est conncté
																					// on peut lire les message du
																					// serveur
							clientUI.setConnectedState();

						}
					} else if (cle.isReadable()) {

						SocketChannel connexion_server = (SocketChannel) cle.channel();
						ByteBuffer buffer = ByteBuffer.allocate(1024);
						int bytesRead = connexion_server.read(buffer);

						if (bytesRead == -1) {// Le server a fermé la connexion

							cle.cancel();
							clientUI.disconnectFromServer();

						} else {

							buffer.flip();
							String receivedMessage = StandardCharsets.UTF_8.decode(buffer).toString();
							clientUI.appendMessage(receivedMessage);

						}
					}
					iterator.remove();
				}
			}
		} catch (IOException ioe) {

			System.out.println("Echec de connexion au server");
			clientUI.disconnectFromServer();

		}
	}
}
