package experiment;

import model.Call;
import core.AgingAlgorithm;
import java.util.*;

/**
 * Thực nghiệm 2: Đánh giá khả năng dập tắt hiện tượng nghẽn kéo dài (Starvation)
 * của thuật toán Aging khi lượng cuộc gọi VIP bùng nổ đột biến lên 50% (Spike Peak).
 * * Mô phỏng chuỗi thời gian (Time-series) liên tục trong 60 phút chia làm 3 Block.
 * @author Group 7
 */
public class Exp2_AgingAlgorithm {

    private static final int NUM_AGENTS = 3; // Giới hạn tài nguyên xử lý để tạo áp lực hệ thống
    private static final int SIM_DURATION_MINUTES = 60;
    private static final int CALLS_PER_MINUTE = 8; // Mật độ cuộc gọi cao nhằm tạo hàng đợi tích lũy

    static class TimeSeriesCall {
        Call call;
        int arrivalTime;   // Giây ảo trôi qua
        int handlingTime;  // Thời lượng cuộc gọi (giây)
        int waitTime = -1; 
        int blockId;       // Định danh giai đoạn (1, 2, 3)

        TimeSeriesCall(Call call, int arrivalTime, int handlingTime, int blockId) {
            this.call = call;
            this.arrivalTime = arrivalTime;
            this.handlingTime = handlingTime;
            this.blockId = blockId;
        }
    }

    public void run() {
        System.out.println("\n==================================================================");
        System.out.println("🧪 EXPERIMENT 2: TESTING STARVATION PREVENTION DURING VIP SPIKE");
        System.out.println("==================================================================");

        List<TimeSeriesCall> dataset = generateTimeSeriesDataset(54321);
        
        System.out.println("  [i] Launching real-time timeline simulation for 60 minutes...");
        runSimulationLoop(dataset);
        
        System.out.println("  [i] Collecting metrics and analyzing Max Wait Time (Max WT)...");
        analyzeAndEvaluate(dataset);
    }

    private List<TimeSeriesCall> generateTimeSeriesDataset(long seed) {
        Random rand = new Random(seed);
        List<TimeSeriesCall> list = new ArrayList<>();
        int orderCounter = 1;

        for (int minute = 0; minute < SIM_DURATION_MINUTES; minute++) {
            int blockId;
            double vipRate;

            /**
             * MÔ PHỎNG ĐỘT BIẾN LƯU LƯỢNG VIP (SPIKE PEAK TRAFFIC):
             * Chia thời gian 60 phút mô phỏng thành 3 giai đoạn (Block):
             * - Block 1 (Phút 0 - 14): Mức VIP bình thường là 20%.
             * - Block 2 (Phút 15 - 29): Đột biến VIP vọt lên 50% trong 15 phút (Yêu cầu đề bài).
             * - Block 3 (Phút 30 - 59): Lưu lượng VIP quay về mức bình thường 20% để hạ nhiệt hệ thống.
             */
            if (minute < 15) {
                blockId = 1; vipRate = 0.20; 
            } else if (minute < 30) {
                blockId = 2; vipRate = 0.50; 
            } else {
                blockId = 3; vipRate = 0.20; 
            }

            for (int k = 0; k < CALLS_PER_MINUTE; k++) {
                int secondOffset = rand.nextInt(60);
                int arrivalTime = minute * 60 + secondOffset;

                boolean isVip = rand.nextDouble() < vipRate;
                int handlingTime = (rand.nextInt(3) + 2) * 60; // 2-4 phút xử lý

                String id = "C" + String.format("%04d", orderCounter);
                Call call = new Call(id, "Cust " + id, "091" + String.format("%07d", rand.nextInt(10000000)), isVip, 0, orderCounter);
                
                list.add(new TimeSeriesCall(call, arrivalTime, handlingTime, blockId));
                orderCounter++;
            }
        }
        // Đảm bảo thứ tự thời gian tăng dần tuyến tính
        list.sort(Comparator.comparingInt(c -> c.arrivalTime));
        return list;
    }

