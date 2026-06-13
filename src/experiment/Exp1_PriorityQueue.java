package experiment;

import model.Call;
import model.CallStatus;
import java.util.*;

/**
 * Thực nghiệm 1: So sánh cấu trúc Hàng đợi Kép tách biệt (Dual Queue) 
 * với Hàng đợi Đơn tích hợp cơ chế chống nghẽn (Single Queue + Aging).
 * * Mục tiêu: Đánh giá giải pháp nào tối ưu thời gian chờ trung bình (AWT) cho khách thường hơn.
 * @author Group 7
 */
public class Exp1_PriorityQueue {

    private static final int NUM_AGENTS = 5; // Số điện thoại viên cố định
    private static final int SIM_DURATION_SECONDS = 4 * 3600; // Mô phỏng chạy trong 4 tiếng (giây)
    private static final double CALL_RATE_PER_SECOND = 500.0 / 3600.0; // ~8.33 cuộc gọi/phút
    private static final int AGING_INTERVAL_SECONDS = 60; // Tiến hành aging sau mỗi 60 giây
    private static final int AGING_BOOST_POINTS = 15; // Điểm cộng thêm để cạnh tranh công bằng với điểm VIP (50)

    // Lớp wrapper lưu trữ thông tin cuộc gọi phục vụ quá trình mô phỏng timeline
    static class SimCall {
        Call call;
        int arrivalTime;   // Thời điểm đến (giây vật lý ảo)
        int handlingTime;  // Thời gian xử lý ngẫu nhiên của Agent (giây)
        int waitTime = -1; // Kết quả thời gian chờ tính được (giây)

        SimCall(Call call, int arrivalTime, int handlingTime) {
            this.call = call;
            this.arrivalTime = arrivalTime;
            this.handlingTime = handlingTime;
        }
    }

    public void run() {
        System.out.println("\n==================================================================");
        System.out.println("🧪 EXPERIMENT 1: DUAL QUEUE VS SINGLE QUEUE WITH AGING ALGORITHM");
        System.out.println("==================================================================");

        // Sinh dữ liệu đồng nhất (Deterministic Input Data) bằng cách khóa Seed ngẫu nhiên
        List<SimCall> datasetA = generateDeterministicDataset(12345);
        List<SimCall> datasetB = cloneDataset(datasetA);

        System.out.println("  [i] Running simulation Scenario A (Dual Queue - Absolute VIP)...");
        runDualQueueSimulation(datasetA);

        System.out.println("  [i] Running simulation Scenario B (Single Queue + Aging Mechanism)...");
        runSingleQueueAgingSimulation(datasetB);

        // Xuất báo cáo so sánh chi tiết
        printComparativeReport(datasetA, datasetB);
    }

    /**
     * Sinh tập dữ liệu cuộc gọi ngẫu nhiên dựa trên phân phối Poisson (khoảng cách thời gian exponential)
     */
    private List<SimCall> generateDeterministicDataset(long seed) {
        Random rand = new Random(seed);
        List<SimCall> list = new ArrayList<>();
        int currentTime = 0;
        int orderCounter = 1;

        while (currentTime < SIM_DURATION_SECONDS) {
            double u = rand.nextDouble();
            while (u == 0) u = rand.nextDouble();
            // Công thức tính thời gian cuộc gọi kế tiếp đến dựa trên mật độ lưu lượng mong muốn
            int nextArrivalInterval = (int) (-Math.log(1 - u) / CALL_RATE_PER_SECOND);
            if (nextArrivalInterval < 1) nextArrivalInterval = 1;
            currentTime += nextArrivalInterval;

            if (currentTime >= SIM_DURATION_SECONDS) break;

            boolean isVip = rand.nextDouble() < 0.20; // Tỷ lệ VIP đúng 20%
            int repeatCalls = rand.nextInt(100) < 15 ? rand.nextInt(3) + 1 : 0; 
            int handlingTime = (rand.nextInt(4) + 2) * 60; // Ngẫu nhiên từ 2 đến 5 phút (đổi ra giây)

            String id = "C" + String.format("%04d", orderCounter);
            Call call = new Call(id, "Customer " + id, "090" + String.format("%07d", rand.nextInt(10000000)), isVip, repeatCalls, orderCounter);
            
            list.add(new SimCall(call, currentTime, handlingTime));
            orderCounter++;
        }
        return list;
    }

