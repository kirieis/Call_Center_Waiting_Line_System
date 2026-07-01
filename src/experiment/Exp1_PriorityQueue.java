package experiment;

import model.Call;
import model.CallStatus;
import java.util.*;

/**
 * THỰC NGHIỆM 1: SO SÁNH HÀNG ĐỢI KÉP TÁCH BIỆT (DUAL QUEUE) 
 * VỚI HÀNG ĐỢI ĐƠN TÍCH HỢP CƠ CHẾ CHỐNG NGHẼN/LÃO HÓA (SINGLE QUEUE + AGING).
 * 
 * ==================================================================================
 * 1. MỤC TIÊU THỰC NGHIỆM:
 * - So sánh hiệu quả của hai cấu trúc hàng đợi trong việc tối ưu hóa thời gian chờ đợi (AWT)
 *   và hạn chế tình trạng "starvation" (đói thuật toán - khách hàng thường bị bỏ rơi vô thời hạn)
 *   khi hệ thống call center rơi vào trạng thái quá tải nghiêm trọng.
 * 
 * 2. CƠ CHẾ MÔ PHỎNG CHI TIẾT:
 * - Sử dụng đồng hồ ảo (virtual clock) chạy bằng biến đếm 't' (đơn vị: giây) để đồng bộ hóa 
 *   thời điểm cuộc gọi đến (arrival time) và thời gian đàm thoại (handling time).
 * - Sử dụng Phân phối Poisson để giả lập cuộc gọi đến ngẫu nhiên nhưng thực tế (500 cuộc gọi/giờ).
 * - Thời gian xử lý cuộc gọi (Handling time) ngẫu nhiên từ 2 đến 5 phút (120 - 300 giây ảo).
 * - Tốc độ mô phỏng được nén theo tỷ lệ thời gian cố định 1:20 so với thời gian thực
 *   (1 giờ ảo (3600 giây) ≈ 6 phút thật). Thời gian Thread.sleep cho mỗi cuộc gọi được TÍNH TOÁN
 *   dựa trên tổng số cuộc gọi thực tế sinh ra (không còn là số random tùy tiện), đảm bảo tổng thời
 *   gian chạy thật của mỗi kịch bản luôn khớp đúng tỷ lệ 1:20 dù dataset có bao nhiêu cuộc gọi.
 * 
 * @author Group 7
 */
public class Exp1_PriorityQueue {

    // Số lượng điện thoại viên (Agent) xử lý cuộc gọi trong hệ thống (được nâng lên thành 10 Agents)
    private static final int NUM_AGENTS = 10;
    
    // Thời gian giả lập hoạt động của tổng đài là 1 giờ (quy đổi ra 3600 giây ảo trên đồng hồ hệ thống)
    private static final int SIM_DURATION_SECONDS = 1 * 3600; 
    
    // Tần suất cuộc gọi trung bình mỗi giây ảo theo cấu hình (500 cuộc/giờ => ~0.1389 cuộc/giây)
    private static final double CALL_RATE_PER_SECOND = 500.0 / 3600.0; 
    
    // Chu kỳ quét hàng đợi để cộng điểm ưu tiên (Aging) cho khách hàng thường (mỗi 60 giây ảo)
    private static final int AGING_INTERVAL_SECONDS = 60; 
    
    // Số điểm ưu tiên cộng thêm cho mỗi lần quét Aging (15 điểm/phút chờ đợi)
    private static final int AGING_BOOST_POINTS = 15; 

    // Tỷ lệ nén thời gian mô phỏng so với thời gian thực: 1:20
    // Nghĩa là 1 giờ ảo (3600 giây) sẽ chạy thật trong khoảng 3600 / 20 = 180 giây = 3 phút
    private static final int TIME_COMPRESSION_RATIO = 20;

    // Tổng thời gian thật (mili giây) mà MỖI kịch bản (A hoặc B) cần chạy hết để giữ đúng tỷ lệ 1:20
    private static final long TARGET_REAL_DURATION_MS = (SIM_DURATION_SECONDS * 1000L) / TIME_COMPRESSION_RATIO;

