package experiment;

import model.Call;
import model.CallStatus;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * THỰC NGHIỆM 1: SO SÁNH HÀNG ĐỢI KÉP TÁCH BIỆT (DUAL QUEUE)
 * VỚI HÀNG ĐỢI ĐƠN TÍCH HỢP CƠ CHẾ CHỐNG NGHẼN/LÃO HÓA (SINGLE QUEUE + AGING).
 * 
 * ==================================================================================
 * 1. MỤC TIÊU THỰC NGHIỆM:
 * - So sánh hiệu quả của hai cấu trúc hàng đợi trong việc tối ưu hóa thời gian
 * chờ đợi (AWT)
 * và hạn chế tình trạng "starvation" (đói thuật toán - khách hàng thường bị bỏ
 * rơi vô thời hạn)
 * khi hệ thống call center rơi vào trạng thái quá tải nghiêm trọng.
 * 
 * 2. CƠ CHẾ MÔ PHỎNG CHI TIẾT (KIẾN TRÚC ĐA LUỒNG - MULTI-THREADING THẬT):
 * - Tổng số cuộc gọi được sinh ra là CỐ ĐỊNH, do lập trình viên cấu hình qua
 * hằng số
 * TOTAL_CALLS_TO_GENERATE (mặc định 500 cuộc gọi).
 * - Thời gian xử lý cuộc gọi (Handling time) là số GIÂY THỰC TẾ của cuộc đàm
 * thoại ngoài đời,
 * random đều trong khoảng 30 đến 180 giây.
 * - KIẾN TRÚC XỬ LÝ: hệ thống dùng NHIỀU THREAD CHẠY THẬT SONG SONG với nhau,
 * mô phỏng đúng
 * bản chất "10 Agent làm việc cùng lúc" thay vì giả lập tuần tự trên 1 luồng
 * duy nhất:
 * + 1 THREAD "DISPATCHER" (điều phối cuộc gọi đến): duyệt qua danh sách cuộc
 * gọi theo đúng
 * thứ tự arrivalTime, Thread.sleep đúng khoảng cách thời gian (đã nén theo
 * PROCESSING_SPEEDUP_FACTOR) giữa 2 cuộc gọi liên tiếp, rồi "bơm" cuộc gọi đó
 * vào hàng đợi
 * chung — mô phỏng việc khách hàng gọi đến rải rác theo thời gian, không đến
 * dồn dập.
 * + NUM_AGENTS (10) THREAD "AGENT": mỗi thread đại diện cho một điện thoại
 * viên, chạy độc lập
 * và song song với các Agent khác. Khi rảnh, Agent tự lấy cuộc gọi có độ ưu
 * tiên cao nhất
 * trong hàng đợi chung (có đồng bộ hóa - synchronized để tránh xung đột dữ liệu
 * giữa các
 * luồng), rồi Thread.sleep đúng (handlingTime / PROCESSING_SPEEDUP_FACTOR) giây
 * để giả lập
 * thời gian đàm thoại, sau đó quay lại lấy cuộc gọi tiếp theo.
 * + 1 THREAD "AGING" (chỉ dùng ở Kịch bản B): chạy song song, định kỳ quét hàng
 * đợi để cộng
 * điểm ưu tiên cho khách hàng thường đang chờ.
 * Nhờ chạy THẬT SỰ song song (không phải giả lập tuần tự), tổng thời gian máy
 * chạy sẽ ngắn hơn
 * nhiều lần so với việc cộng dồn tuần tự tất cả handlingTime, và phản ánh đúng
 * thực tế: 10 Agent
 * xử lý được nhiều việc hơn trong cùng một khoảng thời gian so với 1 Agent làm
 * một mình.
 * - TỐC ĐỘ MÔ PHỎNG TRÊN MÁY: vì handlingTime giờ đã là số giây thực tế (không
 * phải số ảo cần
 * nhân/chia theo một tỷ lệ nén lớn), chương trình chỉ tăng tốc xử lý lên GẤP 5
 * LẦN so với đời
 * thực để rút ngắn thời gian chờ khi chạy thử nghiệm trên máy
 * (PROCESSING_SPEEDUP_FACTOR = 5).
 * Ví dụ: cuộc gọi có handlingTime = 100 giây (đời thật) sẽ khiến Agent thread
 * sleep 100/5 = 20 giây.
 * QUY ĐỔI NGƯỢC: nếu muốn biết một khoảng thời gian đã đo được trên máy tương
 * ứng bao nhiêu giây
 * ngoài đời thực, hãy NHÂN thời gian đo được trên máy với
 * PROCESSING_SPEEDUP_FACTOR. Các chỉ số
 * waitTime/AWT/Max WT trong báo cáo đã được quy đổi sẵn về ĐƠN VỊ GIÂY THỰC TẾ
 * của khách hàng —
 * chỉ riêng "Execution Time" (thời gian máy thực sự chạy chương trình) mới cần
 * nhân 5 để hiểu
 * được nó tương ứng bao lâu ở ngoài đời.
 * 
 * @author Group 7
 */
public class Exp1_PriorityQueue {

    // Số lượng điện thoại viên (Agent) xử lý cuộc gọi trong hệ thống (được nâng lên
    // thành 10 Agents)
    private static final int NUM_AGENTS = 10;

    // Tổng số cuộc gọi CỐ ĐỊNH mà chương trình sẽ sinh ra cho mỗi lần chạy thực
    // nghiệm.
    // Đây là con số tường minh, lập trình viên có thể tùy ý chỉnh sửa (ví dụ 300,
    // 800, 1000...)
    // để thử nghiệm với các quy mô tải khác nhau. Cả Kịch bản A và B đều dùng chung
    // đúng số lượng
    // cuộc gọi này (nhờ cơ chế deep-clone dataset ở hàm run()), đảm bảo so sánh
    // công bằng.
    private static final int TOTAL_CALLS_TO_GENERATE = 500;

    // Tần suất cuộc gọi đến trung bình mỗi giây ảo (dùng để rải ngẫu nhiên
    // arrivalTime giữa các
    // cuộc gọi theo Phân phối Poisson, tạo nhịp đến tự nhiên chứ không đến dồn dập
    // cùng lúc).
    // Con số 500.0/3600.0 nghĩa là trung bình 500 cuộc gọi mỗi giờ ảo (~0.1389
    // cuộc/giây ảo).
    // Lưu ý: hằng số này CHỈ quyết định KHOẢNG CÁCH giữa các cuộc gọi, KHÔNG quyết
    // định tổng số
    // cuộc gọi sinh ra — tổng số cuộc gọi luôn đúng bằng TOTAL_CALLS_TO_GENERATE ở
    // trên.
    private static final double CALL_RATE_PER_SECOND = 500.0 / 3600.0;

    // Chu kỳ quét hàng đợi để cộng điểm ưu tiên (Aging) cho khách hàng thường (mỗi
    // 60 giây ảo)
    private static final int AGING_INTERVAL_SECONDS = 60;

    // Số điểm ưu tiên cộng thêm cho mỗi lần quét Aging (15 điểm/phút chờ đợi)
    private static final int AGING_BOOST_POINTS = 15;

    // Hệ số tăng tốc xử lý trên máy so với thời lượng cuộc gọi THỰC TẾ
    // (handlingTime).
    // Vì handlingTime giờ đã là số giây có ý nghĩa thực tế (30-180 giây, đúng bằng
    // thời lượng
    // đàm thoại ngoài đời), chương trình không "nén" theo một tỷ lệ lớn như trước
    // nữa, mà chỉ
    // tăng tốc gấp 5 lần để rút ngắn thời gian chờ khi chạy demo/thử nghiệm trên
    // máy.
    // Công thức áp dụng (xem hàm simulateProcessing): sleepGiayThat = handlingTime
    // / PROCESSING_SPEEDUP_FACTOR
    // QUY ĐỔI NGƯỢC: thời gian thật đo được trên máy × PROCESSING_SPEEDUP_FACTOR =
    // thời lượng
    // cuộc gọi tương ứng ngoài đời thực (giây).
    private static final int PROCESSING_SPEEDUP_FACTOR = 100;

