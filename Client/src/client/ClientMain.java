package client;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ClientMain {
    private static int lines = -1;
    private static int columns = -1;

    public static void main(String[] args) {
        if (args.length > 1) {
            lines = Integer.parseInt(args[0]);
            columns = Integer.parseInt(args[1]);
        }

        Scanner scanner = new Scanner(System.in);
        System.out.print("Введите адрес в формате HOST:PORT\n> ");
        String[] address;
        boolean addressCorrect = false;

        Socket socket = new Socket();
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
        ChatHandler handler = new ChatHandler(socket);
        handler.runClient();
    }

    static int getLines() {
        return lines;
    }

    static int getColumns() {
        return columns;
    }
}

