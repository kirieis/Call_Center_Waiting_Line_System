package experiment;

import model.Call;
import model.CallStatus;
import java.util.*;

/**
 * Thực nghiệm 1: So sánh cấu trúc Hàng đợi Kép tách biệt (Dual Queue) 
 * với Hàng đợi Đơn tích hợp cơ chế chống nghẽn (Single Queue + Aging).
 * * Mục tiêu: Đánh giá giải pháp nào tối ưu thời gian chờ trung bình (AWT) cho khách thường hơn.
 * * Cơ chế mô phỏng:
 * - Thời gian ảo (virtual clock) quản lý thời điểm cuộc gọi đến và thời lượng đàm thoại
 * (dùng phân phối Poisson 500 cuộc/giờ, handling time 2-5 phút) → tạo tình huống quá tải thực tế.
 * - Mỗi cuộc gọi khi được xử lý sẽ trải qua Thread.sleep(10-100ms) thực tế
 * để người dùng thấy quá trình chạy (tốc độ mô phỏng = 1/10 thời gian thật).
 * - Thời gian chờ được tính từ đồng hồ ảo (đơn vị: giây thời gian thật).
 * * @author Group 7
 */
public class Exp1_PriorityQueue {

    private static final int NUM_AGENTS = 5;
    private static final int SIM_DURATION_SECONDS = 1 * 3600; // Mô phỏng hành vi trong 1 giờ (3600 giây ảo)
    private static final double CALL_RATE_PER_SECOND = 500.0 / 3600.0; // Tần suất cuộc gọi dựa trên phân phối Poisson (500 cuộc/giờ)
    private static final int AGING_INTERVAL_SECONDS = 60; // Chu kỳ quét để tăng điểm ưu tiên (Aging boost) mỗi 60 giây ảo
    private static final int AGING_BOOST_POINTS = 15; // Điểm cộng thêm mỗi lần aging để kéo khách hàng thường lên đầu hàng đợi

    /**
     * Lớp wrapper bao bọc thực thể Call để bổ sung các thuộc tính mô phỏng.
     */
    static class SimCall {
        Call call;
        int arrivalTime;   // Thời điểm cuộc gọi đến (giây ảo)
        int handlingTime;  // Thời lượng đàm thoại Agent xử lý (giây ảo)
        int waitTime = -1; // Thời gian chờ thực tế được tính (giây ảo) = Thời điểm được phục vụ - Thời điểm đến

        SimCall(Call call, int arrivalTime, int handlingTime) {
            this.call = call;
            this.arrivalTime = arrivalTime;
            this.handlingTime = handlingTime;
        }
    }

    /**
     * Hàm khởi chạy thực nghiệm 1.
     */
    public void run() {
        System.out.println("\n==================================================================");
        System.out.println("EXPERIMENT 1: DUAL QUEUE VS SINGLE QUEUE WITH AGING");
        System.out.println("==================================================================");
        System.out.println("  Configuration: 500 calls/hour | 20% VIP | 5 Agents | Duration: 1 hour");
        System.out.println("  Simulation Speed: 1/10 (Processing delay: 10-100ms/call)");

        // Sinh dữ liệu đồng nhất bằng cách khóa Seed ngẫu nhiên (Giúp kết quả test giữa 2 kịch bản luôn công bằng)
    List<SimCall> datasetA = generateDeterministicDataset(new Random().nextInt(1000000));
    List<SimCall> datasetB = generateDeterministicDataset(new Random().nextInt(1000000));
        System.out.println("  Total Generated Calls: " + datasetA.size());
        System.out.println();

        System.out.println("  Running Scenario A (Dual Queue)...");
        long startA = System.currentTimeMillis();
        runDualQueueSimulation(datasetA);
        long durationA = System.currentTimeMillis() - startA;
        System.out.println("  Scenario A Execution Time: " + String.format("%.1f", durationA / 1000.0) + " s");
        System.out.println();

        System.out.println("  Running Scenario B (Single Queue + Aging)...");
        long startB = System.currentTimeMillis();
        runSingleQueueAgingSimulation(datasetB);
        long durationB = System.currentTimeMillis() - startB;
        System.out.println("  Scenario B Execution Time: " + String.format("%.1f", durationB / 1000.0) + " s");
        System.out.println();

        printComparativeReport(datasetA, datasetB);
    }