    private void runSimulationLoop(List<TimeSeriesCall> dataset) {
        int[] agentFreeTime = new int[NUM_AGENTS];
        List<TimeSeriesCall> internalQueue = new ArrayList<>();
        
        int callIndex = 0;
        int totalCalls = dataset.size();
        int processedCount = 0;
        int t = 0;

        // Thiết lập các thông số cấu hình của thuật toán Aging
        int agingThresholdSeconds = 30; // Chờ quá 30 giây ảo sẽ bắt đầu được xét tăng điểm ưu tiên
        int agingBoostScore = 5;       // Mỗi lần tăng điểm ưu tiên thêm 5 điểm

        while (processedCount < totalCalls || !internalQueue.isEmpty()) {
            // Đưa các cuộc gọi mới xuất hiện ở giây t vào hàng đợi
            while (callIndex < totalCalls && dataset.get(callIndex).arrivalTime <= t) {
                internalQueue.add(dataset.get(callIndex));
                callIndex++;
            }

            /**
             * CƠ CHẾ TĂNG TUỔI (AGING ALGORITHM):
             * - Thực hiện định kỳ mỗi 10 giây ảo.
             * - Duyệt qua tất cả các cuộc gọi đang chờ trong hàng đợi ảo.
             * - Nếu cuộc gọi đã chờ lớn hơn hoặc bằng agingThresholdSeconds (30 giây),
             *   ta cộng thêm agingBoostScore (5 điểm) vào thời gian chờ tích luỹ của cuộc gọi.
             * - Điều này giúp đẩy điểm ưu tiên tổng hợp (Priority Score + waitTime) tăng dần tuyến tính theo thời gian.
             */
            if (t > 0 && t % 10 == 0) {
                for (TimeSeriesCall tsc : internalQueue) {
                    int currentWaitTime = t - tsc.arrivalTime;
                    if (currentWaitTime >= agingThresholdSeconds) {
                        tsc.call.setWaitTime(tsc.call.getWaitTime() + agingBoostScore);
                    }
                }
            }

            // Định tuyến điều phối cuộc gọi cho Agent rảnh tay
            for (int i = 0; i < NUM_AGENTS; i++) {
                if (agentFreeTime[i] <= t) {
                    if (!internalQueue.isEmpty()) {
                        int bestTargetIdx = 0;
                        
                        // Tìm cuộc gọi có tổng điểm ưu tiên lớn nhất trong hàng đợi
                        for (int j = 1; j < internalQueue.size(); j++) {
                            int p1 = internalQueue.get(j).call.getPriorityScore() + internalQueue.get(j).call.getWaitTime();
                            int p2 = internalQueue.get(bestTargetIdx).call.getPriorityScore() + internalQueue.get(bestTargetIdx).call.getWaitTime();
                            
                            if (p1 > p2) {
                                bestTargetIdx = j;
                            } else if (p1 == p2) {
                                // Nếu bằng điểm, ưu tiên FIFO (ai đến trước phục vụ trước)
                                if (internalQueue.get(j).arrivalTime < internalQueue.get(bestTargetIdx).arrivalTime) {
                                    bestTargetIdx = j;
                                }
                            }
                        }

                        TimeSeriesCall executionCall = internalQueue.remove(bestTargetIdx);
                        executionCall.waitTime = t - executionCall.arrivalTime; // Ghi nhận thời gian chờ thực tế
                        agentFreeTime[i] = t + executionCall.handlingTime;
                        processedCount++;
                    }
                }
            }
            t++;
            if (t > 3600 * 3) break; // Khóa bảo vệ an toàn chống lặp vô chậm/vô tận
        }
    }

