package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Arrays;

import static client.Executor.*;

public class Receiver extends Thread {
    int lines;
    private int columns;
    int line = 2;

    private String cache;
    private BufferedReader in;
    private StringBuilder builder;
    private boolean stopped;

    Receiver(BufferedReader in, StringBuilder builder) {
        this.in = in;
        this.builder = builder;
        lines = ClientMain.getLines();
        columns = ClientMain.getColumns();
        cache = "";
    }

    boolean isStopped() {
        return stopped;
    }

    void setStop() {
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

    private void print(String message) {
        String[] messageLines = (message + " ").split("\n");
        messageLines[0] = cache + messageLines[0];
        cache = messageLines[messageLines.length - 1];
        cache = cache.substring(0, cache.length() - 1);
        messageLines = Arrays.copyOf(messageLines, messageLines.length - 1);

        for(String s: messageLines) {
            if (s.startsWith("\u001B")) {
                writeLine(s);
                continue;
            }

            int k = s.length() / columns + 1;
            for(int i = 0; i < k; ++i) {
                String lineContent = (i < k - 1) ? s.substring(i * columns, (i + 1) * columns) : s.substring(i * columns);
                writeLine(lineContent);
            }
        }
        System.out.print(MOVE_LINE(1) + CLEAR_LINE + builder.toString());
    }

    private void writeLine(String lineContent) {
        if (lines > 0 && line > lines) {
            System.out.print(MOVE_LINE(line) + SCROLL_UP + CLEAR_LINE + lineContent);
        } else {
            System.out.print(MOVE_LINE(line) + CLEAR_LINE + lineContent);
            ++line;
        }
    }
}