    /**
     * Sinh tập dữ liệu cuộc gọi ngẫu nhiên dựa trên phân phối Poisson.
     * Arrival rate: 500 cuộc/giờ ≈ 0.139 cuộc/giây.
     * Handling time: 2-5 phút (120-300 giây ảo).
     */
    private List<SimCall> generateDeterministicDataset(long seed) {
        Random rand = new Random(seed);
        List<SimCall> list = new ArrayList<>();
        int currentTime = 0;
        int orderCounter = 1;

        while (currentTime < SIM_DURATION_SECONDS) {
            double u = rand.nextDouble();
            while (u == 0) u = rand.nextDouble(); // Tránh lỗi log(0) khi tính phân phối Poisson

            // Áp dụng công thức tính khoảng thời gian giữa 2 cuộc gọi kế tiếp (Inter-arrival time) trong phân phối Poisson
            int nextArrivalInterval = (int) (-Math.log(1 - u) / CALL_RATE_PER_SECOND);
            if (nextArrivalInterval < 1) nextArrivalInterval = 1; // Đảm bảo đồng hồ ảo luôn tiến lên ít nhất 1 giây
            currentTime += nextArrivalInterval;

            if (currentTime >= SIM_DURATION_SECONDS) break;

            boolean isVip = rand.nextDouble() < 0.20; // 20% tỷ lệ cuộc gọi là VIP
            int repeatCalls = rand.nextInt(100) < 15 ? rand.nextInt(3) + 1 : 0;
            int handlingTime = (rand.nextInt(4) + 2) * 60; // Ngẫu nhiên từ 2-5 phút (quy đổi ra 120-300 giây ảo)

            String id = "C" + String.format("%04d", orderCounter);
            Call call = new Call(id, "Customer " + id, "090" + String.format("%07d", rand.nextInt(10000000)), isVip, repeatCalls, orderCounter);

            list.add(new SimCall(call, currentTime, handlingTime));
            orderCounter++;
        }
        return list;
    }

    /**
     * Sao chép sâu (deep clone) tập dữ liệu để tránh việc Kịch bản A thay đổi thuộc tính làm sai lệch Kịch bản B.
     */
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
     * In thanh tiến trình mô phỏng trực quan trên cùng một dòng bằng ký tự điều khiển \r.
     */
    private void printProgress(int processed, int total, String scenarioName) {
        int barWidth = 30;
        double percent = (double) processed / total;
        int filled = (int) (percent * barWidth);
        StringBuilder sb = new StringBuilder("\r  [");
        for (int i = 0; i < barWidth; i++) {
            if (i < filled) sb.append("█");
            else sb.append("░");
        }
        sb.append(String.format("] %d/%d (%d%%) - %s", processed, total, (int)(percent * 100), scenarioName));
        System.out.print(sb.toString());
        if (processed == total) {
            System.out.println();
        }
    }