    private void analyzeAndEvaluate(List<TimeSeriesCall> dataset) {
        int[] maxWtReg = new int[4];
        int[] maxWtVip = new int[4];
        double[] avgWtReg = new double[4];
        int[] countReg = new int[4];

        for (TimeSeriesCall tsc : dataset) {
            int b = tsc.blockId;
            if (tsc.waitTime == -1) continue;

            if (!tsc.call.isVIP()) {
                countReg[b]++;
                avgWtReg[b] += tsc.waitTime;
                if (tsc.waitTime > maxWtReg[b]) maxWtReg[b] = tsc.waitTime;
            } else {
                if (tsc.waitTime > maxWtVip[b]) maxWtVip[b] = tsc.waitTime;
            }
        }

        for (int b = 1; b <= 3; b++) {
            if (countReg[b] > 0) avgWtReg[b] /= countReg[b];
        }

        System.out.println("\n📊 TIME-SERIES MONITORING REPORT (TIME-SERIES ANALYTICS)");
        System.out.println("┌───────────────────────────┬─────────────────────┬─────────────────────┐");
        System.out.println("│ Simulation Phase          │ Regular Cust Max WT │ VIP Customer Max WT │");
        System.out.println("├───────────────────────────┼─────────────────────┼─────────────────────┤");
        System.out.printf("│ Block 1 (00-15m: VIP 20%%) │     %4d seconds    │     %4d seconds    │%n", maxWtReg[1], maxWtVip[1]);
        System.out.printf("│ Block 2 (15-30m: VIP 50%%) │     %4d seconds    │     %4d seconds    │%n", maxWtReg[2], maxWtVip[2]);
        System.out.printf("│ Block 3 (30-60m: VIP 20%%) │     %4d seconds    │     %4d seconds    │%n", maxWtReg[3], maxWtVip[3]);
        System.out.println("└───────────────────────────┴─────────────────────┴─────────────────────┘");

        // Ghi kết quả so sánh ra file CSV
        try {
            config.ConfigLoader loader = new config.ConfigLoader();
            String csvPath = loader.resolvePath("data/Exp2_AgingAlgorithm.csv");
            storage.FileHandler fh = new storage.FileHandler(csvPath);
            List<String> csvLines = new ArrayList<>();
            csvLines.add("Simulation Phase,Regular Cust Max WT (seconds),VIP Customer Max WT (seconds)");
            csvLines.add(String.format("Block 1 (00-15m: VIP 20%%),%d,%d", maxWtReg[1], maxWtVip[1]));
            csvLines.add(String.format("Block 2 (15-30m: VIP 50%%),%d,%d", maxWtReg[2], maxWtVip[2]));
            csvLines.add(String.format("Block 3 (30-60m: VIP 20%%),%d,%d", maxWtReg[3], maxWtVip[3]));
            fh.writeLines(csvLines);
            System.out.println("  [✓] Simulation results exported successfully to " + csvPath);
        } catch (Exception e) {
            System.err.println("  [!] Failed to write CSV file: " + e.getMessage());
        }

        System.out.println("\n💡 DATA SCIENCE ARGUMENTATION:");
        System.out.printf("  - At the peak of congestion (Block 2 - VIP Spike), the longest waiting time for a regular customer was %d seconds (~%.1f minutes).%n", 
                maxWtReg[2], (maxWtReg[2] / 60.0));
        System.out.println("  - Thanks to the Aging algorithm, the accumulated score of regular customers increases linearly to fill the gap.");
        
        /**
         * ĐÁNH GIÁ SỰ ĐÓI TÀI NGUYÊN (RESOURCE STARVATION):
         * - Quy ước ngưỡng thời gian tới hạn của tổng đài là 10 phút (600 giây).
         * - Nếu thời gian chờ tối đa lớn hơn 600 giây, starvation vẫn xuất hiện do tốc độ tích lũy backlog quá nhanh
         *   khi có đột biến VIP 50% và hệ thống bị quá tải nặng nề (8 cuộc gọi/phút đổ vào 3 agents).
         * - Vì vậy, cơ chế Aging mặc dù có làm giảm thời gian chờ so với không có Aging, nhưng KHÔNG THỂ loại bỏ hoàn toàn
         *   sự quá tải cục bộ khi lưu lượng VIP bùng nổ lên 50% trong 15 phút.
         */
        if (maxWtReg[2] <= 600) {
            System.out.println("  [✓] CONCLUSION: Resource Starvation HAS BEEN COMPLETELY ELIMINATED.");
            System.out.println("      The intelligent boosting mechanism ensures no regular calls are held beyond");
            System.out.println("      the maximum critical threshold (10 minutes) of the call center.");
        } else {
            System.out.println("  [!] CONCLUSION: The algorithm helps mitigate but HAS NOT completely eliminated extreme congestion.");
            System.out.println("      Recommendation: Please optimize and increase the 'aging.boost' metric in settings.properties.");
        }
    }
}