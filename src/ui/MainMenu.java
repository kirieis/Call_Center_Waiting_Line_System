package ui;

import config.ConfigLoader;
import core.CallProcessor;
import core.CallRouter;
import model.Call;
import model.CallStatus;
import storage.CallHistoryStore;
import storage.DataGenerator;
import experiment.Exp1_PriorityQueue;
import experiment.Exp2_AgingAlgorithm;
import experiment.Exp3_CallbackFairness;

import java.util.List;

/**
 * Main menu loop of the Call Center system.
 * 
 * Provides a Console interface for the Operator with functions:
 * 1. Generate Random Data (10,000 calls)
 * 2. Auto Sort & Load to Queue
 * 3. Add New Call Manually
 * 4. View Waiting Queue (Priority)
 * 5. Process Next Call
 * 6. View Call History
 * 7. Search Call History
 * 8. Apply Aging Algorithm
 * 9. View Circular Queue Status
 * 10. Run Experiments
 * 0. Exit
 */
public class MainMenu {

    private CallRouter router;
    private CallHistoryStore historyStore;
    private InputHandler input;
    private ConsoleRenderer renderer;
    private CallProcessor processor;
    private DataGenerator dataGen;
    private ConfigLoader configLoader;

    // Default paths and values
    private String rawDataPath;
    private String historyPath;
    private int circularCapacity;
    private int generateCount;
    private int processMultipleCount;

    public MainMenu() {
        this.configLoader = new ConfigLoader();
        loadConfig();
        this.router = new CallRouter(circularCapacity);
        this.historyStore = new CallHistoryStore(historyPath);
        this.input = new InputHandler();
        this.renderer = new ConsoleRenderer();
        this.processor = new CallProcessor();
        this.dataGen = new DataGenerator();
    }

    /**
     * Reads configuration from settings.properties file.
     */
    private void loadConfig() {
        rawDataPath = configLoader.getRawDataPath();
        historyPath = configLoader.getHistoryPath();
        circularCapacity = configLoader.getCircularQueueCapacity();
        generateCount = configLoader.getGenerateCount();
        processMultipleCount = configLoader.getProcessMultipleCount();

        // Configure priority scoring
        int vipBonus = configLoader.getVipBonus();
        int repeatMul = configLoader.getRepeatMultiplier();
        Call.setVipBonus(vipBonus);
        Call.setRepeatMultiplier(repeatMul);
    }

    /**
     * Runs the main menu loop.
     */
    public void run() {
        boolean running = true;

        while (running) {
            display();
            int choice = input.readInt("  Enter choice: ");
            running = handleChoice(choice);
        }

        input.close();
        System.out.println("\n   Thank you for using the system! Goodbye. \n");
    }

    /**
     * Displays the main menu.
     */
    public void display() {
        String[] options = {
                "1.  Generate random data (" + generateCount + " calls)",
                "2.  Auto sort & load to queue",
                "3.  Add new call (manual)",
                "4.  View waiting queue (Priority Queue)",
                "5.  Process next call",
                "6.  View call history",
                "7.  Search call history",
                "8.  Apply Aging algorithm",
                "9.  View Circular Queue status",
                "10. Run experiments",
                "11. Process multiple calls (" + processMultipleCount + " calls)",
                "",
                "0.  Exit"
        };
        renderer.renderMenu(options);
    }

    /**
     * Handles selection from the menu.
     * 
     * @return false if operator selects exit
     */
    private boolean handleChoice(int choice) {
        switch (choice) {
            case 1:
                generateData();
                break;
            case 2:
                autoSortAndLoad();
                break;
            case 3:
                addNewCall();
                break;
            case 4:
                viewWaitingQueue();
                break;
            case 5:
                processNextCall();
                break;
            case 6:
                viewCallHistory();
                break;
            case 7:
                searchCallHistory();
                break;
            case 8:
                applyAging();
                break;
            case 9:
                viewCircularQueue();
                break;
            case 10:
                runExperiments();
                break;
            case 11:
                processMultipleCalls();
                break;
            case 0:
                return false;
            default:
                renderer.renderMessage("Invalid choice. Please choose 0-11.");
        }
        return true;
    }

    // ===== Function 1: Generate random data =====
    private void generateData() {
        System.out.println("\n  ─── GENERATE RANDOM DATA ───");
        dataGen.generate(generateCount, rawDataPath);
        historyStore.clearHistory();
        renderer.renderMessage("call_history.csv has been cleared and reset.");
    }

    // ===== Function 2: Auto Sort & Load =====
    private void autoSortAndLoad() {
        System.out.println("\n  ─── AUTO SORT & LOAD QUEUE ───");
        processor.reset();
        processor.loadFromCSV(rawDataPath);

        // Assign sorted queue to router
        router.setPriorityQueue(processor.getQueue());
        renderer.renderMessage("Priority queue is ready. Total: "
                + router.getQueueSize() + " calls.");
    }