    /**
     * Thực hiện Thread.sleep ngẫu nhiên 10-100ms cho mỗi cuộc gọi xử lý.
     * Đây là quá trình mô phỏng độ trễ xử lý thực tế mà người dùng có thể quan sát được trên console.
     */
    private void simulateProcessing(Random rand) {
        int sleepMs = 10 + rand.nextInt(91); // Thang đo ngẫu nhiên từ 10ms đến 100ms
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Đảm bảo trạng thái ngắt luồng được giữ nguyên nếu có lỗi xảy ra
        }
    }

    /**
     * KỊCH BẢN A: Hai hàng đợi hoàn toàn riêng biệt (Dual Queue).
     * VIP luôn được xử lý trước. Khách thường chỉ được xử lý khi hàng VIP trống hoàn toàn.
     * Dễ gây ra hiện tượng Starvation (đói thuật toán) cho khách hàng thường khi tổng đài quá tải.
     */
    private void runDualQueueSimulation(List<SimCall> dataset) {
        int[] agentFreeTime = new int[NUM_AGENTS]; // Lưu mốc thời gian ảo mà Agent i sẽ rảnh tay
        List<SimCall> vipQueue = new ArrayList<>();
        List<SimCall> regularQueue = new ArrayList<>();
        Random rand = new Random(42);

        int callIndex = 0;
        int totalCalls = dataset.size();
        int processedCount = 0;
        int t = 0; // Đồng hồ ảo (đơn vị tính: giây)

        while (processedCount < totalCalls) {
            // Thêm các cuộc gọi mới đến tại thời điểm t vào hàng đợi tương ứng dựa trên nhãn VIP
            while (callIndex < totalCalls && dataset.get(callIndex).arrivalTime <= t) {
                SimCall sc = dataset.get(callIndex);
                if (sc.call.isVIP()) vipQueue.add(sc);
                else regularQueue.add(sc);
                callIndex++;
            }

            // Quét qua các Agent để xem ai rảnh tay tại giây t để phân phối cuộc gọi
            for (int i = 0; i < NUM_AGENTS; i++) {
                if (agentFreeTime[i] <= t) {
                    SimCall nextCall = null;

                    // Nguyên tắc cốt lõi: Hàng đợi VIP luôn được vét sạch trước khi ngó tới hàng Regular
                    if (!vipQueue.isEmpty()) {
                        nextCall = vipQueue.remove(0);
                    } else if (!regularQueue.isEmpty()) {
                        nextCall = regularQueue.remove(0);
                    }

                    if (nextCall != null) {
                        nextCall.waitTime = t - nextCall.arrivalTime; // Tính toán thời gian chờ đợi trong hàng đợi ảo
                        agentFreeTime[i] = t + nextCall.handlingTime; // Cập nhật mốc thời gian rảnh mới của Agent này
                        processedCount++;

                        // XỬ LÝ THỰC TẾ: Tạo độ trễ hình ảnh trực quan cho luồng chạy
                        simulateProcessing(rand);
                        printProgress(processedCount, totalCalls, "Scenario A");
                    }
                }
            }
            t++; // Tăng đồng hồ ảo lên 1 giây sau mỗi vòng lặp toàn hệ thống
        }
    }

    /**
     * KỊCH BẢN B: Một hàng đợi duy nhất tích hợp cơ chế Aging.
     * Tất cả cuộc gọi xếp chung một hàng. Khách thường chờ lâu sẽ được tăng điểm ưu tiên 
     * theo thời gian, giúp họ không bị "starve" (bỏ rơi vô thời hạn) bởi các cuộc gọi VIP đến sau.
     */
    private void runSingleQueueAgingSimulation(List<SimCall> dataset) {
        int[] agentFreeTime = new int[NUM_AGENTS];
        List<SimCall> priorityQueue = new ArrayList<>();
        Random rand = new Random(42);

        int callIndex = 0;
        int totalCalls = dataset.size();
        int processedCount = 0;
        int t = 0; // Đồng hồ ảo (đơn vị tính: giây)

        while (processedCount < totalCalls) {
            // Thêm các cuộc gọi mới đến tại thời điểm t vào hàng đợi chung
            while (callIndex < totalCalls && dataset.get(callIndex).arrivalTime <= t) {
                priorityQueue.add(dataset.get(callIndex));
                callIndex++;
            }

            // Cơ chế Aging quan trọng: Cứ mỗi 60 giây ảo trôi qua, tăng điểm cho tất cả khách thường còn kẹt trong hàng đợi
            if (t > 0 && t % AGING_INTERVAL_SECONDS == 0) {
                for (SimCall sc : priorityQueue) {
                    if (!sc.call.isVIP()) {
                        // Tận dụng thuộc tính waitTime trong call để lưu trữ điểm thưởng tích lũy từ cơ chế Aging
                        sc.call.setWaitTime(sc.call.getWaitTime() + AGING_BOOST_POINTS);
                    }
                }
            }

            // Phân phối cuộc gọi cho Agent rảnh
            for (int i = 0; i < NUM_AGENTS; i++) {
                if (agentFreeTime[i] <= t) {
                    if (!priorityQueue.isEmpty()) {
                        // Tiến hành giải thuật tìm kiếm cuộc gọi có tổng điểm ưu tiên cao nhất
                        int highestPriorityIndex = 0;
                        for (int j = 1; j < priorityQueue.size(); j++) {
                            // Tổng điểm = Điểm cấu hình ban đầu + Điểm thưởng tích lũy (Aging Boost)
                            int p1 = priorityQueue.get(j).call.getPriorityScore() + priorityQueue.get(j).call.getWaitTime();
                            int p2 = priorityQueue.get(highestPriorityIndex).call.getPriorityScore() + priorityQueue.get(highestPriorityIndex).call.getWaitTime();

                            if (p1 > p2) {
                                highestPriorityIndex = j;
                            } else if (p1 == p2) {
                                // Quy tắc bổ trợ: Nếu bằng tổng điểm, áp dụng cơ chế FIFO (ai đến trước phục vụ trước) để đảm bảo tính công bằng
                                if (priorityQueue.get(j).arrivalTime < priorityQueue.get(highestPriorityIndex).arrivalTime) {
                                    highestPriorityIndex = j;
                                }
                            }
                        }

                        SimCall nextCall = priorityQueue.remove(highestPriorityIndex);
                        nextCall.waitTime = t - nextCall.arrivalTime; // Tính toán thời gian chờ ảo thực tế
                        agentFreeTime[i] = t + nextCall.handlingTime;
                        processedCount++;

                        // XỬ LÝ THỰC TẾ: Luồng chạy tạm dừng mô phỏng thực tế
                        simulateProcessing(rand);
                        printProgress(processedCount, totalCalls, "Scenario B");
                    }
                }
            }
            t++; // Tăng đồng hồ ảo lên 1 giây
        }
    }

    /**
     * Định dạng số giây thành chuỗi dễ đọc (ví dụ: "5 min 23.0 sec").
     */
    private String formatDuration(double seconds) {
        if (seconds < 0) return "N/A";
        int m = (int) (seconds / 60);
        double s = seconds - (m * 60);
        if (m > 0) {
            return String.format("%d min %04.1f sec", m, s);
        } else {
            return String.format("%.1f sec", s);
        }
    }

    /**
     * BÁO CÁO KẾT QUẢ SO SÁNH:
     * Tổng hợp số liệu thu thập từ quá trình chạy mô phỏng thực tế.
     * Thời gian chờ lấy từ đồng hồ ảo (đơn vị: giây thời gian thật).
     */
    private void printComparativeReport(List<SimCall> datasetA, List<SimCall> datasetB) {
        // --- Tính toán thống kê dữ liệu cho Kịch bản A ---
        double vipAwtA = 0, regAwtA = 0, totalAwtA = 0;
        int maxRegA = 0, maxVipA = 0;
        int vipCount = 0, regCount = 0;

        for (SimCall sc : datasetA) {
            if (sc.call.isVIP()) {
                vipAwtA += sc.waitTime;
                vipCount++;
                if (sc.waitTime > maxVipA) maxVipA = sc.waitTime;
            } else {
                regAwtA += sc.waitTime;
                regCount++;
                if (sc.waitTime > maxRegA) maxRegA = sc.waitTime;
            }
            totalAwtA += sc.waitTime;
        }
        vipAwtA /= vipCount;
        regAwtA /= regCount;
        totalAwtA /= datasetA.size();

        // --- Tính toán thống kê dữ liệu cho Kịch bản B ---
        double vipAwtB = 0, regAwtB = 0, totalAwtB = 0;
        int maxRegB = 0, maxVipB = 0;
        for (SimCall sc : datasetB) {
            if (sc.call.isVIP()) {
                vipAwtB += sc.waitTime;
                if (sc.waitTime > maxVipB) maxVipB = sc.waitTime;
            } else {
                regAwtB += sc.waitTime;
                if (sc.waitTime > maxRegB) maxRegB = sc.waitTime;
            }
            totalAwtB += sc.waitTime;
        }
        vipAwtB /= vipCount;
        regAwtB /= regCount;
        totalAwtB /= datasetB.size();

        // Tính tỷ lệ phần trăm cải thiện hiệu năng hệ thống giữa 2 kịch bản
        double regAwtImprovement = (regAwtA > 0) ? ((regAwtA - regAwtB) / regAwtA) * 100.0 : 0;
        double regMaxImprovement = (maxRegA > 0) ? ((maxRegA - maxRegB) / (double) maxRegA) * 100.0 : 0;

        // In bảng so sánh trực quan
        System.out.println("\nSIMULATION METRICS REPORT");
        System.out.println("  Setup: 500 calls/hour | 20% VIP | 5 Agents | Speed: x10");
        System.out.println("  ---------------------------------------------------------------------------------------------------------");
        System.out.printf("  %-45s │ %-28s │ %-28s%n",
                "Metric Description",
                "Scenario A (Dual Queue)",
                "Scenario B (Aging Queue)");
        System.out.println("  ---------------------------------------------------------------------------------------------------------");

        System.out.println("  [1] Average Wait Time (AWT):");
        System.out.printf("   - Regular Customer                           │ %-28s │ %-28s%n",
                formatDuration(regAwtA) + " (" + String.format("%.1f", regAwtA) + "s)",
                formatDuration(regAwtB) + " (" + String.format("%.1f", regAwtB) + "s)");
        System.out.printf("   - VIP Customer                               │ %-28s │ %-28s%n",
                formatDuration(vipAwtA) + " (" + String.format("%.1f", vipAwtA) + "s)",
                formatDuration(vipAwtB) + " (" + String.format("%.1f", vipAwtB) + "s)");
        System.out.printf("   - Overall System                             │ %-28s │ %-28s%n",
                formatDuration(totalAwtA) + " (" + String.format("%.1f", totalAwtA) + "s)",
                formatDuration(totalAwtB) + " (" + String.format("%.1f", totalAwtB) + "s)");

        System.out.println("  [2] Maximum Wait Time (Max WT):");
        System.out.printf("   - Regular Customer                           │ %-28s │ %-28s%n",
                formatDuration(maxRegA) + " (" + maxRegA + "s)",
                formatDuration(maxRegB) + " (" + maxRegB + "s)");
        System.out.printf("   - VIP Customer                               │ %-28s │ %-28s%n",
                formatDuration(maxVipA) + " (" + maxVipA + "s)",
                formatDuration(maxVipB) + " (" + maxVipB + "s)");

        System.out.println("  ---------------------------------------------------------------------------------------------------------");
        System.out.printf("  Regular Customer Variance: Average Delta: %.2f%% | Maximum Delta: %.2f%%%n",
                regAwtImprovement, regMaxImprovement);
        System.out.println("  ---------------------------------------------------------------------------------------------------------");

        // Ghi dữ liệu kết quả ra file định dạng CSV để phục vụ phân tích/vẽ biểu đồ sau này
        try {
            config.ConfigLoader loader = new config.ConfigLoader();
            String csvPath = loader.resolvePath("data/Exp1_PriorityQueue.csv");
            storage.FileHandler fh = new storage.FileHandler(csvPath);
            List<String> csvLines = new ArrayList<>();
            csvLines.add("Metric Description,Scenario A (Dual Queue) (seconds),Scenario B (Aging Queue) (seconds),Improvement (%)");
            csvLines.add(String.format(Locale.US, "Average Wait Time - Regular Customer,%.2f,%.2f,%.2f%%", regAwtA, regAwtB, regAwtImprovement));
            csvLines.add(String.format(Locale.US, "Average Wait Time - VIP Customer,%.2f,%.2f,N/A", vipAwtA, vipAwtB));
            csvLines.add(String.format(Locale.US, "Average Wait Time - Overall System,%.2f,%.2f,%.2f%%", totalAwtA, totalAwtB, (totalAwtA > 0 ? ((totalAwtA - totalAwtB)/totalAwtA)*100.0 : 0)));
            csvLines.add(String.format(Locale.US, "Max Wait Time - Regular Customer,%d,%d,%.2f%%", maxRegA, maxRegB, regMaxImprovement));
            csvLines.add(String.format(Locale.US, "Max Wait Time - VIP Customer,%d,%d,N/A", maxVipA, maxVipB));
            fh.writeLines(csvLines);
            System.out.println("  Data saved to: " + csvPath);
        } catch (Exception e) {
            System.err.println("  Error writing CSV: " + e.getMessage());
        }

        // Kết luận thực nghiệm tổng quan bài toán toán học / giải thuật dưới dạng kê khai thông số tĩnh
        System.out.println("\nMETRICS SUMMARY:");
        System.out.println("  Scenario A (Dual Queue):");
        System.out.println("     - Regular Customer AWT: " + formatDuration(regAwtA) + " | Max WT: " + formatDuration(maxRegA));
        System.out.println("     - VIP Customer AWT: " + formatDuration(vipAwtA) + " | Max WT: " + formatDuration(maxVipA));
        System.out.println("  Scenario B (Single Queue + Aging):");
        System.out.println("     - Regular Customer AWT: " + formatDuration(regAwtB) + " | Max WT: " + formatDuration(maxRegB));
        System.out.println("     - VIP Customer AWT: " + formatDuration(vipAwtB) + " | Max WT: " + formatDuration(maxVipB));
    }
}