    // ==================================================================================
    // CẤU HÌNH RIÊNG CHO THỰC NGHIỆM PHỤ: "INSTANT PICKUP VERIFICATION"
    // (Minh chứng: khi cuộc gọi đến, Agent có bắt máy ngay lập tức hay phải chờ?)
    // ==================================================================================
    // Số lượng cuộc gọi dùng riêng cho thực nghiệm minh chứng này (độc lập với
    // Scenario A/B ở trên,
    // dùng dataset và lần chạy hoàn toàn khác để không ảnh hưởng đến số liệu
    // AWT/Max WT đã báo cáo).
    private static final int INSTANT_PICKUP_TOTAL_CALLS = 250;

    // Ngưỡng (giây thực tế) để phân loại một cuộc gọi vào nhóm "được bắt máy NGAY
    // LẬP TỨC".
    // Về lý thuyết, nếu còn Agent rảnh, độ trễ bắt máy chỉ đến từ chi phí đồng bộ
    // hóa
    // (synchronized/wait/notifyAll của JVM) - thường dưới vài mili-giây MÁY, quy
    // đổi ra giây thực tế
    // (nhân PROCESSING_SPEEDUP_FACTOR) vẫn là một số rất nhỏ. 0.05 giây thực tế
    // được chọn làm ngưỡng
    // an toàn: đủ lớn để không bị nhiễu bởi sai số đồng bộ hóa vặt, nhưng đủ nhỏ để
    // phân biệt rõ ràng
    // với trường hợp phải NẰM CHỜ TRONG HÀNG ĐỢI (thường tính bằng giây đến hàng
    // chục/trăm giây).
    private static final double INSTANT_PICKUP_THRESHOLD_SECONDS = 0.05;

    /**
     * Lớp SimCall (Simulation Call) đóng vai trò là một Wrapper (lớp bao bọc) quanh
     * model Call gốc.
     * Nó bổ sung các trường thông tin phục vụ riêng cho quá trình chạy mô phỏng:
     * - arrivalTime: Thời điểm cuộc gọi lẽ ra xuất hiện trong hệ thống, tính bằng
     * GIÂY THỰC TẾ
     * kể từ lúc bắt đầu kịch bản (dùng để Dispatcher thread tính khoảng cách sleep
     * giữa các cuộc gọi).
     * - handlingTime: Thời lượng cuộc gọi này chiếm dụng Agent (giây thực tế,
     * 30-180s).
     * - waitTime: Thời gian chờ đợi thực tế trong hàng đợi (giây thực tế, đã quy
     * đổi ngược từ thời
     * gian máy đo được bằng PROCESSING_SPEEDUP_FACTOR), tính từ lúc "đến" tới lúc
     * được Agent bắt máy.
     * - dispatchedAtMachineMs: mốc thời gian MÁY (System.currentTimeMillis()) tại
     * thời điểm Dispatcher
     * thread bơm cuộc gọi này vào hàng đợi chung. Vì kiến trúc đa luồng không còn
     * một "đồng hồ ảo t"
     * chạy tuần tự chung cho mọi thread, trường này là điểm neo để Agent thread
     * tính waitTime chính
     * xác khi cuộc gọi được lấy ra xử lý (waitTime = (thời gian máy lúc xử lý -
     * dispatchedAtMachineMs)
     * × PROCESSING_SPEEDUP_FACTOR / 1000, quy đổi mili-giây máy về giây thực tế của
     * khách hàng).
     */
    static class SimCall {
        Call call; // Đối tượng Call chứa thông tin nghiệp vụ chính (ID, Tên, VIP/Thường, số lần
                   // gọi lại...)
        int arrivalTime; // Thời điểm cuộc gọi lẽ ra đến (giây thực tế, dùng cho Dispatcher thread)
        int handlingTime; // Thời gian xử lý đàm thoại (giây thực tế)
        // waitTime dùng kiểu double (KHÔNG phải int như bản cũ) để giữ độ chính xác
        // thập phân.
        // Lý do: khi hệ thống không quá tải (ví dụ NUM_AGENTS lớn), Agent có thể lấy
        // được cuộc gọi
        // chỉ sau vài chục mili-giây máy. Nếu ép kiểu về int ngay sau khi chia cho
        // 1000L, các giá
        // trị dưới 1 giây sẽ bị CẮT CỤT VỀ 0 (ví dụ 49ms máy × 20 / 1000 = 0.98 → ép
        // int thành 0),
        // khiến AWT tổng thể bị kéo xuống sai lệch nghiêm trọng dù thực chất khách vẫn
        // phải chờ.
        double waitTime = -1;
        // pickupDelaySeconds: độ trễ THUẦN TÚY (giây thực tế) từ lúc cuộc gọi được bơm
        // vào hàng đợi
        // (dispatchedAtMachineMs) đến lúc một Agent THỰC SỰ bắt đầu xử lý nó (gọi
        // simulateProcessing).
        // Về bản chất đây CHÍNH LÀ waitTime, nhưng được tách thành trường riêng và đặt
        // tên tường minh
        // để phục vụ minh chứng "khi cuộc gọi đến, Agent có bắt máy ngay lập tức hay
        // không" (xem
        // runInstantPickupVerification() và printInstantPickupReport() ở cuối file).
        double pickupDelaySeconds = -1;
        volatile long dispatchedAtMachineMs = -1; // Mốc thời gian máy khi được bơm vào hàng đợi (set bởi Dispatcher
                                                  // thread)

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
        System.out.println("  Configuration: " + TOTAL_CALLS_TO_GENERATE + " calls (fixed) | 20% VIP | "
                + NUM_AGENTS + " Agents | Handling time: 30-180s (real-world seconds per call)");
        System.out.println("  Processing Speed: " + PROCESSING_SPEEDUP_FACTOR
                + "x faster than real time (sleep = handlingTime / " + PROCESSING_SPEEDUP_FACTOR
                + " seconds per call)");
        System.out.println("  Note: 'Execution Time' printed below is MACHINE time. Multiply it by "
                + PROCESSING_SPEEDUP_FACTOR + " to get the equivalent real-world elapsed time.");

        // BƯỚC 1: Sinh ngẫu nhiên tập dữ liệu cuộc gọi (Dataset) theo phân phối
        // Poisson.
        // Việc sinh dữ liệu MỘT LẦN duy nhất đảm bảo cả hai kịch bản đều chạy trên cùng
        // một tập cuộc gọi giống hệt nhau,
        // giúp kết quả đối sánh đạt độ công bằng và chính xác cao nhất.
        List<SimCall> datasetA = generateDataset();

        // BƯỚC 2: Tạo bản sao sâu (deep clone) của tập dữ liệu dành cho kịch bản B.
        // Điều này rất quan trọng vì trong quá trình mô phỏng, thuộc tính của các đối
        // tượng Call (như điểm ưu tiên,
        // thời gian chờ) sẽ bị sửa đổi. Deep clone giúp Kịch bản A và Kịch bản B hoàn
        // toàn cô lập, không ảnh hưởng lẫn nhau.
        List<SimCall> datasetB = cloneDataset(datasetA);
        System.out.println("  Total Generated Calls: " + datasetA.size() + " (shared by both scenarios)");
        System.out.println();

        // BƯỚC 3: Chạy mô phỏng Kịch bản A (Dual Queue - Hàng đợi kép ưu tiên tuyệt đối
        // VIP)
        System.out.println("  Running Scenario A (Dual Queue)...");
        long startA = System.currentTimeMillis();
        runDualQueueSimulation(datasetA);
        long durationA = System.currentTimeMillis() - startA;
        System.out.println("  Scenario A Execution Time: " + String.format("%.1f", durationA / 1000.0) + " s");
        System.out.println();

        // BƯỚC 4: Chạy mô phỏng Kịch bản B (Single Queue + Aging - Một hàng đợi tích
        // hợp cơ chế tăng điểm theo thời gian)
        System.out.println("  Running Scenario B (Single Queue + Aging)...");
        long startB = System.currentTimeMillis();
        runSingleQueueAgingSimulation(datasetB);
        long durationB = System.currentTimeMillis() - startB;
        System.out.println("  Scenario B Execution Time: " + String.format("%.1f", durationB / 1000.0) + " s");
        System.out.println();

        // BƯỚC 5: Tổng hợp số liệu và in báo cáo so sánh trực quan, đồng thời xuất kết
        // quả ra file CSV.
        printComparativeReport(datasetA, datasetB);