    private List<SimCall> cloneDataset(List<SimCall> original) {
        List<SimCall> clone = new ArrayList<>();
        for (SimCall sc : original) {
            Call oc = sc.call;
            Call nc = new Call(oc.getCustomerId(), oc.getCustomerName(), oc.getPhoneNumber(), oc.isVIP(), oc.getRepeatCalls(), oc.getOrderNumber());
            nc.setPriorityScore(oc.getPriorityScore());
            clone.add(new SimCall(nc, sc.arrivalTime, sc.handlingTime));
        }
        return clone;
    }

    /**
     * KỊCH BẢN A: Khởi tạo 2 hàng đợi hoàn toàn riêng biệt.
     * Điện thoại viên chỉ lấy cuộc gọi thường khi hàng đợi VIP trống rỗng.
     */
    private void runDualQueueSimulation(List<SimCall> dataset) {
        int[] agentFreeTime = new int[NUM_AGENTS];
        List<SimCall> vipQueue = new ArrayList<>();
        List<SimCall> regularQueue = new ArrayList<>();
        
        int callIndex = 0;
        int totalCalls = dataset.size();
        int processedCount = 0;
        int t = 0;

        while (processedCount < totalCalls) {
            // Đưa cuộc gọi vào hàng đợi tương ứng khi đến mốc thời gian xuất hiện
            while (callIndex < totalCalls && dataset.get(callIndex).arrivalTime <= t) {
                SimCall sc = dataset.get(callIndex);
                if (sc.call.isVIP()) vipQueue.add(sc);
                else regularQueue.add(sc);
                callIndex++;
            }

            // Phân phối cuộc gọi cho các Agent đang rảnh tay
            for (int i = 0; i < NUM_AGENTS; i++) {
                if (agentFreeTime[i] <= t) {
                    SimCall nextCall = null;
                    if (!vipQueue.isEmpty()) {
                        nextCall = vipQueue.remove(0); // Lấy khách VIP trước
                    } else if (!regularQueue.isEmpty()) {
                        nextCall = regularQueue.remove(0); // Không có VIP mới xử lý khách thường
                    }

                    if (nextCall != null) {
                        nextCall.waitTime = t - nextCall.arrivalTime;
                        agentFreeTime[i] = t + nextCall.handlingTime;
                        processedCount++;
                    }
                }
            }
            t++;
        }
    }

    /**
     * KỊCH BẢN B: Sử dụng 1 hàng đợi ưu tiên duy nhất.
     * Cứ sau mỗi phút, khách thường chưa được phục vụ sẽ được tăng điểm ưu tiên (Aging).
     */
    private void runSingleQueueAgingSimulation(List<SimCall> dataset) {
        int[] agentFreeTime = new int[NUM_AGENTS];
        List<SimCall> priorityQueue = new ArrayList<>();
        
        int callIndex = 0;
        int totalCalls = dataset.size();
        int processedCount = 0;
        int t = 0;

        while (processedCount < totalCalls) {
            while (callIndex < totalCalls && dataset.get(callIndex).arrivalTime <= t) {
                priorityQueue.add(dataset.get(callIndex));
                callIndex++;
            }

            // Thuật toán Tăng tuổi (Aging Mechanism) áp dụng sau mỗi 60 giây ảo trôi qua
            if (t > 0 && t % AGING_INTERVAL_SECONDS == 0) {
                for (SimCall sc : priorityQueue) {
                    if (!sc.call.isVIP()) {
                        // Tăng biến waitTime nội tại để nâng điểm getAgedPriority() lên
                        sc.call.setWaitTime(sc.call.getWaitTime() + AGING_BOOST_POINTS);
                    }
                }
            }

            // Phân phối cuộc gọi dựa trên tổng điểm Priority Score (Đã cộng dồn điểm Aging)
            for (int i = 0; i < NUM_AGENTS; i++) {
                if (agentFreeTime[i] <= t) {
                    if (!priorityQueue.isEmpty()) {
                        int highestPriorityIndex = 0;
                        for (int j = 1; j < priorityQueue.size(); j++) {
                            int p1 = priorityQueue.get(j).call.getPriorityScore() + priorityQueue.get(j).call.getWaitTime();
                            int p2 = priorityQueue.get(highestPriorityIndex).call.getPriorityScore() + priorityQueue.get(highestPriorityIndex).call.getWaitTime();
                             
                            if (p1 > p2) {
                                highestPriorityIndex = j;
                            } else if (p1 == p2) {
                                // Nếu bằng điểm nhau, ai đến trước xếp trước (FIFO trích xuất)
                                if (priorityQueue.get(j).arrivalTime < priorityQueue.get(highestPriorityIndex).arrivalTime) {
                                    highestPriorityIndex = j;
                                }
                            }
                        }

                        SimCall nextCall = priorityQueue.remove(highestPriorityIndex);
                        nextCall.waitTime = t - nextCall.arrivalTime;
                        agentFreeTime[i] = t + nextCall.handlingTime;
                        processedCount++;
                    }
                }
            }
            t++;
        }
    }

