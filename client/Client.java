
import ITchat.ITchat;
import message.Message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

/**
 * Client de tchat
 */
public class Client extends Thread implements ITchat {

	private SocketChannel socketChannel;
	private Selector selector;
	private ClientUI clientUI;

	public Client(String ip, int port, ClientUI new_clientUI) throws IOException {

		clientUI = new_clientUI;
		selector = Selector.open();
		socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false);
		socketChannel.connect(new InetSocketAddress(ip, port));
		socketChannel.register(selector, SelectionKey.OP_CONNECT);// OP_ACCEPT accepte les demandes de connexion
																	// entrante
	}

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

	public void start() {

		try {

			boolean running = true;

			while (running) {
				selector.select();
				Set<SelectionKey> cles = selector.selectedKeys();
				Iterator<SelectionKey> iterator = cles.iterator();

				while (iterator.hasNext()) {

					SelectionKey cle = iterator.next();

					if (cle.isConnectable()) {

						if (socketChannel.finishConnect()) {

							System.out.println("Connexion au server réussi");
							clientUI.setConnectedState();

						} else {

							System.out.println("Echec de connexion au server");
							socketChannel.close();
							running = false;

						}

					} else if (cle.isReadable()) {
						System.out.println(6);
						SocketChannel connexion_server = (SocketChannel) cle.channel();
						ByteBuffer buffer = ByteBuffer.allocate(1024);
						int bytesRead = connexion_server.read(buffer);

						if (bytesRead == -1) {

							// Le server a fermé la connexion
							cle.cancel();
							connexion_server.close();

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

			ioe.printStackTrace();

		}
	}
}
