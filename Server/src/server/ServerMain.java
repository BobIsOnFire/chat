package server;

import server.scanner.AnsiImageScanner;
import server.scanner.AsciiImageScanner;
import server.scanner.ImageScanner;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ServerMain {
    private static ServerSocketChannel serverChannel;
    private static Selector selector;
    private static ByteBuffer buffer = ByteBuffer.allocate(256);

    private static final String clientJoin = "Подключен к серверу чата.\nВведите ваш ник: ";
    private static final String clientChatEnter = "Добро пожаловать в чат.\nИспользуйте /exit для выхода.\n";
    private static final String pastaModeEnabled = "--- Режим пасты включен ---\nИспользуйте ^L (Ctrl-L) для отправки сообщения\n";
    private static final String pastaModeDisabled = "--- Режим пасты выключен ---\n";
    private static final String wrongColorAttribute = "Введите значение для цвета текста: число от 0 до 255.\n";
    private static final String noPathAttribute = "Введите путь к файлу изображения.\n";
    private static final String invalidURL = "Введите корректный URL или путь к файлу.\n";
    private static final String wrongURLFileType = "По данному адресу изображения не существует.\n";
    private static final String invalidImageSize = "Изображение слишком велико для ANSI-графики (жертвуем возможностями ради скорости).\n";

    private static final int BASIC_CONSOLE_WIDTH = 100;

    public static void main(String[] args) {
        try {
            serverChannel = ServerSocketChannel.open();
            serverChannel.socket().bind(new InetSocketAddress(0));
            serverChannel.configureBlocking(false);
            selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("Открываю сервер по адресу " + InetAddress.getLocalHost() +
                    ":" + serverChannel.socket().getLocalPort());
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

        System.out.println("Принято соединение от: " + address);
        ByteBuffer invitationBuffer = ByteBuffer.wrap(clientJoin.getBytes());
        socket.write(invitationBuffer);

        buffer.clear();
        socket.read(buffer);
        buffer.flip();
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        String name = (new String(bytes)).trim().replaceAll("\\\\", "^").replaceAll("'", "\"");
        buffer.clear();

        socket.configureBlocking(false);
        Map<String, String> map = new HashMap<>();
        int color = (int) (Math.random() * 255);

        map.put("name", name);
        map.put("pasta", "false");
        map.put("color", Integer.toString(color));
        socket.register(selector, SelectionKey.OP_READ, map);

        invitationBuffer = ByteBuffer.wrap(clientChatEnter.getBytes());
        socket.write(invitationBuffer);
        broadcast( coloredName(name, Integer.toString(color)) + " вошел в чат.\n");
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

        String message = sb.toString().replaceAll("\\\\", "^").replaceAll("'", "\""); // todo arrow keys handling

        if (message.trim().equals("/pasta")) {
            map.put("pasta", "true");
            socket.write(ByteBuffer.wrap( pastaModeEnabled.getBytes()) );
            return;
        }

        if (message.trim().equals("/unpasta")) {
            map.put("pasta", "false");
            socket.write( ByteBuffer.wrap( pastaModeDisabled.getBytes()) );
            return;
        }

        if (message.trim().startsWith("/color")) {
            String[] tokens = message.split("\\s+");
            if (tokens.length < 2 || !tokens[1].matches("\\d+") || Integer.parseInt(tokens[1]) < 0 || Integer.parseInt(tokens[1]) > 255) {
                socket.write( ByteBuffer.wrap( wrongColorAttribute.getBytes() ) );
                return;
            }

            map.put("color", tokens[1]);
            String notify = "--- \u001B[38;5;" + tokens[1] + "mЦвет изменен\u001B[39m ---\n";
            socket.write( ByteBuffer.wrap(notify.getBytes()) );
            return;
        }

        if (message.trim().startsWith("/pic2")) {
            String[] tokens = message.split("\\s+");
            if (tokens.length < 2) {
                socket.write( ByteBuffer.wrap( noPathAttribute.getBytes() ) );
                return;
            }

            URL url;
            try {
                url = new URL(tokens[1]);
            } catch (MalformedURLException exc) {
                Path path = Paths.get(tokens[1]);
                if (!Files.exists(path)) {
                    socket.write( ByteBuffer.wrap( invalidURL.getBytes() ) );
                    return;
                }
                url = path.toUri().toURL();
            }

            try {
                int consoleWidth;
                if (tokens.length < 3) {
                    consoleWidth = BASIC_CONSOLE_WIDTH;
                } else {
                    consoleWidth = Integer.parseInt(tokens[2]);
                }

                ImageScanner scanner;
                if (message.trim().startsWith("/pic2ascii")) {
                    scanner = new AsciiImageScanner(url, consoleWidth);
                } else scanner = new AnsiImageScanner(url, consoleWidth);

                String response = name + ": Картинка по адресу " + tokens[1] + "\n";
                broadcast(response);

                while (scanner.hasNextLine()) broadcast(scanner.nextLine());
                return;
            } catch (IOException exc) {
                socket.write( ByteBuffer.wrap( wrongURLFileType.getBytes() ) );
                return;
            } catch (RuntimeException exc) {
                socket.write( ByteBuffer.wrap( invalidImageSize.getBytes() ) );
            }
        }

        // todo /online
        // todo smiles in ansi-graphics (16x16)
        // todo bash client?? is that possible?
        // todo fix lagging on new user enter

        String response;
        boolean pasta = Boolean.parseBoolean(map.get("pasta"));
        if (message.equals("\0\n") || read < 0) {
            socket.close();
            response = name + " покинул чат.\n";
        } else {
            response = (pasta ? "" : name + ": ") + message;
        }

        broadcast(response);
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

    private static String coloredName(String name, String color) {
        return "\u001B[38;5;" + color + "m" + name + "\u001B[39m";
    }
}
