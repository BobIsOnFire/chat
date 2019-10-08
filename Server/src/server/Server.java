package server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

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

        System.out.println("Accepted connection from: " + address);
        ByteBuffer invitationBuffer = ByteBuffer.wrap(clientJoinMessage().getBytes());
        socket.write(invitationBuffer);

        buffer.clear();
        socket.read(buffer);
        buffer.flip();
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        String name = (new String(bytes)).trim().replaceAll("\\\\", "/").replaceAll("'", "\"");
        buffer.clear();

        socket.configureBlocking(false);
        Map<String, String> map = new HashMap<>();
        int color = 1 + (int) (Math.random() * 6);

        map.put("name", name);
        map.put("pasta", "false");
        map.put("color", Integer.toString(color));
        socket.register(selector, SelectionKey.OP_READ, map);

        invitationBuffer = ByteBuffer.wrap(clientChatEnterMessage().getBytes());
        socket.write(invitationBuffer);
        broadcast( coloredName(name, Integer.toString(color)) + " joined the chat.\n");
    }

    private static void handleRead(SelectionKey key) throws IOException {
        SocketChannel socket = (SocketChannel)key.channel();
        StringBuilder sb = new StringBuilder();
        Map<String, String> map = (Map<String, String>)key.attachment();
        String name = coloredName( map.get("name"), map.get("color") );
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

        String message = sb.toString().replaceAll("\\\\", "^").replaceAll("'", "\"");

        if (message.trim().equals("/pasta")) {
            map.put("pasta", "true");
            socket.write( ByteBuffer.wrap("--- Pasta mode activated ---\n".getBytes()) );
            return;
        }

        if (message.trim().equals("/unpasta")) {
            map.put("pasta", "false");
            socket.write( ByteBuffer.wrap("--- Pasta mode disabled ---\n".getBytes()) );
            return;
        }

        if (message.trim().startsWith("/color")) {
            String[] tokens = message.split("\\s+");
            if (tokens.length < 2 || !tokens[1].matches("\\d+") || Integer.parseInt(tokens[1]) < 1 || Integer.parseInt(tokens[1]) > 7) {
                socket.write( ByteBuffer.wrap("Please set color argument: number from 1 to 7\n".getBytes()) );
                return;
            }

            map.put("color", tokens[1]);
            String notify = "--- \\e[3" + tokens[1] + "mColor changed\\e[39m ---\n";
            socket.write( ByteBuffer.wrap(notify.getBytes()) );
            return;
        }

        // todo !online
        // todo buffer lines... somehow ehh

        String msg;
        boolean pasta = Boolean.parseBoolean(map.get("pasta"));
        if (message.equals("\0\n") || read < 0) {
            socket.close();
            msg = name + " left the chat.\n";
        } else {
            msg = (pasta ? "" : name + ": ") + message;
        }

        broadcast(msg);
    }

    private static void broadcast(String message) throws IOException {
        execute("echo -ne '" + message + "'");
        ByteBuffer broadcastBuffer = ByteBuffer.wrap(message.getBytes());

        for(SelectionKey key: selector.keys()) {
            if (key.isValid() && key.channel() instanceof SocketChannel) {
                SocketChannel socket = (SocketChannel) key.channel();
                socket.write(broadcastBuffer);
                broadcastBuffer.rewind();
            }
        }
    }

    private static void execute(String command) throws IOException {
        Process proc = Runtime.getRuntime().exec(new String[]{"bash", "-c", command});
        Scanner sc = new Scanner(proc.getInputStream());

        while(sc.hasNextLine()) {
            System.out.println(sc.nextLine());
        }
    }

    private static String clientJoinMessage() {
        return "Connected to Chat server.\nEnter your nickname: ";
    }

    private static String clientChatEnterMessage() {
        return "Welcome to Chat. Some rights reserved.\nUse /exit to leave the chat.\n";
    }

    private static String coloredName(String name, String color) {
        return "\\e[3" + color + "m" + name + "\\e[39m";
    }
}
