package storage;

import model.Call;
import java.util.List;
import java.util.ArrayList;

public class CallHistoryStore {
    private FileHandler fileHandler;

    public CallHistoryStore(String filePath) {
        this.fileHandler = new FileHandler(filePath);
    }

    public void save(Call call) {
        // save skeleton
    }

    public List<Call> loadAll() {
        return null;
    }

    public List<Call> search(String keyword) {
        return null;
    }

    private String toCSV(Call call) {
        return "";
    }

    private Call fromCSV(String line) {
        return null;
    }
}
