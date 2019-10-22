package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.CharBuffer;
import java.util.Scanner;

import static client.Executor.*;

class ClientHandler {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private StringBuilder builder;
    private int lastSymbol;
    private boolean cyrillic;
    private byte cyrillicBegin;
    private boolean pasta;
    private boolean picmode;

    ClientHandler(Socket socket) {
        this.socket = socket;
        lastSymbol = 10;
        pasta = false;
        builder = new StringBuilder();
        cyrillic = false;
    }

    void runClient() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            CharBuffer buffer = CharBuffer.allocate(256);
            if (in.read(buffer) < 0)
                throw new IOException();
            buffer.flip();
            char[] chars = new char[buffer.limit()];
            buffer.get(chars);
            System.out.print(chars);
            out.println(new Scanner(System.in).nextLine());

            System.out.print(CLEAR_SCREEN + MOVE_LINE(1));
            Runtime.getRuntime().exec(new String[]{"bash", "-c", "stty raw </dev/tty"});

            Receiver receiver = new Receiver(in, builder);
            receiver.start();

            while(!receiver.isStopped()) {
                String str;
                if ((str = read()).trim().equals("/exit") || lastSymbol == 4 || lastSymbol == 3) {
                    out.println("\0");
                    break;
                }

                if (str.trim().equals("/pasta")) {
                    pasta = true;
                    receiver.line = receiver.lines + 1;
                }
                if (str.trim().equals("/unpasta")) pasta = false;

                out.println(str);
            }
            receiver.setStop();
        } catch (Exception e) {
            System.out.println("Shutting things down...");
            e.printStackTrace();
        } finally {
            close();
        }
    }

    private void close() {
        try {
            in.close();
            out.close();
            socket.close();

            Runtime.getRuntime().exec(new String[]{"bash", "-c", "stty cooked </dev/tty"});

            System.out.print(CLEAR_SCREEN + MOVE_LINE(1));
            System.out.println("Чат закрыт.");
        } catch (IOException e) {
            System.err.println("Потоки чата не закрыты.");
        }

    }

    private String read() throws IOException { // todo bytes instead of symbols
        while(true) {
            lastSymbol = System.in.read();

            if (!cyrillic && lastSymbol > 127) {
                cyrillic = true;
                cyrillicBegin = (byte) lastSymbol;
                continue;
            }

            if (lastSymbol == 127 || lastSymbol == 8) {
                System.out.print(DELETE_SYMBOLS(2));
                if (builder.length() > 0) {
                    builder.delete(builder.length() - 1, builder.length());
                }
                continue;
            }

            if (cyrillic) {
                cyrillic = false;
                byte[] bytes = {cyrillicBegin, (byte) lastSymbol};
                builder.append( new String(bytes) );
                continue;
            }

            if ( pasta && lastSymbol == 13 ) {
                builder.append("\r\n");
                continue;
            }

            boolean endOfMessage = lastSymbol == 12 || lastSymbol == 3 || lastSymbol == 4 ||
                    !pasta && (lastSymbol == 10 || lastSymbol == 13);

            if (endOfMessage) {
                String str = builder.toString();
                builder.delete(0, builder.length());
                return str;
            }

            builder.append( (char) lastSymbol );
        }
    }
}