    private void printComparativeReport(List<SimCall> datasetA, List<SimCall> datasetB) {
        double vipAwtA = 0, regAwtA = 0, totalAwtA = 0;
        int vipCount = 0, regCount = 0;
        
        for (SimCall sc : datasetA) {
            if (sc.call.isVIP()) { vipAwtA += sc.waitTime; vipCount++; }
            else { regAwtA += sc.waitTime; regCount++; }
            totalAwtA += sc.waitTime;
        }
        vipAwtA /= vipCount; regAwtA /= regCount; totalAwtA /= datasetA.size();

        double vipAwtB = 0, regAwtB = 0, totalAwtB = 0;
        for (SimCall sc : datasetB) {
            if (sc.call.isVIP()) vipAwtB += sc.waitTime;
            else regAwtB += sc.waitTime;
            totalAwtB += sc.waitTime;
        }
        vipAwtB /= vipCount; regAwtB /= regCount; totalAwtB /= datasetB.size();

        System.out.println("\n📊 AVERAGE WAIT TIME (AWT) COMPARISON TABLE");
        System.out.println("┌──────────────────────────┬──────────────────────────┬──────────────────────────┐");
        System.out.println("│ Customer Classification  │ Scenario A (Dual Queue)  │ Scenario B (Aging Queue) │");
        System.out.println("├──────────────────────────┼──────────────────────────┼──────────────────────────┤");
        System.out.printf("│ Regular Customer         │       %8.2f seconds    │       %8.2f seconds    │%n", regAwtA, regAwtB);
        System.out.printf("│ VIP Customer             │       %8.2f seconds    │       %8.2f seconds    │%n", vipAwtA, vipAwtB);
        System.out.printf("│ OVERALL SYSTEM           │       %8.2f seconds    │       %8.2f seconds    │%n", totalAwtA, totalAwtB);
        System.out.println("└──────────────────────────┴──────────────────────────┴──────────────────────────┘");
        
        // Ghi kết quả so sánh ra file CSV
        try {
            config.ConfigLoader loader = new config.ConfigLoader();
            String csvPath = loader.resolvePath("data/Exp1_PriorityQueue.csv");
            storage.FileHandler fh = new storage.FileHandler(csvPath);
            List<String> csvLines = new ArrayList<>();
            csvLines.add("Customer Classification,Scenario A (Dual Queue) (seconds),Scenario B (Aging Queue) (seconds)");
            csvLines.add(String.format(Locale.US, "Regular Customer,%.2f,%.2f", regAwtA, regAwtB));
            csvLines.add(String.format(Locale.US, "VIP Customer,%.2f,%.2f", vipAwtA, vipAwtB));
            csvLines.add(String.format(Locale.US, "OVERALL SYSTEM,%.2f,%.2f", totalAwtA, totalAwtB));
            fh.writeLines(csvLines);
            System.out.println("  [✓] Simulation results exported successfully to " + csvPath);
        } catch (Exception e) {
            System.err.println("  [!] Failed to write CSV file: " + e.getMessage());
        }

        System.out.println("\n💡 EXPERIMENT 1 CONCLUSION:");
        if (regAwtB < regAwtA) {
            System.out.println("  [✓] Scenario B (Single Queue + Aging) is significantly more optimal for regular customers.");
            System.out.println("      The algorithm thoroughly prevents 'resource starvation' without causing too negative");
            System.out.println("      an impact on the VIP customer experience index.");
        } else {
            System.out.println("  [!] Scenario A is absolutely optimal for VIPs but makes regular customers wait for too long.");
        }
    }
}