    // ===== Function 3: Add new call manually =====
    private void addNewCall() {
        System.out.println("\n  ─── ADD NEW CALL ───");

        String customerId = input.readString("  Enter Customer ID: ");
        String customerName = input.readString("  Enter Customer Name: ");
        String phoneNumber = input.readPhoneNumber();
        boolean isVIP = input.readBoolean("  VIP Customer?");
        int repeatCalls = input.readInt("  Number of repeat calls: ");

        Call call = new Call(customerId, customerName, phoneNumber,
                isVIP, repeatCalls, router.getQueueSize() + 1);

        router.addCall(call);
        renderer.renderMessage("Call added successfully!");
        renderer.renderCall(call);
    }

    // ===== Function 4: View waiting queue =====
    private void viewWaitingQueue() {
        System.out.println("\n  ─── WAITING QUEUE (PRIORITY QUEUE) ───");
        List<Call> snapshot = router.getQueueSnapshot();
        renderer.renderQueue(snapshot, false, "boolean");
    }

    // ===== Function 5: Process next call =====
    private void processNextCall() {
        System.out.println("\n  ─── PROCESS NEXT CALL ───");
        Call call = router.processNext();

        if (call == null) {
            renderer.renderMessage("No calls in queue.");
            return;
        }

        renderer.renderMessage("Processing call:");
        renderer.renderCall(call);

        // Randomly set status: 70% COMPLETED, 30% MISSED
        java.util.Random rand = new java.util.Random();
        if (rand.nextInt(100) < 70) {
            call.setStatus(CallStatus.COMPLETED);
        } else {
            call.setStatus(CallStatus.MISSED);
        }

        historyStore.save(call);
        renderer.renderMessage("Call processed (Status: " + call.getStatus() + ") and saved to history.");
        renderer.renderMessage("Remaining in queue: " + router.getQueueSize());
    }

    // ===== Function 6: View call history =====
    private void viewCallHistory() {
        System.out.println("\n  ─── CALL HISTORY ───");
        List<Call> history = historyStore.loadAll();
        renderer.renderHistory(history);
    }

    // ===== Function 7: Search call history =====
    private void searchCallHistory() {
        System.out.println("\n  ─── SEARCH CALL HISTORY ───");
        String keyword = input.readString("  Enter keyword (name, phone, customer ID): ");
        List<Call> results = historyStore.search(keyword);

        if (results.isEmpty()) {
            renderer.renderMessage("No calls matching: \"" + keyword + "\"");
        } else {
            renderer.renderMessage("Found " + results.size() + " results:");
            renderer.renderQueue(results, false, "question");
        }
    }

    // ===== Function 11: Process multiple calls =====
    private void processMultipleCalls() {
        System.out.println("\n  ─── PROCESS MULTIPLE CALLS ───");
        int countToProcess = processMultipleCount;
        int processed = 0;
        java.util.Random rand = new java.util.Random();

        for (int i = 0; i < countToProcess; i++) {
            Call call = router.processNext();
            if (call == null) {
                break;
            }

            // Randomly set status: 70% COMPLETED, 30% MISSED
            if (rand.nextInt(100) < 70) {
                call.setStatus(CallStatus.COMPLETED);
            } else {
                call.setStatus(CallStatus.MISSED);
            }

            historyStore.save(call);
            processed++;
        }

        if (processed == 0) {
            renderer.renderMessage("No calls in queue to process.");
        } else {
            renderer.renderMessage("Successfully processed " + processed + " calls.");
            renderer.renderMessage("Remaining in queue: " + router.getQueueSize());
        }
    }

    // ===== Function 8: Apply Aging Algorithm =====
    private void applyAging() {
        System.out.println("\n  ─── APPLY AGING ALGORITHM ───");
        if (router.isQueueEmpty()) {
            renderer.renderMessage("Queue is empty. Nothing to age.");
            return;
        }

        int sizeBefore = router.getQueueSize();
        router.applyAging();
        renderer.renderMessage("Applied Aging Algorithm to " + sizeBefore + " calls.");
        renderer.renderMessage("Queue priorities have been updated.");
    }

    // ===== Function 9: View Circular Queue =====
    private void viewCircularQueue() {
        System.out.println("\n  ─── CIRCULAR QUEUE STATUS ───");
        List<Call> circularCalls = router.getCircularQueueSnapshot();
        int capacity = router.getCircularQueue().getCapacity();
        renderer.renderCircularQueueStatus(circularCalls, capacity);
    }

    // ===== Function 10: Run experiments =====
    private void runExperiments() {
        System.out.println("\n  ─── RUN EXPERIMENTS ───");
        String[] expOptions = {
                "1. Exp1 - Priority Queue Performance",
                "2. Exp2 - Aging Algorithm Test",
                "3. Exp3 - Call Back Fairness Queue",
                "4. Run all",
                "0. Go back"
        };

        for (String opt : expOptions) {
            System.out.println("  " + opt);
        }

        int expChoice = input.readInt("  Select experiment: ");

        switch (expChoice) {
            case 1:
                new Exp1_PriorityQueue().run();
                break;
            case 2:
                new Exp2_AgingAlgorithm().run();
                break;
            case 3:
                new Exp3_CallbackFairness().run();
                break;
            case 4:
                System.out.println("\n  === Running all experiments ===\n");
                new Exp1_PriorityQueue().run();
                System.out.println();
                new Exp2_AgingAlgorithm().run();
                System.out.println();
                new Exp3_CallbackFairness().run();
                break;
            case 0:
                break;
            default:
                renderer.renderMessage("Invalid choice.");
        }
    }
}