    /**
     * Lớp SimCall (Simulation Call) đóng vai trò là một Wrapper (lớp bao bọc) quanh model Call gốc.
     * Nó bổ sung các trường thông tin phục vụ riêng cho quá trình chạy mô phỏng:
     * - arrivalTime: Thời điểm cuộc gọi bắt đầu xuất hiện trong hệ thống (giây ảo).
     * - handlingTime: Thời lượng cuộc gọi này chiếm dụng Agent (giây ảo).
     * - waitTime: Thời gian chờ đợi thực tế trong hàng đợi (giây ảo), tính từ lúc đến tới lúc được Agent bắt máy.
     */
    static class SimCall {
        Call call;          // Đối tượng Call chứa thông tin nghiệp vụ chính (ID, Tên, VIP/Thường, số lần gọi lại...)
        int arrivalTime;    // Thời điểm cuộc gọi đến (giây ảo trên timeline)
        int handlingTime;   // Thời gian xử lý đàm thoại (giây ảo)
        int waitTime = -1;  // Thời gian chờ ảo (giây ảo) = Thời điểm phục vụ - arrivalTime

        SimCall(Call call, int arrivalTime, int handlingTime) {
            this.call = call;
            this.arrivalTime = arrivalTime;
            this.handlingTime = handlingTime;
        }
    }

    /**
     * Phương thức khởi chạy thực nghiệm chính (Entrypoint của Exp1).
     */
    public void run() {
        System.out.println("\n==================================================================");
        System.out.println("EXPERIMENT 1: DUAL QUEUE VS SINGLE QUEUE WITH AGING");
        System.out.println("==================================================================");
        System.out.println("  Configuration: 500 calls/hour | 20% VIP | 10 Agents | Duration: 1 hour");
        System.out.println("  Simulation Speed: Compressed 1:" + TIME_COMPRESSION_RATIO 
                + " (1 hour virtual ≈ " + (TARGET_REAL_DURATION_MS / 1000 / 60) + " minutes real, per scenario)");

        // BƯỚC 1: Sinh ngẫu nhiên tập dữ liệu cuộc gọi (Dataset) theo phân phối Poisson.
        // Việc sinh dữ liệu MỘT LẦN duy nhất đảm bảo cả hai kịch bản đều chạy trên cùng một tập cuộc gọi giống hệt nhau,
        // giúp kết quả đối sánh đạt độ công bằng và chính xác cao nhất.
        List<SimCall> datasetA = generateDataset();
        
        // BƯỚC 2: Tạo bản sao sâu (deep clone) của tập dữ liệu dành cho kịch bản B.
        // Điều này rất quan trọng vì trong quá trình mô phỏng, thuộc tính của các đối tượng Call (như điểm ưu tiên, 
        // thời gian chờ) sẽ bị sửa đổi. Deep clone giúp Kịch bản A và Kịch bản B hoàn toàn cô lập, không ảnh hưởng lẫn nhau.
        List<SimCall> datasetB = cloneDataset(datasetA); 
        System.out.println("  Total Generated Calls: " + datasetA.size() + " (shared by both scenarios)");
        System.out.println();

        // BƯỚC 3: Chạy mô phỏng Kịch bản A (Dual Queue - Hàng đợi kép ưu tiên tuyệt đối VIP)
        System.out.println("  Running Scenario A (Dual Queue)...");
        long startA = System.currentTimeMillis();
        runDualQueueSimulation(datasetA);
        long durationA = System.currentTimeMillis() - startA;
        System.out.println("  Scenario A Execution Time: " + String.format("%.1f", durationA / 1000.0) + " s");
        System.out.println();

        // BƯỚC 4: Chạy mô phỏng Kịch bản B (Single Queue + Aging - Một hàng đợi tích hợp cơ chế tăng điểm theo thời gian)
        System.out.println("  Running Scenario B (Single Queue + Aging)...");
        long startB = System.currentTimeMillis();
        runSingleQueueAgingSimulation(datasetB);
        long durationB = System.currentTimeMillis() - startB;
        System.out.println("  Scenario B Execution Time: " + String.format("%.1f", durationB / 1000.0) + " s");
        System.out.println();

        // BƯỚC 5: Tổng hợp số liệu và in báo cáo so sánh trực quan, đồng thời xuất kết quả ra file CSV.
        printComparativeReport(datasetA, datasetB);
    }