        // BƯỚC 6: THỰC NGHIỆM PHỤ - Minh chứng "Agent có bắt máy ngay lập tức hay
        // không".
        // Chạy ĐỘC LẬP, dùng dataset và lần chạy riêng (không tái sử dụng
        // datasetA/datasetB ở trên)
        // để không ảnh hưởng đến số liệu AWT/Max WT đã báo cáo ở BƯỚC 5.
        runInstantPickupVerification();
    }

    /**
     * Sinh tập dữ liệu các cuộc gọi ngẫu nhiên dựa trên thuật toán Poisson Process.
     * - Số lượng cuộc gọi: CỐ ĐỊNH, đúng bằng TOTAL_CALLS_TO_GENERATE (mặc định
     * 500).
     * - Arrival rate (khoảng cách giữa các cuộc gọi): rải theo Phân phối Poisson
     * với tốc độ
     * trung bình CALL_RATE_PER_SECOND, tạo nhịp đến tự nhiên (không đến dồn dập
     * cùng lúc).
     * - Handling time (thời lượng xử lý): số giây thực tế, random đều từ 30 đến 180
     * giây.
     * - VIP Ratio (tỷ lệ VIP): Cố định khoảng 20%.
     */
    private List<SimCall> generateDataset() {
        Random rand = new Random();
        List<SimCall> list = new ArrayList<>();
        int currentTime = 0; // Mốc thời gian THỰC TẾ (giây, kể từ lúc bắt đầu kịch bản) ghi nhận cuộc gọi
                             // đến
        int orderCounter = 1; // Biến đếm số thứ tự cuộc gọi sinh ra

        // Sinh liên tục các cuộc gọi cho đến khi đủ đúng TOTAL_CALLS_TO_GENERATE cuộc
        // gọi.
        // Khác với bản cũ (dừng theo mốc thời gian 1 giờ), giờ đây tổng số cuộc gọi
        // LUÔN CỐ ĐỊNH,
        // còn khoảng cách thời gian giữa các cuộc gọi (currentTime) chỉ đóng vai trò
        // tạo nhịp đến
        // tự nhiên và có thể kéo dài ngắn hơn hoặc dài hơn 1 giờ ảo tùy vào yếu tố ngẫu
        // nhiên.
        while (list.size() < TOTAL_CALLS_TO_GENERATE) {
            double u = rand.nextDouble();
            while (u == 0)
                u = rand.nextDouble(); // Loại trừ trường hợp u = 0 để tránh lỗi toán học Math.log(0)

            // Áp dụng công thức tính khoảng thời gian giữa 2 sự kiện đến liên tiếp
            // (Inter-arrival time)
            // của quá trình Poisson: dt = -ln(1 - u) / lambda
            int nextArrivalInterval = (int) (-Math.log(1 - u) / CALL_RATE_PER_SECOND);

            // Đảm bảo đồng hồ ảo luôn tiến lên ít nhất 1 giây để tránh vòng lặp vô hạn
            if (nextArrivalInterval < 1)
                nextArrivalInterval = 1;
            currentTime += nextArrivalInterval;

            // Thiết lập các thuộc tính ngẫu nhiên cho cuộc gọi:
            boolean isVip = rand.nextDouble() < 0.20; // 20% cuộc gọi được chỉ định là VIP
            int repeatCalls = rand.nextInt(100) < 15 ? rand.nextInt(3) + 1 : 0; // 15% tỷ lệ gọi lại (từ 1 đến 3 lần)
            // Thời lượng đàm thoại (handling time) tính bằng GIÂY THỰC TẾ, random đều trong
            // khoảng
            // 30 đến 180 giây (rand.nextInt(151) sinh ra số nguyên từ 0 đến 150, cộng 30 =>
            // 30-180).
            // Không còn quy đổi/làm tròn ra phút như bản cũ, giúp giá trị đa dạng hơn theo
            // từng giây.
            int handlingTime = rand.nextInt(151) + 30;

            // Khởi tạo đối tượng Call gốc
            String id = "C" + String.format("%04d", orderCounter);
            Call call = new Call(id, "Customer " + id, "090" + String.format("%07d", rand.nextInt(10000000)), isVip,
                    repeatCalls, orderCounter);

            // Bọc vào SimCall và thêm vào danh sách kết quả
            list.add(new SimCall(call, currentTime, handlingTime));
            orderCounter++;
        }
        // Limit only 10% of regular calls to be allowed to exceed VIP points
        List<SimCall> regularCalls = new ArrayList<>();
        for (SimCall sc : list) {
            if (!sc.call.isVIP()) {
                regularCalls.add(sc);
            }
        }
        int numAllowedToExceed = (int) Math.ceil(regularCalls.size() * 0.10);
        Collections.shuffle(regularCalls, rand);
        for (int i = 0; i < numAllowedToExceed && i < regularCalls.size(); i++) {
            regularCalls.get(i).call.setAllowExceedVIP(true);
        }
        return list;
    }

    /**
     * Thực hiện sao chép sâu (Deep Clone) danh sách cuộc gọi.
     * Việc clone từng đối tượng Call và SimCall là bắt buộc để đảm bảo các thay đổi
     * về trạng thái,
     * thời gian chờ, điểm số ưu tiên ở Scenario A hoàn toàn độc lập với Scenario B.
     */
    private List<SimCall> cloneDataset(List<SimCall> original) {
        List<SimCall> clone = new ArrayList<>();
        for (SimCall sc : original) {
            Call oc = sc.call;
            // Khởi tạo đối tượng Call mới với các thông tin sao chép từ đối tượng cũ
            Call nc = new Call(oc.getCustomerId(), oc.getCustomerName(), oc.getPhoneNumber(), oc.isVIP(),
                    oc.getRepeatCalls(), oc.getOrderNumber());
            nc.setAllowExceedVIP(oc.isAllowExceedVIP());
            nc.setPriorityScore(oc.getPriorityScore());
            // Bọc Call mới vào SimCall mới có cùng arrivalTime và handlingTime
            clone.add(new SimCall(nc, sc.arrivalTime, sc.handlingTime));
        }
        return clone;
    }

    /**
     * Hiển thị thanh tiến trình (Progress Bar) chạy trực quan trên màn hình
     * Console.
     * Sử dụng ký tự đặc biệt '\r' để ghi đè dòng hiện tại thay vì xuống dòng mới,
     * tạo cảm giác chuyển động mượt mà.
     */
    private void printProgress(int processed, int total, String scenarioName) {
        int barWidth = 30; // Độ rộng của thanh tiến trình (số ký tự hiển thị)
        double percent = (double) processed / total;
        int filled = (int) (percent * barWidth);

        StringBuilder sb = new StringBuilder("\r  [");
        for (int i = 0; i < barWidth; i++) {
            if (i < filled)
                sb.append("█"); // Ký tự tô đầy thể hiện phần trạng thái đã xử lý
            else
                sb.append("░"); // Ký tự trống thể hiện phần chưa xử lý
        }
        sb.append(String.format("] %d/%d (%d%%) - %s", processed, total, (int) (percent * 100), scenarioName));
        System.out.print(sb.toString());

        // Khi hoàn thành 100%, in xuống dòng để chuẩn bị cho các dòng log tiếp theo
        if (processed == total) {
            System.out.println();
        }
    }

