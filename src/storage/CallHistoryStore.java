package storage;

import model.Call;
import model.CallStatus;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages history of successfully processed calls.
 * 
 * Functions:
 * - Saves completed calls into a CSV file
 * - Loads all call history
 * - Searches calls by keyword (name, phone, customer ID)
 * 
 * CSV format: customerId,customerName,phoneNumber,isVIP,repeatCalls,orderNumber,priorityScore,status
 */
public class CallHistoryStore {

    private FileHandler fileHandler;

    /**
     * Initializes with default history file path.
     */
    public CallHistoryStore() {
        this("data/call_history.csv");
    }

    /**
     * Initializes with custom file path.
     */
    public CallHistoryStore(String filePath) {
        this.fileHandler = new FileHandler(filePath);
        initFileIfNeeded();
    }

    /**
     * Creates CSV header if file doesn't exist.
     */
    private void initFileIfNeeded() {
        if (!fileHandler.exists()) {
            clearHistory();
        }
    }

    /**
     * Clears history by creating a fresh file containing only the header.
     */
    public void clearHistory() {
        List<String> header = new ArrayList<>();
        header.add("No.| Cust ID| Cust Name| Phone| VIP| Repeats| Score| Status");
        fileHandler.writeLines(header);
    }

    /**
     * Saves processed call into history file.
     * 
     * @param call call to save
     */
    public void save(Call call) {
        fileHandler.appendLine(toCSV(call));
    }

    /**
     * Reads entire call history from file.
     * 
     * @return list of processed calls
     */
    public List<Call> loadAll() {
        List<Call> calls = new ArrayList<>();
        List<String> lines = fileHandler.readLines();

        // Skip header
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!line.isEmpty()) {
                try {
                    Call call = fromCSV(line);
                    if (call != null) {
                        calls.add(call);
                    }
                } catch (Exception e) {
                    // Skip corrupt line
                }
            }
        }

        return calls;
    }

    /**
     * Searches call history by keyword.
     * Searches in: customer name, phone number, customer ID.
     * 
     * @param keyword search keyword (case-insensitive)
     * @return list of matching calls
     */
    public List<Call> search(String keyword) {
        List<Call> all = loadAll();
        List<Call> results = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();

        for (Call call : all) {
            if (call.getCustomerName().toLowerCase().contains(lowerKeyword)
                    || call.getPhoneNumber().contains(keyword)
                    || call.getCustomerId().toLowerCase().contains(lowerKeyword)) {
                results.add(call);
            }
        }

        return results;
    }

    /**
     * Converts a Call object to CSV string.
     */
    private String toCSV(Call call) {
        return String.join("|",
                String.valueOf(call.getOrderNumber()),
                call.getCustomerId(),
                call.getCustomerName(),
                call.getPhoneNumber(),
                String.valueOf(call.isVIP()),
                String.valueOf(call.getRepeatCalls()),
                String.valueOf(call.getPriorityScore()),
                call.getStatus().name()
        );
    }

    /**
     * Parses a CSV string to Call object.
     */
    private Call fromCSV(String line) {
        String[] parts = line.split("\\|");
        if (parts.length < 8) return null;

        Call call = new Call(
                parts[1].trim(),
                parts[2].trim(),
                parts[3].trim(),
                Boolean.parseBoolean(parts[4].trim()),
                Integer.parseInt(parts[5].trim()),
                Integer.parseInt(parts[0].trim())
        );
        call.setPriorityScore(Integer.parseInt(parts[6].trim()));
        call.setStatus(CallStatus.valueOf(parts[7].trim()));

        return call;
    }
}
