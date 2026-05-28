import java.util.Scanner;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    private static final CallQueue callQueue = new CallQueue();
    private static int orderNumber = 1;

    public static void main(String[] args) {
        int choice;

        do {
            showMenu();
            choice = readInt("Choose: ");

            switch (choice) {
                case 1:
                    addDemoCalls();
                    break;
                case 2:
                    addCallByInput();
                    break;
                case 3:
                    serveNextCall();
                    break;
                case 4:
                    callQueue.showWaitingCalls();
                    break;
                case 0:
                    System.out.println("Exit program.");
                    break;
                default:
                    System.out.println("Invalid choice.");
                    break;
            }
        } while (choice != 0);
    }

    private static void showMenu() {
        System.out.println("\n===== Call Center Waiting Line System =====");
        System.out.println("1. Add demo calls");
        System.out.println("2. Add new call");
        System.out.println("3. Serve next call");
        System.out.println("4. Show waiting calls");
        System.out.println("0. Exit");
    }

    private static void addDemoCalls() {
        callQueue.addCall(new Call("Normal Customer", "0901000001", false, 0, orderNumber++));
        callQueue.addCall(new Call("VIP Customer", "0901000002", true, 0, orderNumber++));
        callQueue.addCall(new Call("Repeat Customer", "0901000003", false, 5, orderNumber++));

        System.out.println("Demo calls added.");
        callQueue.showWaitingCalls();
    }

    private static void addCallByInput() {
        String name = readText("Customer name: ");
        String phone = readText("Phone number: ");
        boolean vip = readYesNo("Is VIP? (y/n): ");
        int repeatCalls = readInt("Repeat calls: ");

        Call call = new Call(name, phone, vip, repeatCalls, orderNumber++);
        callQueue.addCall(call);

        System.out.println("New call added.");
    }

    private static void serveNextCall() {
        if (callQueue.isEmpty()) {
            System.out.println("No waiting calls.");
            return;
        }

        Call nextCall = callQueue.getNextCall();
        System.out.println("Serving: " + nextCall);
    }

    private static String readText(String message) {
        System.out.print(message);
        return scanner.nextLine();
    }

    private static int readInt(String message) {
        while (true) {
            try {
                System.out.print(message);
                return Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException error) {
                System.out.println("Please enter a number.");
            }
        }
    }

    private static boolean readYesNo(String message) {
        while (true) {
            System.out.print(message);
            String answer = scanner.nextLine();

            if (answer.equalsIgnoreCase("y")) {
                return true;
            }

            if (answer.equalsIgnoreCase("n")) {
                return false;
            }

            System.out.println("Please enter y or n.");
        }
    }
}
