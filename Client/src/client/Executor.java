package client;

class Executor {
    static final String CLEAR_SCREEN = "\u001B[2J";
    static final String CLEAR_LINE = "\u001B[2K";
    static final String SCROLL_UP = "\u001B[S";

    static String DELETE_SYMBOLS(int amount) {
        return String.format("\u001B[%dD\u001B[K", amount + 1);
    }

    static String MOVE_LINE(int line) {
        return String.format("\u001B[%d;1H", line);
    }
}