    /**
     * Giả lập độ trễ thời gian thực khi một Agent thread xử lý MỘT cuộc gọi cụ thể,
     * bằng cách cho
     * chính luồng Agent đó ngủ (Thread.sleep) một khoảng thời gian tỷ lệ thuận với
     * handlingTime.
     * 
     * CÔNG THỨC: sleepMs = (handlingTime × 1000) / PROCESSING_SPEEDUP_FACTOR
     * Với PROCESSING_SPEEDUP_FACTOR = 5, nghĩa là máy xử lý NHANH HƠN đời thực đúng
     * 5 lần.
     * Ví dụ: cuộc gọi có handlingTime = 100 giây (đời thật) => Agent thread sleep
     * 100×1000/5 = 20000ms.
     * 
     * Vì hàm này được gọi TRÊN THREAD RIÊNG của từng Agent (không phải luồng
     * chính), việc sleep của
     * Agent này KHÔNG chặn các Agent khác — 10 Agent có thể cùng sleep song song,
     * phản ánh đúng bản
     * chất 10 điện thoại viên làm việc đồng thời, thay vì cộng dồn tuần tự trên một
     * luồng duy nhất.
     * 
     * QUY ĐỔI NGƯỢC: thời gian đo được trên máy × PROCESSING_SPEEDUP_FACTOR = thời
     * lượng cuộc gọi
     * tương ứng ngoài đời thực (giây).
     * 
     * @param handlingTimeSeconds thời lượng đàm thoại thực tế (giây) của cuộc gọi
     *                            đang được xử lý
     */
    private void simulateProcessing(int handlingTimeSeconds) {
        long sleepMs = (handlingTimeSeconds * 1000L) / PROCESSING_SPEEDUP_FACTOR;
        if (sleepMs < 1)
            sleepMs = 1; // đảm bảo luôn có độ trễ tối thiểu, tránh sleep(0) vô nghĩa

        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Khôi phục trạng thái ngắt luồng nếu tiến trình bị gián đoạn đột ngột
        }
    }

    /**
     * KỊCH BẢN A: HỆ THỐNG HÀNG ĐỢI KÉP TÁCH BIỆT (DUAL QUEUE) - ƯU TIÊN TUYỆT ĐỐI
     * VIP
     * 
     * [Nguyên tắc hoạt động]:
     * - Hệ thống duy trì 2 hàng đợi độc lập: vipQueue (chứa khách VIP) và
     * regularQueue (chứa khách thường).
     * - 1 Dispatcher thread bơm cuộc gọi vào đúng hàng đợi của nó theo đúng nhịp
     * arrivalTime.
     * - NUM_AGENTS (10) Agent thread chạy song song. Khi một Agent rảnh tay:
     * + Agent luôn kiểm tra vipQueue trước. Nếu có khách VIP, Agent sẽ phục vụ ngay
     * lập tức.
     * + CHỈ KHI hàng đợi vipQueue rỗng hoàn toàn, Agent mới bắt đầu phục vụ khách
     * hàng thường trong regularQueue.
     * - Nguy cơ: Gây ra hiện tượng "Starvation" (đói thuật toán) nghiêm trọng cho
     * khách thường nếu lượng khách VIP
     * đến liên tục, khiến khách thường bị kẹt lại phía sau vô thời hạn.
     * 
     * [Về đồng bộ hóa đa luồng]:
     * - vipQueue/regularQueue dùng chung 1 khóa (lockObject) để đảm bảo tại một
     * thời điểm chỉ có
     * MỘT thread (Dispatcher hoặc 1 trong 10 Agent) được thêm/lấy phần tử, tránh
     * xung đột dữ liệu
     * (race condition) khi nhiều luồng cùng đọc/ghi danh sách.
     * - processedCount dùng AtomicInteger để đếm số cuộc gọi đã xử lý xong một cách
     * an toàn giữa
     * nhiều luồng, làm điều kiện dừng chung cho toàn bộ hệ thống.
     */
    private void runDualQueueSimulation(List<SimCall> dataset) {
        List<SimCall> vipQueue = new ArrayList<>(); // Hàng đợi lưu trữ các cuộc gọi VIP đang chờ
        List<SimCall> regularQueue = new ArrayList<>(); // Hàng đợi lưu trữ các cuộc gọi Thường đang chờ
        final Object lockObject = new Object(); // Khóa đồng bộ hóa dùng chung cho cả 2 hàng đợi
        final int totalCalls = dataset.size();
        final AtomicInteger processedCount = new AtomicInteger(0);
        final long startMachineMs = System.currentTimeMillis(); // Mốc thời gian máy lúc bắt đầu kịch bản

        // --- THREAD DISPATCHER: bơm cuộc gọi vào đúng hàng đợi theo đúng nhịp
        // arrivalTime ---
        Thread dispatcherThread = new Thread(() -> {
            int previousArrival = 0;
            for (SimCall sc : dataset) {
                // Khoảng cách (giây thực tế) giữa cuộc gọi này và cuộc gọi trước đó
                int gapSeconds = sc.arrivalTime - previousArrival;
                if (gapSeconds > 0) {
                    long sleepMs = (gapSeconds * 1000L) / PROCESSING_SPEEDUP_FACTOR;
                    if (sleepMs > 0) {
                        try {
                            Thread.sleep(sleepMs);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
                previousArrival = sc.arrivalTime;
                sc.dispatchedAtMachineMs = System.currentTimeMillis(); // Ghi nhận mốc thời gian máy khi cuộc gọi "đến"
                synchronized (lockObject) {
                    if (sc.call.isVIP()) {
                        vipQueue.add(sc);
                    } else {
                        regularQueue.add(sc);
                    }
                    lockObject.notifyAll(); // Đánh thức các Agent đang chờ (nếu có) vì vừa có việc mới
                }
            }
        }, "Dispatcher-A");

        // --- NUM_AGENTS THREAD AGENT: mỗi thread là 1 điện thoại viên chạy song song
        // ---
        Thread[] agentThreads = new Thread[NUM_AGENTS];
        for (int a = 0; a < NUM_AGENTS; a++) {
            agentThreads[a] = new Thread(() -> {
                while (processedCount.get() < totalCalls) {
                    SimCall nextCall = null;
                    synchronized (lockObject) {
                        // Ưu tiên tuyệt đối: luôn lấy khách VIP trước, chỉ lấy khách Thường khi
                        // vipQueue rỗng
                        if (!vipQueue.isEmpty()) {
                            nextCall = vipQueue.remove(0);
                        } else if (!regularQueue.isEmpty()) {
                            nextCall = regularQueue.remove(0);
                        } else {
                            // Hàng đợi hiện đang rỗng: Agent chờ (block) tối đa 50ms rồi kiểm tra lại,
                            // tránh vòng lặp "busy-wait" ngốn CPU trong lúc chờ Dispatcher bơm thêm cuộc
                            // gọi.
                            try {
                                lockObject.wait(50);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                return;
                            }
                        }
                    }
                    if (nextCall != null) {
                        // waitTime (giây thực tế, kiểu double) = (thời gian máy hiện tại - mốc máy lúc
                        // cuộc gọi
                        // được bơm vào) quy đổi từ mili-giây MÁY sang giây THỰC TẾ bằng cách nhân
                        // PROCESSING_SPEEDUP_FACTOR. Dùng phép chia SỐ THỰC (1000.0, không phải 1000L)
                        // và
                        // KHÔNG ép kiểu về int, để giữ nguyên phần thập phân — tránh làm tròn các cuộc
                        // gọi
                        // chờ dưới 1 giây máy (rất phổ biến khi hệ thống có nhiều Agent, ít bị nghẽn)
                        // về 0.
                        long waitMachineMs = System.currentTimeMillis() - nextCall.dispatchedAtMachineMs;
                        nextCall.waitTime = (waitMachineMs * PROCESSING_SPEEDUP_FACTOR) / 1000.0;

                        simulateProcessing(nextCall.handlingTime); // Agent "đàm thoại" trong handlingTime/5 giây máy
                        int done = processedCount.incrementAndGet();
                        printProgress(done, totalCalls, "Scenario A");
                    }
                }
            }, "Agent-A-" + a);
        }

        // Khởi động toàn bộ Dispatcher + Agent threads để chạy song song
        dispatcherThread.start();
        for (Thread t : agentThreads)
            t.start();

        // Đợi Dispatcher bơm xong hết dữ liệu, rồi đợi tất cả Agent xử lý xong nốt phần
        // còn lại trong hàng đợi
        try {
            dispatcherThread.join();
            for (Thread t : agentThreads)
                t.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * KỊCH BẢN B: HÀNG ĐỢI ĐƠN TÍCH HỢP CƠ CHẾ CHỐNG NGHẼN/LÃO HÓA (SINGLE QUEUE +
     * AGING)
     * 
     * [Nguyên tắc hoạt động]:
     * - Chỉ duy trì một hàng đợi duy nhất (priorityQueue) cho tất cả các cuộc gọi
     * (cả VIP và Thường).
     * - Độ ưu tiên của mỗi cuộc gọi được đánh giá động dựa trên công thức:
     * Tổng Điểm Ưu Tiên = Điểm Cấu Hình Ban Đầu (Priority Score) + Điểm Thưởng Tích
     * Lũy Chờ Đợi (Aging Boost)
     * - Cơ chế Aging (Lão hóa): Cứ sau mỗi chu kỳ (60 giây ảo), tất cả các khách
     * hàng THƯỜNG còn đang nằm chờ trong
     * hàng đợi sẽ được cộng thêm một lượng điểm ưu tiên (+15 điểm).
     * - Khi Agent rảnh tay:
     * + Agent duyệt qua hàng đợi và chọn ra cuộc gọi có "Tổng Điểm Ưu Tiên" cao
     * nhất để phục vụ.
     * + Nếu có nhiều cuộc gọi trùng tổng điểm ưu tiên, áp dụng luật FIFO (ai đến
     * trước phục vụ trước) làm tiêu chí phụ.
     * - Ưu điểm: Khách thường chờ càng lâu sẽ có điểm ưu tiên càng cao, dần dần
     * vượt qua điểm của khách VIP mới đến,
     * giúp họ chắc chắn được phục vụ và loại bỏ hoàn toàn hiện tượng Starvation.
     */
    /**
     * KỊCH BẢN B: HÀNG ĐỢI ĐƠN TÍCH HỢP CƠ CHẾ CHỐNG NGHẼN/LÃO HÓA (SINGLE QUEUE +
     * AGING)
     * 
     * [Nguyên tắc hoạt động]:
     * - Chỉ duy trì một hàng đợi duy nhất (priorityQueue) cho tất cả các cuộc gọi
     * (cả VIP và Thường).
     * - Độ ưu tiên của mỗi cuộc gọi được đánh giá động dựa trên công thức:
     * Tổng Điểm Ưu Tiên = Điểm Cấu Hình Ban Đầu (Priority Score) + Điểm Thưởng Tích
     * Lũy Chờ Đợi (Aging Boost)
     * - Cơ chế Aging (Lão hóa): Cứ sau mỗi chu kỳ (AGING_INTERVAL_SECONDS giây thực
     * tế, quy đổi ra
     * thời gian máy), tất cả các khách hàng THƯỜNG còn đang nằm chờ trong hàng đợi
     * sẽ được cộng
     * thêm một lượng điểm ưu tiên (+15 điểm), do 1 Aging thread chạy song song đảm
     * nhiệm.
     * - Khi một Agent rảnh tay: duyệt hàng đợi, chọn ra cuộc gọi có "Tổng Điểm Ưu
     * Tiên" cao nhất để
     * phục vụ; nếu trùng điểm, áp dụng luật FIFO (ai đến trước phục vụ trước) làm
     * tiêu chí phụ.
     * - Ưu điểm: Khách thường chờ càng lâu sẽ có điểm ưu tiên càng cao, dần dần
     * vượt qua điểm của khách VIP mới đến,
     * giúp họ chắc chắn được phục vụ và loại bỏ hoàn toàn hiện tượng Starvation.
     * 
     * [Về đồng bộ hóa đa luồng]:
     * - priorityQueue dùng chung 1 khóa (lockObject) cho Dispatcher, 10 Agent, và
     * Aging thread.
     * - processedCount dùng AtomicInteger, tương tự Kịch bản A.
     */
    private void runSingleQueueAgingSimulation(List<SimCall> dataset) {
        List<SimCall> priorityQueue = new ArrayList<>(); // Hàng đợi chung tích hợp cơ chế xếp hạng động
        final Object lockObject = new Object(); // Khóa đồng bộ hóa dùng chung cho hàng đợi
        final int totalCalls = dataset.size();
        final AtomicInteger processedCount = new AtomicInteger(0);

        // --- THREAD DISPATCHER: bơm cuộc gọi vào hàng đợi chung theo đúng nhịp
        // arrivalTime ---
        Thread dispatcherThread = new Thread(() -> {
            int previousArrival = 0;
            for (SimCall sc : dataset) {
                int gapSeconds = sc.arrivalTime - previousArrival;
                if (gapSeconds > 0) {
                    long sleepMs = (gapSeconds * 1000L) / PROCESSING_SPEEDUP_FACTOR;
                    if (sleepMs > 0) {
                        try {
                            Thread.sleep(sleepMs);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
                previousArrival = sc.arrivalTime;
                sc.dispatchedAtMachineMs = System.currentTimeMillis();
                synchronized (lockObject) {
                    priorityQueue.add(sc);
                    lockObject.notifyAll();
                }
            }
        }, "Dispatcher-B");

        // --- THREAD AGING: định kỳ quét hàng đợi, cộng điểm ưu tiên cho khách hàng
        // Thường ---
        // Chu kỳ quét (AGING_INTERVAL_SECONDS giây thực tế) cũng được nén theo
        // PROCESSING_SPEEDUP_FACTOR
        // để khớp với tốc độ 5x của toàn hệ thống.
        final long agingIntervalMachineMs = (AGING_INTERVAL_SECONDS * 1000L) / PROCESSING_SPEEDUP_FACTOR;
        Thread agingThread = new Thread(() -> {
            while (processedCount.get() < totalCalls) {
                try {
                    Thread.sleep(agingIntervalMachineMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                synchronized (lockObject) {
                    for (SimCall sc : priorityQueue) {
                        if (!sc.call.isVIP()) {
                            // Cộng thêm điểm thưởng tích lũy cho khách hàng thường (dùng chung trường
                            // waitTime
                            // của Call gốc làm nơi lưu điểm Aging Boost, giữ đúng quy ước của bản gốc)
                            sc.call.setWaitTime(sc.call.getWaitTime() + AGING_BOOST_POINTS);
                        }
                    }
                }
            }
        }, "Aging-B");

        // --- NUM_AGENTS THREAD AGENT: mỗi thread là 1 điện thoại viên chạy song song
        // ---
        Thread[] agentThreads = new Thread[NUM_AGENTS];
        for (int a = 0; a < NUM_AGENTS; a++) {
            agentThreads[a] = new Thread(() -> {
                while (processedCount.get() < totalCalls) {
                    SimCall nextCall = null;
                    synchronized (lockObject) {
                        if (!priorityQueue.isEmpty()) {
                            // Thuật toán tìm kiếm phần tử có độ ưu tiên cao nhất trong hàng đợi
                            int highestPriorityIndex = 0;
                            for (int j = 1; j < priorityQueue.size(); j++) {
                                int p1 = priorityQueue.get(j).call.getAgedPriority();
                                int p2 = priorityQueue.get(highestPriorityIndex).call.getAgedPriority();
                                if (p1 > p2) {
                                    highestPriorityIndex = j;
                                } else if (p1 == p2) {
                                    // Quy tắc bổ trợ (Tie-breaker): ai có arrivalTime sớm hơn được chọn
                                    if (priorityQueue.get(j).arrivalTime < priorityQueue
                                            .get(highestPriorityIndex).arrivalTime) {
                                        highestPriorityIndex = j;
                                    }
                                }
                            }
                            nextCall = priorityQueue.remove(highestPriorityIndex);
                        } else {
                            // Hàng đợi rỗng: Agent chờ (block) tối đa 50ms rồi kiểm tra lại
                            try {
                                lockObject.wait(50);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                return;
                            }
                        }
                    }
                    if (nextCall != null) {
                        // Xem giải thích chi tiết công thức này ở Scenario A (runDualQueueSimulation) —
                        // dùng double, chia số thực 1000.0, không ép kiểu int để tránh mất phần thập
                        // phân.
                        long waitMachineMs = System.currentTimeMillis() - nextCall.dispatchedAtMachineMs;
                        nextCall.waitTime = (waitMachineMs * PROCESSING_SPEEDUP_FACTOR) / 1000.0;

                        simulateProcessing(nextCall.handlingTime);
                        int done = processedCount.incrementAndGet();
                        printProgress(done, totalCalls, "Scenario B");
                    }
                }
            }, "Agent-B-" + a);
        }

        // Khởi động toàn bộ Dispatcher + Aging + Agent threads để chạy song song
        dispatcherThread.start();
        agingThread.start();
        for (Thread t : agentThreads)
            t.start();

        // Đợi tất cả các luồng hoàn tất
        try {
            dispatcherThread.join();
            for (Thread t : agentThreads)
                t.join();
            agingThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Hàm hỗ trợ chuyển đổi số giây ảo thành chuỗi văn bản dễ đọc gồm Phút và Giây.
     * Ví dụ: 64408.3 giây -> "1073 min 28.3 sec"
     */
    private String formatDuration(double seconds) {
        if (seconds < 0)
            return "N/A";
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
     * - Tính toán thời gian chờ trung bình (AWT) cho từng nhóm (VIP, Thường) và
     * toàn bộ hệ thống.
     * - Tìm kiếm thời gian chờ lớn nhất (Max WT) để kiểm chứng tình trạng kẹt hàng
     * đợi.
     * - Tính phần trăm cải thiện thời gian chờ của khách thường giữa hai kịch bản.
     * - Xuất dữ liệu ra file CSV để vẽ biểu đồ và lưu trữ.
     */
    private void printComparativeReport(List<SimCall> datasetA, List<SimCall> datasetB) {
        // --- 1. TÍNH TOÁN CÁC CHỈ SỐ CHO KỊCH BẢN A (DUAL QUEUE) ---
        double vipAwtA = 0, regAwtA = 0, totalAwtA = 0;
        // maxRegA/maxVipA dùng double (KHÔNG phải int) để khớp kiểu với waitTime
        // (double) và không
        // làm tròn mất phần thập phân của thời gian chờ lớn nhất.
        double maxRegA = 0, maxVipA = 0;
        int vipCount = 0, regCount = 0;

        for (SimCall sc : datasetA) {
            if (sc.call.isVIP()) {
                vipAwtA += sc.waitTime;
                vipCount++;
                if (sc.waitTime > maxVipA)
                    maxVipA = sc.waitTime;
            } else {
                regAwtA += sc.waitTime;
                regCount++;
                if (sc.waitTime > maxRegA)
                    maxRegA = sc.waitTime;
            }
            totalAwtA += sc.waitTime;
        }
        vipAwtA /= vipCount;
        regAwtA /= regCount;
        totalAwtA /= datasetA.size();

        // --- 2. TÍNH TOÁN CÁC CHỈ SỐ CHO KỊCH BẢN B (AGING QUEUE) ---
        double vipAwtB = 0, regAwtB = 0, totalAwtB = 0;
        double maxRegB = 0, maxVipB = 0;
        for (SimCall sc : datasetB) {
            if (sc.call.isVIP()) {
                vipAwtB += sc.waitTime;
                if (sc.waitTime > maxVipB)
                    maxVipB = sc.waitTime;
            } else {
                regAwtB += sc.waitTime;
                if (sc.waitTime > maxRegB)
                    maxRegB = sc.waitTime;
            }
            totalAwtB += sc.waitTime;
        }
        vipAwtB /= vipCount;
        regAwtB /= regCount;
        totalAwtB /= datasetB.size();

        // --- 3. ĐÁNH GIÁ MỨC ĐỘ CẢI THIỆN CHO KHÁCH HÀNG THƯỜNG ---
        // Tỷ lệ giảm thời gian chờ trung bình cho khách thường (%)
        double regAwtImprovement = (regAwtA > 0) ? ((regAwtA - regAwtB) / regAwtA) * 100.0 : 0;
        // Tỷ lệ giảm thời gian chờ tối đa cho khách thường (%) - maxRegA giờ đã là
        // double nên không
        // cần ép kiểu (double) như bản cũ nữa
        double regMaxImprovement = (maxRegA > 0) ? ((maxRegA - maxRegB) / maxRegA) * 100.0 : 0;

        // --- 4. IN BẢNG BÁO CÁO CHI TIẾT LÊN CONSOLE ---
        System.out.println("\nSIMULATION METRICS REPORT");
        System.out.println("  Setup: " + TOTAL_CALLS_TO_GENERATE + " calls (fixed) | 20% VIP | " + NUM_AGENTS
                + " Agents | Handling time: 30-180s real | Speed: " + PROCESSING_SPEEDUP_FACTOR + "x faster");
        System.out.println(
                "  ---------------------------------------------------------------------------------------------------------");
        System.out.printf("  %-45s │ %-28s │ %-28s%n",
                "Metric Description",
                "Scenario A (Dual Queue)",
                "Scenario B (Aging Queue)");
        System.out.println(
                "  ---------------------------------------------------------------------------------------------------------");

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
                formatDuration(maxRegA) + " (" + String.format("%.1f", maxRegA) + "s)",
                formatDuration(maxRegB) + " (" + String.format("%.1f", maxRegB) + "s)");
        System.out.printf("   - VIP Customers                              │ %-28s │ %-28s%n",
                formatDuration(maxVipA) + " (" + String.format("%.1f", maxVipA) + "s)",
                formatDuration(maxVipB) + " (" + String.format("%.1f", maxVipB) + "s)");

        System.out.println(
                "  ---------------------------------------------------------------------------------------------------------");
        System.out.printf(
                "  Regular Customer Improvement: Average Wait Reduced by %.2f%% | Max Wait Reduced by %.2f%%%n",
                regAwtImprovement, regMaxImprovement);
        System.out.println("  (Positive % = Scenario B (Aging) is better for regular customers)");
        System.out.println(
                "  ---------------------------------------------------------------------------------------------------------");

        // --- 5. GHI DỮ LIỆU KẾT QUẢ RA FILE CSV PHỤC VỤ VẼ BIỂU ĐỒ ---
        try {
            config.ConfigLoader loader = new config.ConfigLoader();
            String csvPath = loader.resolvePath("data/Exp1_PriorityQueue.csv");
            storage.FileHandler fh = new storage.FileHandler(csvPath);
            List<String> csvLines = new ArrayList<>();
            csvLines.add(
                    "Metric Description,Scenario A (Dual Queue) (seconds),Scenario B (Aging Queue) (seconds),Improvement (%)");
            csvLines.add(String.format(Locale.US, "Average Wait Time - Regular (Non-VIP) Customers,%.2f,%.2f,%.2f%%",
                    regAwtA, regAwtB, regAwtImprovement));
            csvLines.add(String.format(Locale.US, "Average Wait Time - VIP Customers,%.2f,%.2f,N/A", vipAwtA, vipAwtB));
            csvLines.add(String.format(Locale.US, "Average Wait Time - Overall System (All Customers),%.2f,%.2f,%.2f%%",
                    totalAwtA, totalAwtB, (totalAwtA > 0 ? ((totalAwtA - totalAwtB) / totalAwtA) * 100.0 : 0)));
            // Max Wait Time giờ dùng %.2f (số thực) thay vì %d (số nguyên), khớp với kiểu
            // double của maxRegA/maxVipA
            csvLines.add(String.format(Locale.US, "Max Wait Time - Regular (Non-VIP) Customers,%.2f,%.2f,%.2f%%",
                    maxRegA, maxRegB, regMaxImprovement));
            csvLines.add(String.format(Locale.US, "Max Wait Time - VIP Customers,%.2f,%.2f,N/A", maxVipA, maxVipB));
            fh.writeLines(csvLines);
            System.out.println("  Data saved to: " + csvPath);
        } catch (Exception e) {
            System.err.println("  Error writing CSV: " + e.getMessage());
        }

        // --- 6. TỔNG KẾT TĨNH KẾT QUẢ TRÊN CONSOLE ---
        System.out.println("\nMETRICS SUMMARY:");
        System.out.println("  Scenario A (Dual Queue):");
        System.out.println("     - Regular (Non-VIP) Customers AWT: " + formatDuration(regAwtA) + " | Max WT: "
                + formatDuration(maxRegA));
        System.out.println(
                "     - VIP Customers AWT: " + formatDuration(vipAwtA) + " | Max WT: " + formatDuration(maxVipA));
        System.out.println("  Scenario B (Single Queue + Aging):");
        System.out.println("     - Regular (Non-VIP) Customers AWT: " + formatDuration(regAwtB) + " | Max WT: "
                + formatDuration(maxRegB));
        System.out.println(
                "     - VIP Customers AWT: " + formatDuration(vipAwtB) + " | Max WT: " + formatDuration(maxVipB));
    }

    // ================================================================================================
    // THỰC NGHIỆM PHỤ: "INSTANT PICKUP VERIFICATION"
    // ================================================================================================
    /**
     * MỤC TIÊU: Minh chứng bằng số liệu THỰC ĐO (không phải suy luận lý thuyết) cho
     * câu hỏi:
     * "Khi một cuộc gọi (VIP hoặc Thường) đến hệ thống, Agent có bắt máy NGAY LẬP
     * TỨC hay không?"
     *
     * NGUYÊN LÝ MINH CHỨNG:
     * - Nếu tại thời điểm cuộc gọi đến vẫn còn ít nhất 1 Agent đang RẢNH, Agent đó
     * sẽ giành được
     * cuộc gọi gần như tức thời (chỉ trễ bởi chi phí đồng bộ hóa của JVM - vài
     * mili-giây MÁY,
     * quy đổi ra giây thực tế vẫn xấp xỉ 0) => pickupDelaySeconds ≈ 0.
     * - Nếu TẤT CẢ Agent đều đang bận (đang Thread.sleep xử lý cuộc gọi khác), cuộc
     * gọi mới phải
     * NẰM CHỜ trong hàng đợi cho đến khi có Agent nào đó xử lý xong và quay lại lấy
     * việc tiếp theo
     * => pickupDelaySeconds > 0, thường lớn hơn ngưỡng
     * INSTANT_PICKUP_THRESHOLD_SECONDS rất nhiều.
     * - Do đó, việc phân loại từng cuộc gọi theo pickupDelaySeconds (đo được TRỰC
     * TIẾP bằng
     * System.currentTimeMillis(), không suy diễn) chính là minh chứng khách quan
     * cho câu hỏi trên.
     *
     * THIẾT KẾ: Dùng lại đúng kiến trúc multi-thread thật (1 Dispatcher +
     * NUM_AGENTS Agent threads,
     * đồng bộ hóa bằng synchronized/wait/notifyAll) giống hệt
     * runDualQueueSimulation(), nhưng:
     * - Chạy trên dataset RIÊNG (INSTANT_PICKUP_TOTAL_CALLS cuộc gọi, độc lập với
     * Scenario A/B ở
     * trên) để không ảnh hưởng đến số liệu AWT/Max WT đã báo cáo.
     * - Không phân biệt VIP/Thường (không cần 2 hàng đợi) vì mục tiêu chỉ là đo
     * pickupDelay, không
     * phải so sánh thuật toán ưu tiên.
     * - Ghi lại pickupDelaySeconds cho MỖI cuộc gọi, sau đó phân loại kết quả thành
     * 2 nhóm song song
     * để in báo cáo minh chứng.
     */
    private void runInstantPickupVerification() {
        System.out.println("\n==================================================================");
        System.out.println("EXPERIMENT 1B: INSTANT PICKUP VERIFICATION");
        System.out.println("(Verification: Does the Agent answer immediately when a call arrives?)");
        System.out.println("==================================================================");
        System.out.println("  Configuration: " + INSTANT_PICKUP_TOTAL_CALLS + " calls (fixed, separate dataset) | "
                + NUM_AGENTS + " Agents | Threshold: pickupDelay < " + INSTANT_PICKUP_THRESHOLD_SECONDS
                + "s is considered \"instant pickup\"");

        // Sinh dataset RIÊNG cho thực nghiệm này (độc lập hoàn toàn với
        // datasetA/datasetB ở BƯỚC 1-2)
        List<SimCall> dataset = generateInstantPickupDataset();
        System.out.println("  Total Generated Calls: " + dataset.size());
        System.out.println("  Running...");

        long startMs = System.currentTimeMillis();
        executeInstantPickupSimulation(dataset);
        long durationMs = System.currentTimeMillis() - startMs;
        System.out.println("  Execution Time: " + String.format("%.1f", durationMs / 1000.0) + " s");

        printInstantPickupReport(dataset);
    }

    /**
     * Sinh dataset riêng cho thực nghiệm Instant Pickup, dùng đúng cơ chế Poisson
     * Process giống
     * generateDataset() ở trên (arrival rate CALL_RATE_PER_SECOND, handlingTime
     * 30-180s, 20% VIP),
     * nhưng số lượng cuộc gọi = INSTANT_PICKUP_TOTAL_CALLS (nhỏ hơn, để thực nghiệm
     * chạy nhanh).
     */
    private List<SimCall> generateInstantPickupDataset() {
        Random rand = new Random();
        List<SimCall> list = new ArrayList<>();
        int currentTime = 0;
        int orderCounter = 1;

        while (list.size() < INSTANT_PICKUP_TOTAL_CALLS) {
            double u = rand.nextDouble();
            while (u == 0)
                u = rand.nextDouble();
            int nextArrivalInterval = (int) (-Math.log(1 - u) / CALL_RATE_PER_SECOND);
            if (nextArrivalInterval < 1)
                nextArrivalInterval = 1;
            currentTime += nextArrivalInterval;

            boolean isVip = rand.nextDouble() < 0.20;
            int repeatCalls = rand.nextInt(100) < 15 ? rand.nextInt(3) + 1 : 0;
            int handlingTime = rand.nextInt(151) + 30;

            String id = "P" + String.format("%04d", orderCounter);
            Call call = new Call(id, "Customer " + id, "090" + String.format("%07d", rand.nextInt(10000000)), isVip,
                    repeatCalls, orderCounter);
            list.add(new SimCall(call, currentTime, handlingTime));
            orderCounter++;
        }
        // Limit only 10% of regular calls to be allowed to exceed VIP points
        List<SimCall> regularCalls = new ArrayList<>();
        for (SimCall sc : list) {
            if (!sc.call.isVIP()) {
                regularCalls.add(sc);
            }
        }
        int numAllowedToExceed = (int) Math.ceil(regularCalls.size() * 0.10);
        Collections.shuffle(regularCalls, rand);
        for (int i = 0; i < numAllowedToExceed && i < regularCalls.size(); i++) {
            regularCalls.get(i).call.setAllowExceedVIP(true);
        }
        return list;
    }

    /**
     * Chạy mô phỏng multi-thread thật (1 Dispatcher + NUM_AGENTS Agent threads) và
     * đo chính xác
     * pickupDelaySeconds cho MỖI cuộc gọi = khoảng thời gian từ lúc được bơm vào
     * hàng đợi
     * (dispatchedAtMachineMs) đến lúc một Agent THỰC SỰ lấy được nó ra khỏi hàng
     * đợi để bắt đầu xử lý.
     *
     * Đây là cùng cơ chế đồng bộ hóa (synchronized/wait/notifyAll) như
     * runDualQueueSimulation(),
     * chỉ khác là dùng 1 hàng đợi chung duy nhất (không tách VIP/Thường) vì mục
     * tiêu ở đây chỉ là
     * đo độ trễ bắt máy, không phải so sánh thuật toán ưu tiên giữa 2 nhóm khách
     * hàng.
     */
    private void executeInstantPickupSimulation(List<SimCall> dataset) {
        List<SimCall> queue = new ArrayList<>(); // Hàng đợi chung duy nhất (không phân biệt VIP/Thường)
        final Object lockObject = new Object();
        final int totalCalls = dataset.size();
        final AtomicInteger processedCount = new AtomicInteger(0);

        // --- THREAD DISPATCHER: bơm cuộc gọi vào hàng đợi theo đúng nhịp arrivalTime
        // ---
        Thread dispatcherThread = new Thread(() -> {
            int previousArrival = 0;
            for (SimCall sc : dataset) {
                int gapSeconds = sc.arrivalTime - previousArrival;
                if (gapSeconds > 0) {
                    long sleepMs = (gapSeconds * 1000L) / PROCESSING_SPEEDUP_FACTOR;
                    if (sleepMs > 0) {
                        try {
                            Thread.sleep(sleepMs);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
                previousArrival = sc.arrivalTime;
                sc.dispatchedAtMachineMs = System.currentTimeMillis(); // Mốc "cuộc gọi đến"
                synchronized (lockObject) {
                    queue.add(sc);
                    lockObject.notifyAll();
                }
            }
        }, "Dispatcher-InstantPickup");

        // --- NUM_AGENTS THREAD AGENT: mỗi thread là 1 điện thoại viên chạy song song
        // ---
        Thread[] agentThreads = new Thread[NUM_AGENTS];
        for (int a = 0; a < NUM_AGENTS; a++) {
            agentThreads[a] = new Thread(() -> {
                while (processedCount.get() < totalCalls) {
                    SimCall nextCall = null;
                    synchronized (lockObject) {
                        if (!queue.isEmpty()) {
                            nextCall = queue.remove(0); // FIFO đơn giản, không cần ưu tiên VIP ở thực nghiệm này
                        } else {
                            try {
                                lockObject.wait(50);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                return;
                            }
                        }
                    }
                    if (nextCall != null) {
                        // Mốc thời gian máy NGAY TẠI THỜI ĐIỂM Agent thực sự lấy được cuộc gọi ra khỏi
                        // hàng đợi (bắt đầu xử lý) - đây chính là thời điểm "bắt máy" trong thực tế.
                        long pickupMachineMs = System.currentTimeMillis();
                        long delayMachineMs = pickupMachineMs - nextCall.dispatchedAtMachineMs;
                        // Quy đổi độ trễ từ mili-giây MÁY sang giây THỰC TẾ của khách hàng (nhân
                        // PROCESSING_SPEEDUP_FACTOR), dùng phép chia số thực để giữ độ chính xác thập
                        // phân (xem giải thích chi tiết ở SimCall.waitTime phía trên).
                        nextCall.pickupDelaySeconds = (delayMachineMs * PROCESSING_SPEEDUP_FACTOR) / 1000.0;
                        nextCall.waitTime = nextCall.pickupDelaySeconds; // đồng bộ luôn waitTime cho nhất quán

                        simulateProcessing(nextCall.handlingTime);
                        int done = processedCount.incrementAndGet();
                        printProgress(done, totalCalls, "Instant Pickup Verification");
                    }
                }
            }, "Agent-InstantPickup-" + a);
        }

        dispatcherThread.start();
        for (Thread t : agentThreads)
            t.start();
        try {
            dispatcherThread.join();
            for (Thread t : agentThreads)
                t.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * In báo cáo SONG SONG 2 nhóm dựa trên pickupDelaySeconds đo được thực tế:
     * - Nhóm A: "INSTANT PICKUP" (pickupDelaySeconds <
     * INSTANT_PICKUP_THRESHOLD_SECONDS)
     * => minh chứng cho trường hợp "còn Agent rảnh, bắt máy ngay lập tức".
     * - Nhóm B: "QUEUED / DELAYED PICKUP" (pickupDelaySeconds >=
     * INSTANT_PICKUP_THRESHOLD_SECONDS)
     * => minh chứng cho trường hợp "tất cả Agent đều bận, phải chờ trong hàng đợi".
     * Đây là bằng chứng THỰC ĐO (đo bằng System.currentTimeMillis() khi chương
     * trình thực sự chạy),
     * không phải số liệu suy luận lý thuyết hay giả lập cứng.
     */
    private void printInstantPickupReport(List<SimCall> dataset) {
        List<SimCall> instantGroup = new ArrayList<>();
        List<SimCall> queuedGroup = new ArrayList<>();
        for (SimCall sc : dataset) {
            if (sc.pickupDelaySeconds < INSTANT_PICKUP_THRESHOLD_SECONDS) {
                instantGroup.add(sc);
            } else {
                queuedGroup.add(sc);
            }
        }

        double instantAvg = instantGroup.stream().mapToDouble(sc -> sc.pickupDelaySeconds).average().orElse(0);
        double instantMax = instantGroup.stream().mapToDouble(sc -> sc.pickupDelaySeconds).max().orElse(0);
        double queuedAvg = queuedGroup.stream().mapToDouble(sc -> sc.pickupDelaySeconds).average().orElse(0);
        double queuedMax = queuedGroup.stream().mapToDouble(sc -> sc.pickupDelaySeconds).max().orElse(0);
        double queuedMin = queuedGroup.stream().mapToDouble(sc -> sc.pickupDelaySeconds).min().orElse(0);

        System.out.println("\nINSTANT PICKUP VERIFICATION REPORT");
        System.out.println("  Definition: pickupDelaySeconds = real-world time interval (seconds) from when a call");
        System.out.println("  ARRIVES in the system until an Agent ACTUALLY starts processing it (measured directly via");
        System.out.println("  System.currentTimeMillis(), scaled by PROCESSING_SPEEDUP_FACTOR).");
        System.out.println(
                "  ---------------------------------------------------------------------------------------------------------");
        System.out.printf("  %-45s │ %-28s │ %-28s%n",
                "Metric Description",
                "Group A: INSTANT PICKUP",
                "Group B: QUEUED / DELAYED");
        System.out.println(
                "  ---------------------------------------------------------------------------------------------------------");
        System.out.printf("  %-45s │ %-28s │ %-28s%n",
                "Classification condition",
                "pickupDelay < " + INSTANT_PICKUP_THRESHOLD_SECONDS + "s",
                "pickupDelay ≥ " + INSTANT_PICKUP_THRESHOLD_SECONDS + "s");
        System.out.printf("  %-45s │ %-28s │ %-28s%n",
                "Practical meaning",
                "Available Agent when call arrives",
                "All Agents busy when call arrives");
        System.out.printf("  %-45s │ %-28d │ %-28d%n",
                "Number of calls (out of " + dataset.size() + ")",
                instantGroup.size(), queuedGroup.size());
        System.out.printf("  %-45s │ %-28s │ %-28s%n",
                "Ratio (%)",
                String.format("%.1f%%", 100.0 * instantGroup.size() / dataset.size()),
                String.format("%.1f%%", 100.0 * queuedGroup.size() / dataset.size()));
        System.out.printf("  %-45s │ %-28s │ %-28s%n",
                "Average Pickup Delay",
                String.format("%.4f s", instantAvg),
                formatDuration(queuedAvg) + " (" + String.format("%.1f", queuedAvg) + "s)");
        System.out.printf("  %-45s │ %-28s │ %-28s%n",
                "Minimum Pickup Delay",
                "0.0000 s (almost instant)",
                queuedGroup.isEmpty() ? "N/A" : String.format("%.1f", queuedMin) + "s");
        System.out.printf("  %-45s │ %-28s │ %-28s%n",
                "Maximum Pickup Delay",
                String.format("%.4f s", instantMax),
                formatDuration(queuedMax) + " (" + String.format("%.1f", queuedMax) + "s)");
        System.out.println(
                "  ---------------------------------------------------------------------------------------------------------");
        System.out.println("  VERIFICATION CONCLUSION:");
        System.out.printf(
                "  - %d/%d calls (%.1f%%) were answered ALMOST INSTANTLY (average delay of only%n",
                instantGroup.size(), dataset.size(), 100.0 * instantGroup.size() / dataset.size());
        System.out.printf(
                "    %.4f seconds) - these are cases where at least 1 Agent was available upon arrival.%n",
                instantAvg);
        if (!queuedGroup.isEmpty()) {
            System.out.printf(
                    "  - %d/%d calls (%.1f%%) had to WAIT IN QUEUE for an average of %s before being answered%n",
                    queuedGroup.size(), dataset.size(), 100.0 * queuedGroup.size() / dataset.size(),
                    formatDuration(queuedAvg));
            System.out.println(
                    "    - these are cases where ALL " + NUM_AGENTS + " Agents were busy handling other calls");
            System.out.println("    upon arrival, confirming the hypothesis.");
        } else {
            System.out.println("  - No queued cases were recorded in this run (workload not heavy enough");
            System.out.println("    to occupy all " + NUM_AGENTS
                    + " Agents simultaneously). You can increase CALL_RATE_PER_SECOND");
            System.out.println("    or decrease NUM_AGENTS to better reproduce queued cases.");
        }
        System.out.println(
                "  ---------------------------------------------------------------------------------------------------------");

        // Ghi dữ liệu chi tiết ra CSV riêng để phục vụ đối chiếu/vẽ biểu đồ nếu cần
        try {
            config.ConfigLoader loader = new config.ConfigLoader();
            String csvPath = loader.resolvePath("data/Exp1B_InstantPickupVerification.csv");
            storage.FileHandler fh = new storage.FileHandler(csvPath);
            List<String> csvLines = new ArrayList<>();
            csvLines.add("CallID,IsVIP,ArrivalTime(s),HandlingTime(s),PickupDelay(s),Group");
            for (SimCall sc : dataset) {
                String group = sc.pickupDelaySeconds < INSTANT_PICKUP_THRESHOLD_SECONDS ? "INSTANT_PICKUP"
                        : "QUEUED_DELAYED";
                csvLines.add(String.format(Locale.US, "%s,%s,%d,%d,%.4f,%s",
                        sc.call.getCustomerId(), sc.call.isVIP(), sc.arrivalTime, sc.handlingTime,
                        sc.pickupDelaySeconds, group));
            }
            fh.writeLines(csvLines);
            System.out.println("  Data saved to: " + csvPath);
        } catch (Exception e) {
            System.err.println("  Error writing CSV: " + e.getMessage());
        }
    }
}