package experiment;

/**
 * Lớp chạy tất cả thực nghiệm để kiểm tra.
 */
public class RunAllExperiments {
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("===     CALL CENTER WAITING LINE SYSTEM EXPERIMENTS     ===");
        System.out.println("==================================================");

        System.out.println("\n--- [EXP 1] PriorityQueueExperiment");
        new Exp1_PriorityQueue().run();

        System.out.println("\n--- [EXP 2] AgingAlgorithmExperiment");
        new Exp2_AgingAlgorithm().run();

        System.out.println("\n--- [EXP 3] HistoryLookupExperiment");
        new Exp3_HistoryLookup().run();

        System.out.println("\n==================================================");
        System.out.println("DONE! ALL EXPERIMENTS COMPLETED!");
        System.out.println("==================================================");
    }
}
