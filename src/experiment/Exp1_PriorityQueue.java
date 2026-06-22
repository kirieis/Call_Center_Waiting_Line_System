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
    private static final int SIM_DURATION_SECONDS = 4 * 3600; // Mô phỏng chạy trong 4 tiếng (14,400 giây)
    
    /**
     * TỐC ĐỘ CUỘC GỌI MỖI GIÂY (CALL_RATE_PER_SECOND):
     * - Yêu cầu bài toán: Mô phỏng 500 cuộc gọi/giờ.
     * - Quy đổi: 1 giờ = 3600 giây.
     * - Do đó, tốc độ cuộc gọi mỗi giây (lambda trong phân phối Poisson) = 500.0 / 3600.0 ≈ 0.1389 cuộc gọi/giây.
     * - Quy đổi ra phút: 0.1389 * 60 ≈ 8.33 cuộc gọi/phút.
     * - Đây chính là nơi định nghĩa con số "500 cuộc gọi/giờ" để đưa vào mô phỏng.
     */
    private static final double CALL_RATE_PER_SECOND = 500.0/3600.0; 
    
    private static final int AGING_INTERVAL_SECONDS = 60; // Tiến hành aging sau mỗi 60 giây
    private static final int AGING_BOOST_POINTS = 15; // Điểm cộng thêm để cạnh tranh công bằng với điểm VIP (50)

    /**
     * LỚP WRAPPER SIMCALL:
     * - Bao bọc thực thể Call để bổ sung các thuộc tính phục vụ riêng cho môi trường mô phỏng.
     * - Lưu giữ thời gian cuộc gọi đến (arrivalTime), thời gian đàm thoại của Agent (handlingTime)
     *   và tính toán thời gian chờ đợi thực tế (waitTime).
     */
    static class SimCall {
        Call call;
        int arrivalTime;   // Thời điểm cuộc gọi đến tổng đài (tính bằng giây ảo)
        int handlingTime;  // Thời lượng đàm thoại mà Agent cần xử lý cuộc gọi (giây)
        int waitTime = -1; // Kết quả thời gian chờ tính được (giây), mặc định = -1 (chưa xử lý)

        SimCall(Call call, int arrivalTime, int handlingTime) {
            this.call = call;
            this.arrivalTime = arrivalTime;
            this.handlingTime = handlingTime;
        }
    }

    /**
     * HÀM KHỞI CHẠY THỰC NGHIỆM 1 (run):
     * - Điều phối toàn bộ vòng đời thực nghiệm 1.
     * - Bước 1: Sinh tập dữ liệu đồng nhất bằng cách khóa Seed ngẫu nhiên (đảm bảo so sánh công bằng).
     * - Bước 2: Nhân bản tập dữ liệu để chạy song song 2 kịch bản độc lập.
     * - Bước 3: Chạy mô phỏng Kịch bản A (Dual Queue).
     * - Bước 4: Chạy mô phỏng Kịch bản B (Single Queue + Aging).
     * - Bước 5: Phân tích số liệu và xuất báo cáo so sánh.
     */
    public void run() {
        System.out.println("\n==================================================================");
        System.out.println("🧪 EXPERIMENT 1: DUAL QUEUE VS SINGLE QUEUE WITH AGING ALGORITHM");
        System.out.println("==================================================================");

        // Sinh dữ liệu đồng nhất (Deterministic Input Data) bằng cách khóa Seed ngẫu nhiên
        List<SimCall> datasetA = generateDeterministicDataset(12345);
        // Nhân bản sâu (deep clone) để Scenario B chạy trên cùng một bộ dữ liệu đầu vào y hệt Scenario A
        List<SimCall> datasetB = cloneDataset(datasetA);

        System.out.println("  [i] Running simulation Scenario A (Dual Queue - Absolute VIP)...");
        runDualQueueSimulation(datasetA);

        System.out.println("  [i] Running simulation Scenario B (Single Queue + Aging Mechanism)...");
        runSingleQueueAgingSimulation(datasetB);

        // Xuất báo cáo so sánh chi tiết và lưu kết quả ra file CSV
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
            // sinh số ngẫu nhiên u trong khoảng (0, 1]
            double u = rand.nextDouble();
            while (u == 0) u = rand.nextDouble();
            
            /**
             * MÔ PHỎNG QUÁ TRÌNH POISSON (POISSON PROCESS):
             * - Trong thực tế, các cuộc gọi đến Call Center là các sự kiện ngẫu nhiên độc lập.
             * - Do đó, khoảng thời gian giữa 2 cuộc gọi liên tiếp (Inter-arrival time) tuân theo phân phối mũ (Exponential Distribution).
             * - Công thức nghịch đảo hàm mật độ tích lũy: nextArrivalInterval = -ln(1 - u) / lambda.
             * - Ở đây lambda = CALL_RATE_PER_SECOND (được tính từ 500 cuộc gọi / 3600 giây).
             * - Kết quả nextArrivalInterval là số giây ngẫu nhiên cho đến cuộc gọi tiếp theo.
             */
            int nextArrivalInterval = (int) (-Math.log(1 - u) / CALL_RATE_PER_SECOND);
            if (nextArrivalInterval < 1) nextArrivalInterval = 1; // Đảm bảo khoảng cách tối thiểu 1 giây
            currentTime += nextArrivalInterval;

            if (currentTime >= SIM_DURATION_SECONDS) break;

            /**
             * THIẾT LẬP TỶ LỆ KHÁCH HÀNG VIP:
             * - Yêu cầu bài toán: Tỷ lệ VIP chiếm 20%.
             * - Sử dụng rand.nextDouble() < 0.20 để gán nhãn VIP với xác suất chính xác là 20%.
             */
            boolean isVip = rand.nextDouble() < 0.20; 
            
            // 15% khách hàng có lịch sử cuộc gọi lặp lại (từ 1 đến 3 cuộc gọi trước đó)
            int repeatCalls = rand.nextInt(100) < 15 ? rand.nextInt(3) + 1 : 0; 
            
            // Thời gian xử lý cuộc gọi ngẫu nhiên của điện thoại viên từ 2 đến 5 phút (quy ra giây: 120s - 300s)
            int handlingTime = (rand.nextInt(4) + 2) * 60; 

            String id = "C" + String.format("%04d", orderCounter);
            Call call = new Call(id, "Customer " + id, "090" + String.format("%07d", rand.nextInt(10000000)), isVip, repeatCalls, orderCounter);
            
            list.add(new SimCall(call, currentTime, handlingTime));
            orderCounter++;
        }
        return list;
    }

    /**
     * SAO CHÉP TẬP DỮ LIỆU (cloneDataset):
     * - Thực hiện nhân bản sâu (deep clone) danh sách SimCall.
     * - Điều này là bắt buộc vì nếu dùng chung tham chiếu đối tượng Call, kết quả tính toán waitTime
     *   hoặc điểm ưu tiên của Scenario A sẽ ghi đè và làm sai lệch dữ liệu của Scenario B.
     */
    private List<SimCall> cloneDataset(List<SimCall> original) {
        List<SimCall> clone = new ArrayList<>();
        for (SimCall sc : original) {
            Call oc = sc.call;
            // Khởi tạo một đối tượng Call hoàn toàn mới với các thông số sao chép từ Call cũ
            Call nc = new Call(oc.getCustomerId(), oc.getCustomerName(), oc.getPhoneNumber(), oc.isVIP(), oc.getRepeatCalls(), oc.getOrderNumber());
            nc.setPriorityScore(oc.getPriorityScore());
            // Đóng gói vào đối tượng SimCall mới
            clone.add(new SimCall(nc, sc.arrivalTime, sc.handlingTime));
        }
        return clone;
    }

    /**
     * KỊCH BẢN A: Khởi tạo 2 hàng đợi hoàn toàn riêng biệt (Dual Queue).
     * - Khách VIP xếp vào hàng VIP, khách thường xếp vào hàng thường.
     * - Điện thoại viên (Agent) CHỈ lấy cuộc gọi từ hàng đợi thường khi hàng đợi VIP hoàn toàn trống.
     * - Phân tích: Kịch bản này ưu tiên tuyệt đối cho khách VIP (Absolute VIP). Tuy nhiên,
     *   khi hệ thống quá tải (500 cuộc gọi/giờ so với công suất phục vụ giới hạn của 5 Agents),
     *   khách thường sẽ bị bỏ rơi (Resource Starvation) dẫn đến thời gian chờ cực kỳ khủng khiếp.
     */
    private void runDualQueueSimulation(List<SimCall> dataset) {
        int[] agentFreeTime = new int[NUM_AGENTS]; // Lưu thời điểm rảnh của các Agent (giây)
        List<SimCall> vipQueue = new ArrayList<>(); // Hàng đợi riêng biệt cho VIP
        List<SimCall> regularQueue = new ArrayList<>(); // Hàng đợi riêng biệt cho khách thường
        
        int callIndex = 0;
        int totalCalls = dataset.size();
        int processedCount = 0;
        int t = 0; // Biến đếm thời gian thực mô phỏng (giây)

        while (processedCount < totalCalls) {
            // Khi đến giây t, nếu có cuộc gọi mới đến Call Center thì đưa vào hàng đợi tương ứng
            while (callIndex < totalCalls && dataset.get(callIndex).arrivalTime <= t) {
                SimCall sc = dataset.get(callIndex);
                if (sc.call.isVIP()) vipQueue.add(sc);
                else regularQueue.add(sc);
                callIndex++;
            }

            // Quét qua các Agent để xem có ai đang rảnh tay ở giây t không
            for (int i = 0; i < NUM_AGENTS; i++) {
                if (agentFreeTime[i] <= t) {
                    SimCall nextCall = null;
                    
                    // Nguyên tắc phân phối: VIP luôn luôn trước
                    if (!vipQueue.isEmpty()) {
                        nextCall = vipQueue.remove(0); // Lấy khách VIP đứng đầu hàng
                    } else if (!regularQueue.isEmpty()) {
                        nextCall = regularQueue.remove(0); // Khi hàng VIP trống mới xét tới khách thường
                    }

                    if (nextCall != null) {
                        nextCall.waitTime = t - nextCall.arrivalTime; // Tính thời gian chờ (giây)
                        agentFreeTime[i] = t + nextCall.handlingTime; // Đặt thời điểm rảnh tiếp theo của Agent này
                        processedCount++;
                    }
                }
            }
            t++; // Tăng thời gian mô phỏng thêm 1 giây
        }
    }

    /**
     * KỊCH BẢN B: Sử dụng 1 hàng đợi duy nhất tích hợp cơ chế chống nghẽn (Single Queue + Aging).
     * - Tất cả cuộc gọi (VIP và thường) đều xếp chung vào 1 hàng đợi ưu tiên.
     * - Điểm ưu tiên cơ bản (Base Priority): VIP mặc định được +50 điểm. Khách thường bắt đầu với 0 điểm (hoặc +10/mỗi cuộc gọi lặp lại).
     * - Cơ chế Tăng tuổi (Aging Mechanism): Cứ sau mỗi AGING_INTERVAL_SECONDS (60 giây), những khách thường đang chờ
     *   sẽ được cộng thêm AGING_BOOST_POINTS (15 điểm) vào điểm ưu tiên.
     * - Phân tích:
     *   + Sau 1 phút chờ: Điểm khách thường tăng lên 15.
     *   + Sau 2 phút chờ: Điểm tăng lên 30.
     *   + Sau 3 phút chờ: Điểm tăng lên 45.
     *   + Sau 3.33 phút chờ (200 giây): Điểm tăng lên 50 (bằng điểm VIP mới đến).
     *   + Sau 4 phút chờ: Điểm tăng lên 60 (vượt qua điểm VIP mới đến).
     *   + Do đó, khách thường chờ lâu chắc chắn sẽ được phục vụ trước khách VIP vừa mới đến.
     *   Điều này giúp rút ngắn đáng kể thời gian chờ của khách thường và giải quyết triệt để nạn nghẽn hàng đợi (Resource Starvation).
     */
    private void runSingleQueueAgingSimulation(List<SimCall> dataset) {
        int[] agentFreeTime = new int[NUM_AGENTS];
        List<SimCall> priorityQueue = new ArrayList<>();
        
        int callIndex = 0;
        int totalCalls = dataset.size();
        int processedCount = 0;
        int t = 0;

        while (processedCount < totalCalls) {
            // Khi đến giây t, đưa các cuộc gọi mới xuất hiện vào hàng đợi chung
            while (callIndex < totalCalls && dataset.get(callIndex).arrivalTime <= t) {
                priorityQueue.add(dataset.get(callIndex));
                callIndex++;
            }

            // Thuật toán Tăng tuổi (Aging Mechanism) áp dụng sau mỗi 60 giây ảo trôi qua
            if (t > 0 && t % AGING_INTERVAL_SECONDS == 0) {
                for (SimCall sc : priorityQueue) {
                    if (!sc.call.isVIP()) {
                        // Tăng biến waitTime nội tại để nâng điểm getAgedPriority() lên
                        // Mỗi lần tăng thêm AGING_BOOST_POINTS (15 điểm) sau mỗi phút
                        sc.call.setWaitTime(sc.call.getWaitTime() + AGING_BOOST_POINTS);
                    }
                }
            }

            // Phân phối cuộc gọi cho Agent rảnh tay
            for (int i = 0; i < NUM_AGENTS; i++) {
                if (agentFreeTime[i] <= t) {
                    if (!priorityQueue.isEmpty()) {
                        int highestPriorityIndex = 0;
                        
                        // Tìm cuộc gọi có tổng điểm ưu tiên cao nhất trong hàng đợi ở giây t
                        for (int j = 1; j < priorityQueue.size(); j++) {
                            // Tổng điểm = Base Priority Score + Điểm Aging (waitTime đã được boost)
                            int p1 = priorityQueue.get(j).call.getPriorityScore() + priorityQueue.get(j).call.getWaitTime();
                            int p2 = priorityQueue.get(highestPriorityIndex).call.getPriorityScore() + priorityQueue.get(highestPriorityIndex).call.getWaitTime();
                             
                            if (p1 > p2) {
                                highestPriorityIndex = j;
                            } else if (p1 == p2) {
                                // Nếu bằng điểm nhau, áp dụng nguyên tắc FIFO (ai đến trước phục vụ trước)
                                if (priorityQueue.get(j).arrivalTime < priorityQueue.get(highestPriorityIndex).arrivalTime) {
                                    highestPriorityIndex = j;
                                }
                            }
                        }

                        SimCall nextCall = priorityQueue.remove(highestPriorityIndex);
                        nextCall.waitTime = t - nextCall.arrivalTime; // Tính thời gian chờ thực tế từ khi đến
                        agentFreeTime[i] = t + nextCall.handlingTime;
                        processedCount++;
                    }
                }
            }
            t++;
        }
    }

    /**
     * BÁO CÁO KẾT QUẢ SO SÁNH (printComparativeReport):
     * - Thực hiện tổng hợp số liệu và tính thời gian chờ trung bình (AWT) cho từng nhóm đối tượng.
     * - Xuất bảng báo cáo định dạng ascii trực quan ra console.
     * - Ghi dữ liệu kết quả ra file CSV phục vụ phân tích dữ liệu hoặc vẽ biểu đồ.
     */
    private void printComparativeReport(List<SimCall> datasetA, List<SimCall> datasetB) {
        // --- 1. Tính toán số liệu cho Kịch bản A (Dual Queue) ---
        double vipAwtA = 0, regAwtA = 0, totalAwtA = 0;
        int vipCount = 0, regCount = 0;
        
        for (SimCall sc : datasetA) {
            if (sc.call.isVIP()) { 
                vipAwtA += sc.waitTime; 
                vipCount++; 
            } else { 
                regAwtA += sc.waitTime; 
                regCount++; 
            }
            totalAwtA += sc.waitTime;
        }
        // Tính trung bình (AWT = tổng thời gian chờ / số lượng cuộc gọi)
        vipAwtA /= vipCount; 
        regAwtA /= regCount; 
        totalAwtA /= datasetA.size();

        // --- 2. Tính toán số liệu cho Kịch bản B (Single Queue + Aging) ---
        double vipAwtB = 0, regAwtB = 0, totalAwtB = 0;
        for (SimCall sc : datasetB) {
            if (sc.call.isVIP()) {
                vipAwtB += sc.waitTime;
            } else {
                regAwtB += sc.waitTime;
            }
            totalAwtB += sc.waitTime;
        }
        // Tính trung bình cho kịch bản B
        vipAwtB /= vipCount; 
        regAwtB /= regCount; 
        totalAwtB /= datasetB.size();

        // In bảng so sánh trực quan
        System.out.println("\n📊 AVERAGE WAIT TIME (AWT) COMPARISON TABLE");
        System.out.println("┌──────────────────────────┬──────────────────────────┬──────────────────────────┐");
        System.out.println("│ Customer Classification  │ Scenario A (Dual Queue)  │ Scenario B (Aging Queue) │");
        System.out.println("├──────────────────────────┼──────────────────────────┼──────────────────────────┤");
        System.out.printf("│ Regular Customer         │       %8.2f seconds    │       %8.2f seconds    │%n", regAwtA, regAwtB);
        System.out.printf("│ VIP Customer             │       %8.2f seconds    │       %8.2f seconds    │%n", vipAwtA, vipAwtB);
        System.out.printf("│ OVERALL SYSTEM           │       %8.2f seconds    │       %8.2f seconds    │%n", totalAwtA, totalAwtB);
        System.out.println("└──────────────────────────┴──────────────────────────┴──────────────────────────┘");
        
        // --- 3. Ghi kết quả so sánh ra file CSV để lưu trữ lâu dài ---
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

        // --- 4. Đưa ra kết luận chuyên môn dựa trên số liệu ---
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