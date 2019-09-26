package testclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.CharBuffer;
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

    public static void main(String[] args) {
        if (args.length > 1) {
            lines = Integer.parseInt(args[0]);
            columns = Integer.parseInt(args[1]);
        }

        Scanner scan = new Scanner(System.in);
        System.out.println("Введите адрес:");
        String[] address = scan.nextLine().split(":");
        String ip = address[0];
        int port = Integer.parseInt(address[1]);
        System.out.println("Введите свой ник:");
        String message = scan.nextLine();

        try {
            socket = new Socket(ip, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            execute("echo -ne '\\e[2J\\e[1;1H'");
            out.println(message);
            TestClient.Sender sender = new TestClient.Sender();
            sender.start();

            execute("stty raw </dev/tty");
            while(!sender.stopped) {
                String str;
                if ((str = read()).trim().equals("!exit") || lastSymbol == 4) {
                    out.println("\0");
                    break;
                }

                out.println(str);
            }

            sender.setStop();
        } catch (Exception e) {
            e.printStackTrace();
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
        } catch (IOException var1) {
            System.err.println("Channels are not closed.");
        }

    }

    private static void print(String message) throws IOException {
        String[] strokes = message.split("\n");
        for(String s: strokes) {
            int k = s.length() / columns + 1;

            for(int i = 0; i < k; ++i) {
                String m = (i < k - 1) ? s.substring(i * columns, (i + 1) * columns) : s.substring(i * columns);
                if (lines > 0 && line > lines) {
                    execute("echo -ne '\\e[" + line + ";1H\\e[S\\e[2K';echo -n '" + m + "';echo -ne '\\e[1;1H\\e[2K" + builder.toString() + "'");
                } else {
                    execute("echo -ne '\\e[" + line + ";1H\\e[2K';echo -n '" + m + "';echo -ne '\\e[1;1H\\e[2K" + builder.toString() + "'");
                    ++line;
                }
            }
        }

    }

    private static String read() throws IOException {
        while(true) {
            lastSymbol = System.in.read();

            if (!cyrillic && lastSymbol > 127) {
                cyrillic = true;
                cyrillicBegin = (byte) lastSymbol;
                continue;
            }

            if (lastSymbol == 10 || lastSymbol == 13 || lastSymbol == 4 || lastSymbol == 3) {
                String str = builder.toString();
                builder = new StringBuilder();
                execute("echo -ne '\\e[1;1H\\e[2K'");
                return str;
            }

            if (lastSymbol == 127) {
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

            builder.append( (char) lastSymbol);
        }
    }

    private static void execute(String command) throws IOException {
        Process proc = Runtime.getRuntime().exec(new String[]{"bash", "-c", command});
        Scanner sc = new Scanner(proc.getInputStream());

        while(sc.hasNextLine()) {
            System.out.print(sc.nextLine());
        }

    }

    private static void printFinish() {
        try {
            execute("echo -ne '\\e[2J\\e[1;1H'");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static class Sender extends Thread {
        private boolean stopped;

        private Sender() {
        }

        private void setStop() {
            this.stopped = true;
            TestClient.printFinish();
            System.out.println("Chat is closed.");
        }

        public void run() {
            external: while(true) {
                try {
                    if (!this.stopped) {
                        CharBuffer buffer = CharBuffer.allocate(256);

                        while(true) {
                            if (TestClient.in.read(buffer) <= 0) {
                                continue external;
                            }

                            buffer.flip();
                            char[] chars = new char[buffer.limit()];
                            buffer.get(chars);
                            String str = new String(chars);
                            TestClient.print(str);
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
