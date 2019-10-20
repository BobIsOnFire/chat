package client;

import java.io.IOException;
import java.util.Scanner;

class Executor {
    static void execute(String command) throws IOException {
        Process proc = Runtime.getRuntime().exec(new String[]{"bash", "-c", command});
        Scanner sc = new Scanner(proc.getInputStream());

        while (sc.hasNextLine()) System.out.print(sc.nextLine());
    }

    static final String CLEAR_SCREEN = "\\e[2J";
    static final String CLEAR_LINE = "\\e[2K";
    static final String SCROLL_UP = "\\e[S";

    static String DELETE_SYMBOLS(int amount) {
        return String.format("\\e[%dD\\e[K", amount + 1);
    }

    static String MOVE_LINE(int line) {
        return String.format("\\e[%d;1H", line);
    }
}
