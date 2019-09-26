package server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Server {
    private static ServerSocketChannel serverChannel;
    private static Selector selector;
    private static ByteBuffer buffer = ByteBuffer.allocate(256);

    public static void main(String[] args) {
        try {
            serverChannel = ServerSocketChannel.open();
            serverChannel.socket().bind(new InetSocketAddress(0));
            serverChannel.configureBlocking(false);
            selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("Opening server on " + InetAddress.getLocalHost() + ":" + serverChannel.socket().getLocalPort());

            while(serverChannel.isOpen()) {
                selector.select();
                Iterator iter = selector.selectedKeys().iterator();

                while(iter.hasNext()) {
                    SelectionKey key = (SelectionKey)iter.next();
                    iter.remove();
                    if (key.isAcceptable()) {
                        handleAccept(key);
                    }

                    if (key.isReadable()) {
                        handleRead(key);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void handleAccept(SelectionKey key) throws IOException {
        SocketChannel socket = ((ServerSocketChannel)key.channel()).accept();
        String address = socket.socket().getInetAddress().toString() + ":" + socket.socket().getPort();

        buffer.clear();
        socket.read(buffer);
        buffer.flip();
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        String name = (new String(bytes)).trim();
        buffer.clear();

        socket.configureBlocking(false);
        Map<String, String> map = new HashMap<>();
        map.put("name", name);
        map.put("pasta", "false");
        socket.register(selector, SelectionKey.OP_READ, map);

        System.out.println("Accepted connection from: " + address + ", username - " + name);
        ByteBuffer invitationBuffer = ByteBuffer.wrap(clientJoinMessage().getBytes());
        socket.write(invitationBuffer);
        invitationBuffer.clear();
        broadcast(name + " joined the chat.\n");
    }

    private static void handleRead(SelectionKey key) throws IOException {
        SocketChannel socket = (SocketChannel)key.channel();
        StringBuilder sb = new StringBuilder();
        Map<String, String> map = (Map<String, String>)key.attachment();
        String name = map.get("name");
        buffer.clear();

        int read;
        try {
            while((read = socket.read(buffer)) > 0) {
                buffer.flip();
                byte[] bytes = new byte[buffer.limit()];
                buffer.get(bytes);
                sb.append(new String(bytes));
                buffer.clear();
            }
        } catch (IOException e) {
            read = -1;
        }

        if (sb.toString().trim().equals("!pasta")) {
            map.put("pasta", "true");
            return;
        }

        if (sb.toString().trim().equals("!unpasta")) {
            map.put("pasta", "false");
            return;
        }

        // todo !online
        // todo buffer lines

        String msg;
        boolean pasta = Boolean.parseBoolean(map.get("pasta"));
        if (sb.toString().equals("\0\n") || read < 0) {
            socket.close();
            msg = name + " left the chat.\n";
        } else {
            msg = (pasta ? "" : name + ": ") + sb.toString();
        }

        broadcast(msg);
    }

    private static void broadcast(String message) throws IOException {
        System.out.print(message);
        ByteBuffer broadcastBuffer = ByteBuffer.wrap(message.getBytes());

        for(SelectionKey key: selector.keys()) {
            if (key.isValid() && key.channel() instanceof SocketChannel) {
                SocketChannel socket = (SocketChannel) key.channel();
                socket.write(broadcastBuffer);
                broadcastBuffer.rewind();
            }
        }

    }

    private static String clientJoinMessage() {
        return "Welcome to Chat. Some rights reserved.\nUse !exit to leave the chat.\n";
    }
}