    /**
     * Sinh tập dữ liệu các cuộc gọi ngẫu nhiên dựa trên thuật toán Poisson Process.
     * - Arrival rate (tốc độ đến): 500 cuộc gọi/giờ (xấp xỉ 0.1389 cuộc gọi/giây).
     * - Handling time (thời lượng xử lý): Ngẫu nhiên từ 2 đến 5 phút (120 đến 300 giây ảo).
     * - VIP Ratio (tỷ lệ VIP): Cố định khoảng 20%.
     */
    private List<SimCall> generateDataset() {
        Random rand = new Random();
        List<SimCall> list = new ArrayList<>();
        int currentTime = 0; // Đồng hồ ảo dùng để ghi nhận thời điểm cuộc gọi đến
        int orderCounter = 1; // Biến đếm số thứ tự cuộc gọi sinh ra

        // Sinh liên tục các cuộc gọi cho đến khi thời điểm đến vượt quá thời gian mô phỏng (1 giờ = 3600 giây)
        while (currentTime < SIM_DURATION_SECONDS) {
            double u = rand.nextDouble();
            while (u == 0) u = rand.nextDouble(); // Loại trừ trường hợp u = 0 để tránh lỗi toán học Math.log(0)

            // Áp dụng công thức tính khoảng thời gian giữa 2 sự kiện đến liên tiếp (Inter-arrival time)
            // của quá trình Poisson: dt = -ln(1 - u) / lambda
            int nextArrivalInterval = (int) (-Math.log(1 - u) / CALL_RATE_PER_SECOND);
            
            // Đảm bảo đồng hồ ảo luôn tiến lên ít nhất 1 giây để tránh vòng lặp vô hạn
            if (nextArrivalInterval < 1) nextArrivalInterval = 1; 
            currentTime += nextArrivalInterval;

            // Nếu thời điểm cuộc gọi đến vượt quá thời lượng mô phỏng 1 giờ thì dừng sinh cuộc gọi
            if (currentTime >= SIM_DURATION_SECONDS) break;

            // Thiết lập các thuộc tính ngẫu nhiên cho cuộc gọi:
            boolean isVip = rand.nextDouble() < 0.20; // 20% cuộc gọi được chỉ định là VIP
            int repeatCalls = rand.nextInt(100) < 15 ? rand.nextInt(3) + 1 : 0; // 15% tỷ lệ gọi lại (từ 1 đến 3 lần)
            int handlingTime = (rand.nextInt(4) + 2) * 60; // Thời lượng đàm thoại ngẫu nhiên từ 2-5 phút (120-300 giây)

            // Khởi tạo đối tượng Call gốc
            String id = "C" + String.format("%04d", orderCounter);
            Call call = new Call(id, "Customer " + id, "090" + String.format("%07d", rand.nextInt(10000000)), isVip, repeatCalls, orderCounter);

            // Bọc vào SimCall và thêm vào danh sách kết quả
            list.add(new SimCall(call, currentTime, handlingTime));
            orderCounter++;
        }
        return list;
    }

    /**
     * Thực hiện sao chép sâu (Deep Clone) danh sách cuộc gọi.
     * Việc clone từng đối tượng Call và SimCall là bắt buộc để đảm bảo các thay đổi về trạng thái, 
     * thời gian chờ, điểm số ưu tiên ở Scenario A hoàn toàn độc lập với Scenario B.
     */
    private List<SimCall> cloneDataset(List<SimCall> original) {
        List<SimCall> clone = new ArrayList<>();
        for (SimCall sc : original) {
            Call oc = sc.call;
            // Khởi tạo đối tượng Call mới với các thông tin sao chép từ đối tượng cũ
            Call nc = new Call(oc.getCustomerId(), oc.getCustomerName(), oc.getPhoneNumber(), oc.isVIP(), oc.getRepeatCalls(), oc.getOrderNumber());
            nc.setPriorityScore(oc.getPriorityScore());
            // Bọc Call mới vào SimCall mới có cùng arrivalTime và handlingTime
            clone.add(new SimCall(nc, sc.arrivalTime, sc.handlingTime));
        }
        return clone;
    }

