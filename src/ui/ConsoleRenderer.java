package ui;

import model.Call;
import java.util.List;

/**
 * Console UI rendering class.
 * 
 * Responsible for rendering:
 * - Main menu
 * - Call list table (with paging)
 * - Call details
 * - System messages
 */
public class ConsoleRenderer {

    private static final int PAGE_SIZE = 20;
    private static final String LINE_SEPARATOR = "═══════════════════════════════════════════════════════════════════════════════════════════════════";
    private static final String THIN_SEPARATOR = "───────────────────────────────────────────────────────────────────────────────────────────────────";

    /**
     * Displays call list in a table, with pagination and custom VIP formatting.
     * 
     * @param calls list of calls
     * @param paginate whether to paginate every PAGE_SIZE lines
     * @param vipMode display format for VIP status ("question", "boolean", or "default")
     */
    public void renderQueue(List<Call> calls, boolean paginate, String vipMode) {
        if (calls == null || calls.isEmpty()) {
            renderMessage("No calls in queue.");
            return;
        }

        System.out.println();
        System.out.println("  " + LINE_SEPARATOR);
        System.out.printf("  ║ %-5s │ %-8s │ %-22s │ %-12s │ %-5s │ %-7s │ %-7s │ %-10s ║%n",
                "No.", "Cust ID", "Cust Name", "Phone", "VIP", "Repeats", "Score", "Status");
        System.out.println("  " + LINE_SEPARATOR);

        for (int i = 0; i < calls.size(); i++) {
            Call call = calls.get(i);
            String vipStr;
            if ("question".equals(vipMode)) {
                vipStr = "?";
            } else if ("boolean".equals(vipMode)) {
                vipStr = call.isVIP() ? "true" : "false";
            } else {
                vipStr = call.isVIP() ? " ★" : " -";
            }

            System.out.printf("  ║ %-5d │ %-8s │ %-22s │ %-12s │ %-5s │ %-7d │ %-7d │ %-10s ║%n",
                    (i + 1),
                    truncate(call.getCustomerId(), 8),
                    truncate(call.getCustomerName(), 22),
                    call.getPhoneNumber(),
                    vipStr,
                    call.getRepeatCalls(),
                    call.getAgedPriority(),
                    call.getStatus());

            // Paging every PAGE_SIZE lines
            if (paginate && (i + 1) % PAGE_SIZE == 0 && i < calls.size() - 1) {
                System.out.println("  " + THIN_SEPARATOR);
                System.out.printf("  ║ --- Page %d/%d --- Showing %d/%d calls ---%n",
                        (i + 1) / PAGE_SIZE, (int) Math.ceil((double) calls.size() / PAGE_SIZE),
                        (i + 1), calls.size());
                System.out.println("  " + THIN_SEPARATOR);
            }
        }

        System.out.println("  " + LINE_SEPARATOR);
        System.out.println("  Total: " + calls.size() + " calls");
        System.out.println();
    }

    /**
     * Backward-compatible renderQueue.
     */
    public void renderQueue(List<Call> calls) {
        renderQueue(calls, true, "default");
    }

    /**
     * Displays call details.
     */
    public void renderCall(Call call) {
        if (call == null) {
            renderMessage("No call information.");
            return;
        }

        System.out.println();
        System.out.println("  ╔═══════════════════════════════════╗");
        System.out.println("  ║          CALL DETAILS             ║");
        System.out.println("  ╠═══════════════════════════════════╣");
        System.out.printf("  ║  Cust ID  : %-20s  ║%n", call.getCustomerId());
        System.out.printf("  ║  Cust Name: %-20s  ║%n", truncate(call.getCustomerName(), 20));
        System.out.printf("  ║  Phone    : %-20s  ║%n", call.getPhoneNumber());
        System.out.printf("  ║  VIP      : %-20s  ║%n", call.isVIP() ? "★ Yes" : "- No");
        System.out.printf("  ║  Repeats  : %-20d  ║%n", call.getRepeatCalls());
        System.out.printf("  ║  Priority : %-20d  ║%n", call.getAgedPriority());
        System.out.printf("  ║  Status   : %-19s  ║%n", call.getStatus());
        System.out.println("  ╚═══════════════════════════════════╝");
        System.out.println();
    }

    /**
     * Displays call history (reuses renderQueue).
     */
    public void renderHistory(List<Call> calls) {
        System.out.println("\n  ╔═════════════════════════════════════════╗");
        System.out.println("  ║            CALL HISTORY                 ║");
        System.out.println("  ╚═════════════════════════════════════════╝");
        renderQueue(calls, false, "question");
    }

    /**
     * Displays system message.
     */
    public void renderMessage(String msg) {
        System.out.println("\n  >> " + msg);
    }

    /**
     * Displays menu from option list.
     */
    public void renderMenu(String[] options) {
        System.out.println();
        System.out.println("  ╔═════════════════════════════════════════════════════════╗");
        System.out.println("  ║       CALL CENTER WAITING LINE SYSTEM                  ║");
        System.out.println("  ║       Call Queue Management System                      ║");
        System.out.println("  ╠═════════════════════════════════════════════════════════╣");

        for (String option : options) {
            System.out.printf("  ║   %-52s  ║%n", option);
        }

        System.out.println("  ╚═════════════════════════════════════════════════════════╝");
    }

    /**
     * Displays circular queue status.
     */
    public void renderCircularQueueStatus(List<Call> calls, int capacity) {
        System.out.println("\n  ╔═════════════════════════════════════════╗");
        System.out.println("  ║         CIRCULAR QUEUE STATUS           ║");
        System.out.println("  ╠═════════════════════════════════════════╣");
        System.out.printf("  ║  Capacity   : %d / %d                  ║%n", calls.size(), capacity);
        System.out.printf("  ║  Remaining  : %d                       ║%n", capacity - calls.size());
        System.out.printf("  ║  Fill Rate  : %.1f%%                   ║%n",
                (calls.size() * 100.0 / capacity));
        System.out.println("  ╚═════════════════════════════════════════╝");

        if (!calls.isEmpty()) {
            renderQueue(calls);
        }
    }

    /**
     * Clears console screen.
     */
    public void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    /**
     * Truncates string if too long.
     */
    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 2) + "..";
    }
}
