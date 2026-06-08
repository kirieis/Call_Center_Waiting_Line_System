package ui;

import java.util.Scanner;

public class InputHandler {
    private Scanner scanner;

    public InputHandler() {
        this.scanner = new Scanner(System.in);
    }

    public int readInt(String prompt) {
        return 0;
    }

    public String readString(String prompt) {
        return "";
    }

    public String readPhoneNumber() {
        return "";
    }

    public boolean readBoolean(String prompt) {
        return false;
    }

    public void close() {
        scanner.close();
    }
}
