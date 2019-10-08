package testclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Scanner;

public class TestClient {
    private static Socket socket;
    private static BufferedReader in;
    private static PrintWriter out;
    private static int line = 2;

    private static StringBuilder builder = new StringBuilder();
    private static int lastSymbol = 10;
    private static boolean cyrillic = false;
    private static byte cyrillicBegin;

    private static int lines = -1;
    private static int columns = -1;
    private static String cache = "";
    private static boolean pasta = false;

    public static void main(String[] args) {
        if (args.length > 1) {
            lines = Integer.parseInt(args[0]);
            columns = Integer.parseInt(args[1]);
        }

        Scanner scanner = new Scanner(System.in);
        System.out.print("Введите адрес в формате HOST:PORT\n> ");
        String[] address;
        boolean addressCorrect = false;

        while (!addressCorrect) {
            if (!scanner.hasNextLine())
                System.exit(0);
            address = scanner.nextLine().split(":");
            addressCorrect = address.length == 2 &&
                    address[1].matches("^\\d+$") &&
                    Integer.parseInt(address[1]) > 10000 &&
                    Integer.parseInt(address[1]) < 65536;

            if (!addressCorrect)
                System.out.print("Формат адреса неккоректен. Попробуйте еще раз.\n> ");
            else
                try {
                    String ip = address[0];
                    int port = Integer.parseInt(address[1]);
                    socket = new Socket(ip, port);
                } catch (IOException e) {
                    System.out.print("Невозможно подключиться к удаленному хосту. Попробуйте ввести адрес еще раз.\n> ");
                    addressCorrect = false;
                }
        }

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

            out.println(scanner.nextLine());

            execute("echo -ne '\\e[2J\\e[1;1H'");
            Receiver receiver = new Receiver();
            receiver.start();

            execute("stty raw </dev/tty");
            while(!receiver.stopped) {
                String str;
                if ((str = read()).trim().equals("/exit") || lastSymbol == 4 || lastSymbol == 3) {
                    out.println("\0");
                    break;
                }

                if (str.trim().equals("/pasta")) {
                    pasta = true;
                    line = lines;
                }
                if (str.trim().equals("/unpasta")) pasta = false;

                out.println(str);
            }
            receiver.setStop();
        } catch (Exception e) {
            System.out.println("Shutting things down...");
        } finally {
            close();
        }

    }

    private static void close() {
        try {
            in.close();
            out.close();
            socket.close();

            execute("stty cooked </dev/tty");
            TestClient.printFinish();
            System.out.println("Chat is closed.");
        } catch (IOException e) {
            System.err.println("Channels are not closed.");
        }

    }

    private static void print(String message) throws IOException {
        String[] lines = (message + " ").split("\n");
        lines[0] = cache + lines[0];
        cache = lines[lines.length - 1];
        cache = cache.substring(0, cache.length() - 1);
        lines = Arrays.copyOf(lines, lines.length - 1);

        for(String s: lines) {
            int k = s.length() / columns + 1;

            for(int i = 0; i < k; ++i) {
                String m = (i < k - 1) ? s.substring(i * columns, (i + 1) * columns) : s.substring(i * columns);
                if (TestClient.lines > 0 && line > TestClient.lines) {
                    execute("echo -ne '\\e[" + line + ";1H\\e[S\\e[2K';echo -ne '" + m + "'");
                } else {
                    execute("echo -ne '\\e[" + line + ";1H\\e[2K';echo -ne '" + m + "'");
                    ++line;
                }
            }
        }
        execute("echo -ne '\\e[1;1H\\e[2K" + toEscapedString(builder) + "'");

    }

    private static String read() throws IOException {
        while(true) {
            lastSymbol = System.in.read();

            if (!cyrillic && lastSymbol > 127) {
                cyrillic = true;
                cyrillicBegin = (byte) lastSymbol;
                continue;
            }

            if (lastSymbol == 127 || lastSymbol == 8) {
                execute("echo -ne '\\e[3D\\e[K'");
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
                builder = new StringBuilder();
                return str;
            }

            builder.append( (char) lastSymbol );
        }
    }

    private static void execute(String command) throws IOException {
        Process proc = Runtime.getRuntime().exec(new String[]{"bash", "-c", command});
        Scanner sc = new Scanner(proc.getInputStream());

        while (sc.hasNextLine()) System.out.print(sc.nextLine());
    }

    private static void printFinish() {
        try {
            execute("echo -ne '\\e[2J\\e[1;1H'");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static String toEscapedString(StringBuilder sb) {
        return sb.toString().replaceAll("\\n", "\n\\\\e[E\\\\e[2K");
    }

    private static class Receiver extends Thread {
        private boolean stopped;

        private void setStop() {
            this.stopped = true;
        }

        public void run() {
            external: while(true) {
                try {
                    if (!this.stopped) {
                        CharBuffer buffer = CharBuffer.allocate(256);

                        while(true) {
                            if (in.read(buffer) <= 0) continue external;

                            buffer.flip();
                            char[] chars = new char[buffer.limit()];
                            buffer.get(chars);
                            String str = new String(chars);
                            print(str);
                            buffer.clear();
                        }
                    }
                } catch (IOException e) {
                    this.setStop();
                }
                return;
            }
        }
    }
}