    /**
     * Hiển thị thanh tiến trình (Progress Bar) chạy trực quan trên màn hình Console.
     * Sử dụng ký tự đặc biệt '\r' để ghi đè dòng hiện tại thay vì xuống dòng mới, tạo cảm giác chuyển động mượt mà.
     */
    private void printProgress(int processed, int total, String scenarioName) {
        int barWidth = 30; // Độ rộng của thanh tiến trình (số ký tự hiển thị)
        double percent = (double) processed / total;
        int filled = (int) (percent * barWidth);
        
        StringBuilder sb = new StringBuilder("\r  [");
        for (int i = 0; i < barWidth; i++) {
            if (i < filled) sb.append("█"); // Ký tự tô đầy thể hiện phần trạng thái đã xử lý
            else sb.append("░");            // Ký tự trống thể hiện phần chưa xử lý
        }
        sb.append(String.format("] %d/%d (%d%%) - %s", processed, total, (int)(percent * 100), scenarioName));
        System.out.print(sb.toString());
        
        // Khi hoàn thành 100%, in xuống dòng để chuẩn bị cho các dòng log tiếp theo
        if (processed == total) {
            System.out.println();
        }
    }

    /**
     * Giả lập thời gian xử lý thực tế bằng cách cho luồng ngủ (Thread.sleep), với thời lượng
     * được TÍNH TOÁN dựa trên tỷ lệ nén thời gian cố định 1:20 (xem TIME_COMPRESSION_RATIO), 
     * KHÔNG còn là con số random tùy tiện như trước.
     * 
     * Công thức: sleep trung bình mỗi cuộc gọi = TARGET_REAL_DURATION_MS / totalCalls
     * => Tổng thời gian sleep cộng dồn của toàn bộ totalCalls cuộc gọi sẽ xấp xỉ đúng bằng 
     *    TARGET_REAL_DURATION_MS (6 phút thật cho 1 giờ ảo), bất kể dataset sinh ra bao nhiêu cuộc gọi.
     * Có dao động ngẫu nhiên nhẹ +-30% quanh mức trung bình để progress bar chạy tự nhiên,
     * không đều tăm tắp một cách máy móc.
     * 
     * @param rand nguồn ngẫu nhiên dùng chung của kịch bản đang chạy
     * @param totalCalls tổng số cuộc gọi của kịch bản đang chạy (dùng để chia đều thời gian sleep)
     */
    private void simulateProcessing(Random rand, int totalCalls) {
        // Thời gian sleep trung bình lý thuyết cho mỗi cuộc gọi, để tổng thời gian thật khớp tỷ lệ 1:20
        long avgSleepMs = TARGET_REAL_DURATION_MS / totalCalls;
        if (avgSleepMs < 1) avgSleepMs = 1; // đảm bảo luôn có độ trễ tối thiểu, tránh chia hết về 0

        // Dao động ngẫu nhiên +-30% quanh mức trung bình để tạo nhịp tự nhiên cho progress bar
        long variance = Math.max(1, (long) (avgSleepMs * 0.3));
        long sleepMs = avgSleepMs + (rand.nextLong() % (variance * 2 + 1)) - variance;
        if (sleepMs < 0) sleepMs = 0;

        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Khôi phục trạng thái ngắt luồng nếu tiến trình bị gián đoạn đột ngột
        }
    }

    /**
     * KỊCH BẢN A: HỆ THỐNG HÀNG ĐỢI KÉP TÁCH BIỆT (DUAL QUEUE) - ƯU TIÊN TUYỆT ĐỐI VIP
     * 
     * [Nguyên tắc hoạt động]:
     * - Hệ thống duy trì 2 hàng đợi độc lập: vipQueue (chứa khách VIP) và regularQueue (chứa khách thường).
     * - Khi một cuộc gọi đến tại thời điểm t, nó được phân loại vào đúng hàng đợi của mình.
     * - Khi Agent rảnh tay:
     *   + Agent luôn kiểm tra vipQueue trước. Nếu có khách VIP, Agent sẽ phục vụ ngay lập tức.
     *   + CHỈ KHI hàng đợi vipQueue rỗng hoàn toàn, Agent mới bắt đầu phục vụ khách hàng thường trong regularQueue.
     * - Nguy cơ: Gây ra hiện tượng "Starvation" (đói thuật toán) nghiêm trọng cho khách thường nếu lượng khách VIP 
     *   đến liên tục, khiến khách thường bị kẹt lại phía sau vô thời hạn.
     */
    private void runDualQueueSimulation(List<SimCall> dataset) {
        int[] agentFreeTime = new int[NUM_AGENTS]; // agentFreeTime[i] lưu mốc thời điểm (giây ảo) mà Agent i sẽ rảnh tay để nhận cuộc gọi mới
        List<SimCall> vipQueue = new ArrayList<>(); // Hàng đợi lưu trữ các cuộc gọi VIP đang chờ
        List<SimCall> regularQueue = new ArrayList<>(); // Hàng đợi lưu trữ các cuộc gọi Thường đang chờ
        Random rand = new Random();

        int callIndex = 0;
        int totalCalls = dataset.size();
        int processedCount = 0;
        int t = 0; // Đồng hồ ảo (virtual clock) bắt đầu chạy từ giây thứ 0

        // Lặp cho đến khi toàn bộ cuộc gọi sinh ra được xử lý xong
        while (processedCount < totalCalls) {
            // Bước 1: Quét các cuộc gọi có thời điểm đến (arrivalTime) <= thời gian ảo hiện tại t để đưa vào hàng đợi
            while (callIndex < totalCalls && dataset.get(callIndex).arrivalTime <= t) {
                SimCall sc = dataset.get(callIndex);
                if (sc.call.isVIP()) {
                    vipQueue.add(sc);
                } else {
                    regularQueue.add(sc);
                }
                callIndex++;
            }

            // Bước 2: Duyệt qua danh sách Agent để tìm Agent rảnh tay tại thời điểm t
            for (int i = 0; i < NUM_AGENTS; i++) {
                if (agentFreeTime[i] <= t) {
                    SimCall nextCall = null;

                    // Áp dụng luật ưu tiên tuyệt đối: VIP được ưu tiên trước, Regular chỉ được gọi khi VIP trống
                    if (!vipQueue.isEmpty()) {
                        nextCall = vipQueue.remove(0); // Lấy cuộc gọi VIP đứng đầu hàng (FIFO trong nhóm VIP)
                    } else if (!regularQueue.isEmpty()) {
                        nextCall = regularQueue.remove(0); // Lấy cuộc gọi Thường đứng đầu hàng (FIFO trong nhóm Thường)
                    }

                    // Bước 3: Nếu tìm thấy cuộc gọi cần xử lý, tiến hành cập nhật số liệu
                    if (nextCall != null) {
                        // Thời gian chờ = Thời điểm phục vụ hiện tại (t) - Thời điểm đến (arrivalTime)
                        nextCall.waitTime = t - nextCall.arrivalTime; 
                        
                        // Agent i sẽ bận trong khoảng thời gian xử lý: cập nhật mốc rảnh tay tiếp theo
                        agentFreeTime[i] = t + nextCall.handlingTime; 
                        processedCount++;

                        // Tạo hiệu ứng trễ xử lý thực tế (đã tính theo tỷ lệ nén 1:20) và vẽ lại thanh tiến trình
                        simulateProcessing(rand, totalCalls);
                        printProgress(processedCount, totalCalls, "Scenario A");
                    }
                }
            }
            t++; // Đồng hồ ảo tăng lên 1 giây sau mỗi chu kỳ quét
        }
    }

    /**
     * KỊCH BẢN B: HÀNG ĐỢI ĐƠN TÍCH HỢP CƠ CHẾ CHỐNG NGHẼN/LÃO HÓA (SINGLE QUEUE + AGING)
     * 
     * [Nguyên tắc hoạt động]:
     * - Chỉ duy trì một hàng đợi duy nhất (priorityQueue) cho tất cả các cuộc gọi (cả VIP và Thường).
     * - Độ ưu tiên của mỗi cuộc gọi được đánh giá động dựa trên công thức:
     *     Tổng Điểm Ưu Tiên = Điểm Cấu Hình Ban Đầu (Priority Score) + Điểm Thưởng Tích Lũy Chờ Đợi (Aging Boost)
     * - Cơ chế Aging (Lão hóa): Cứ sau mỗi chu kỳ (60 giây ảo), tất cả các khách hàng THƯỜNG còn đang nằm chờ trong 
     *   hàng đợi sẽ được cộng thêm một lượng điểm ưu tiên (+15 điểm).
     * - Khi Agent rảnh tay:
     *   + Agent duyệt qua hàng đợi và chọn ra cuộc gọi có "Tổng Điểm Ưu Tiên" cao nhất để phục vụ.
     *   + Nếu có nhiều cuộc gọi trùng tổng điểm ưu tiên, áp dụng luật FIFO (ai đến trước phục vụ trước) làm tiêu chí phụ.
     * - Ưu điểm: Khách thường chờ càng lâu sẽ có điểm ưu tiên càng cao, dần dần vượt qua điểm của khách VIP mới đến, 
     *   giúp họ chắc chắn được phục vụ và loại bỏ hoàn toàn hiện tượng Starvation.
     */
    private void runSingleQueueAgingSimulation(List<SimCall> dataset) {
        int[] agentFreeTime = new int[NUM_AGENTS]; // agentFreeTime[i] lưu mốc thời điểm Agent i rảnh tay
        List<SimCall> priorityQueue = new ArrayList<>(); // Hàng đợi chung tích hợp cơ chế xếp hạng động
        Random rand = new Random();

        int callIndex = 0;
        int totalCalls = dataset.size();
        int processedCount = 0;
        int t = 0; // Đồng hồ ảo bắt đầu từ giây thứ 0

        while (processedCount < totalCalls) {
            // Bước 1: Quét và đẩy các cuộc gọi mới xuất hiện tại thời điểm t vào hàng đợi chung
            while (callIndex < totalCalls && dataset.get(callIndex).arrivalTime <= t) {
                priorityQueue.add(dataset.get(callIndex));
                callIndex++;
            }

            // Bước 2: Thực hiện cơ chế Aging (Lão hóa) định kỳ mỗi 60 giây ảo
            // Ghi chú: Chúng ta tận dụng trường waitTime của đối tượng Call (được mặc định là 0 khi khởi tạo) 
            // làm nơi lưu trữ điểm thưởng tích lũy (Aging Boost Points) của cuộc gọi đó.
            if (t > 0 && t % AGING_INTERVAL_SECONDS == 0) {
                for (SimCall sc : priorityQueue) {
                    if (!sc.call.isVIP()) {
                        // Cộng thêm điểm thưởng tích lũy cho khách hàng thường
                        sc.call.setWaitTime(sc.call.getWaitTime() + AGING_BOOST_POINTS);
                    }
                }
            }

            // Bước 3: Phân phối cuộc gọi cho Agent rảnh tay
            for (int i = 0; i < NUM_AGENTS; i++) {
                if (agentFreeTime[i] <= t) {
                    if (!priorityQueue.isEmpty()) {
                        
                        // Thuật toán tìm kiếm phần tử có độ ưu tiên cao nhất trong hàng đợi
                        int highestPriorityIndex = 0;
                        for (int j = 1; j < priorityQueue.size(); j++) {
                            // Tính tổng điểm ưu tiên của cuộc gọi đang xét ở vị trí j
                            int p1 = priorityQueue.get(j).call.getPriorityScore() + priorityQueue.get(j).call.getWaitTime();
                            
                            // Tính tổng điểm ưu tiên của cuộc gọi tốt nhất tìm thấy trước đó
                            int p2 = priorityQueue.get(highestPriorityIndex).call.getPriorityScore() + priorityQueue.get(highestPriorityIndex).call.getWaitTime();

                            if (p1 > p2) {
                                // Nếu cuộc gọi j có điểm ưu tiên cao hơn, ghi nhận index mới
                                highestPriorityIndex = j;
                            } else if (p1 == p2) {
                                // Quy tắc bổ trợ (Tie-breaker): Nếu bằng điểm nhau, ai có thời điểm đến (arrivalTime) sớm hơn sẽ được chọn
                                if (priorityQueue.get(j).arrivalTime < priorityQueue.get(highestPriorityIndex).arrivalTime) {
                                    highestPriorityIndex = j;
                                }
                            }
                        }

                        // Lấy cuộc gọi tối ưu nhất ra khỏi hàng đợi để xử lý
                        SimCall nextCall = priorityQueue.remove(highestPriorityIndex);
                        
                        // Tính toán thời gian chờ ảo thực tế = Thời điểm bắt đầu phục vụ (t) - Thời điểm đến (arrivalTime)
                        nextCall.waitTime = t - nextCall.arrivalTime; 
                        
                        // Cập nhật mốc thời gian Agent rảnh tay tiếp theo
                        agentFreeTime[i] = t + nextCall.handlingTime;
                        processedCount++;

                        // Tạo trễ thực tế (đã tính theo tỷ lệ nén 1:20) để hiển thị tiến trình mượt mà
                        simulateProcessing(rand, totalCalls);
                        printProgress(processedCount, totalCalls, "Scenario B");
                    }
                }
            }
            t++; // Tăng đồng hồ ảo
        }
    }

    /**
     * Hàm hỗ trợ chuyển đổi số giây ảo thành chuỗi văn bản dễ đọc gồm Phút và Giây.
     * Ví dụ: 64408.3 giây -> "1073 min 28.3 sec"
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
     * BÁO CÁO VÀ KẾT XUẤT KẾT QUẢ SO SÁNH
     * - Tính toán thời gian chờ trung bình (AWT) cho từng nhóm (VIP, Thường) và toàn bộ hệ thống.
     * - Tìm kiếm thời gian chờ lớn nhất (Max WT) để kiểm chứng tình trạng kẹt hàng đợi.
     * - Tính phần trăm cải thiện thời gian chờ của khách thường giữa hai kịch bản.
     * - Xuất dữ liệu ra file CSV để vẽ biểu đồ và lưu trữ.
     */
    private void printComparativeReport(List<SimCall> datasetA, List<SimCall> datasetB) {
        // --- 1. TÍNH TOÁN CÁC CHỈ SỐ CHO KỊCH BẢN A (DUAL QUEUE) ---
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

        // --- 2. TÍNH TOÁN CÁC CHỈ SỐ CHO KỊCH BẢN B (AGING QUEUE) ---
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

        // --- 3. ĐÁNH GIÁ MỨC ĐỘ CẢI THIỆN CHO KHÁCH HÀNG THƯỜNG ---
        // Tỷ lệ giảm thời gian chờ trung bình cho khách thường (%)
        double regAwtImprovement = (regAwtA > 0) ? ((regAwtA - regAwtB) / regAwtA) * 100.0 : 0;
        // Tỷ lệ giảm thời gian chờ tối đa cho khách thường (%)
        double regMaxImprovement = (maxRegA > 0) ? ((maxRegA - maxRegB) / (double) maxRegA) * 100.0 : 0;

        // --- 4. IN BẢNG BÁO CÁO CHI TIẾT LÊN CONSOLE ---
        System.out.println("\nSIMULATION METRICS REPORT");
        System.out.println("  Setup: 500 calls/hour | 20% VIP | 10 Agents | Speed: Compressed 1:" + TIME_COMPRESSION_RATIO);
        System.out.println("  ---------------------------------------------------------------------------------------------------------");
        System.out.printf("  %-45s │ %-28s │ %-28s%n",
                 "Metric Description",
                 "Scenario A (Dual Queue)",
                 "Scenario B (Aging Queue)");
        System.out.println("  ---------------------------------------------------------------------------------------------------------");

        System.out.println("  [1] Average queue wait time:");
        System.out.printf("   - Regular (Non-VIP) Customers                │ %-28s │ %-28s%n",
                formatDuration(regAwtA) + " (" + String.format("%.1f", regAwtA) + "s)",
                formatDuration(regAwtB) + " (" + String.format("%.1f", regAwtB) + "s)");
        System.out.printf("   - VIP Customers                              │ %-28s │ %-28s%n",
                formatDuration(vipAwtA) + " (" + String.format("%.1f", vipAwtA) + "s)",
                formatDuration(vipAwtB) + " (" + String.format("%.1f", vipAwtB) + "s)");
        System.out.printf("   - Overall System (All Customers)             │ %-28s │ %-28s%n",
                formatDuration(totalAwtA) + " (" + String.format("%.1f", totalAwtA) + "s)",
                formatDuration(totalAwtB) + " (" + String.format("%.1f", totalAwtB) + "s)");

        System.out.println("  [2] Maximum Wait Time (Max WT) - Longest single wait experienced:");
        System.out.printf("   - Regular (Non-VIP) Customers                │ %-28s │ %-28s%n",
                formatDuration(maxRegA) + " (" + maxRegA + "s)",
                formatDuration(maxRegB) + " (" + maxRegB + "s)");
        System.out.printf("   - VIP Customers                              │ %-28s │ %-28s%n",
                formatDuration(maxVipA) + " (" + maxVipA + "s)",
                formatDuration(maxVipB) + " (" + maxVipB + "s)");

        System.out.println("  ---------------------------------------------------------------------------------------------------------");
        System.out.printf("  Regular Customer Improvement: Average Wait Reduced by %.2f%% | Max Wait Reduced by %.2f%%%n",
                regAwtImprovement, regMaxImprovement);
        System.out.println("  (Positive % = Scenario B (Aging) is better for regular customers)");
        System.out.println("  ---------------------------------------------------------------------------------------------------------");

        // --- 5. GHI DỮ LIỆU KẾT QUẢ RA FILE CSV PHỤC VỤ VẼ BIỂU ĐỒ ---
        try {
            config.ConfigLoader loader = new config.ConfigLoader();
            String csvPath = loader.resolvePath("data/Exp1_PriorityQueue.csv");
            storage.FileHandler fh = new storage.FileHandler(csvPath);
            List<String> csvLines = new ArrayList<>();
            csvLines.add("Metric Description,Scenario A (Dual Queue) (seconds),Scenario B (Aging Queue) (seconds),Improvement (%)");
            csvLines.add(String.format(Locale.US, "Average Wait Time - Regular (Non-VIP) Customers,%.2f,%.2f,%.2f%%", regAwtA, regAwtB, regAwtImprovement));
            csvLines.add(String.format(Locale.US, "Average Wait Time - VIP Customers,%.2f,%.2f,N/A", vipAwtA, vipAwtB));
            csvLines.add(String.format(Locale.US, "Average Wait Time - Overall System (All Customers),%.2f,%.2f,%.2f%%", totalAwtA, totalAwtB, (totalAwtA > 0 ? ((totalAwtA - totalAwtB)/totalAwtA)*100.0 : 0)));
            csvLines.add(String.format(Locale.US, "Max Wait Time - Regular (Non-VIP) Customers,%d,%d,%.2f%%", maxRegA, maxRegB, regMaxImprovement));
            csvLines.add(String.format(Locale.US, "Max Wait Time - VIP Customers,%d,%d,N/A", maxVipA, maxVipB));
            fh.writeLines(csvLines);
            System.out.println("  Data saved to: " + csvPath);
        } catch (Exception e) {
            System.err.println("  Error writing CSV: " + e.getMessage());
        }

        // --- 6. TỔNG KẾT TĨNH KẾT QUẢ TRÊN CONSOLE ---
        System.out.println("\nMETRICS SUMMARY:");
        System.out.println("  Scenario A (Dual Queue):");
        System.out.println("     - Regular (Non-VIP) Customers AWT: " + formatDuration(regAwtA) + " | Max WT: " + formatDuration(maxRegA));
        System.out.println("     - VIP Customers AWT: " + formatDuration(vipAwtA) + " | Max WT: " + formatDuration(maxVipA));
        System.out.println("  Scenario B (Single Queue + Aging):");
        System.out.println("     - Regular (Non-VIP) Customers AWT: " + formatDuration(regAwtB) + " | Max WT: " + formatDuration(maxRegB));
        System.out.println("     - VIP Customers AWT: " + formatDuration(vipAwtB) + " | Max WT: " + formatDuration(maxVipB));
    }
}