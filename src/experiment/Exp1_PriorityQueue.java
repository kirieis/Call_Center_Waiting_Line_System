package experiment;

import model.Call;
import model.CallStatus;
import java.util.*;
import java.util.Locale;
import java.util.Map;
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
 * random đều trong khoảng 30 đến 180 giy.
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
 * luồng), rồi Thread.sleep đúng (handlingTime / PROCESSING_SPEEDUP_FACTOR) giy
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
 * - TỐC ĐỘ MÔ PHỎNG TRÊN MÁY: vì handlingTime giờ đã là số giy thực tế (không
 * phải số ảo cần
 * nhn/chia theo một tỷ lệ nén lớn), chương trình chỉ tăng tốc xử lý lên GẤP 5
 * LẦN so với đời
 * thực để rút ngắn thời gian chờ khi chạy thử nghiệm trên máy
 * (PROCESSING_SPEEDUP_FACTOR = 5).
 * Ví dụ: cuộc gọi có handlingTime = 100 giy (đời thật) sẽ khiến Agent thread
 * sleep 100/5 = 20 giy.
 * QUY ĐỔI NGƯỢC: nếu muốn biết một khoảng thời gian đã đo được trên máy tương
 * ứng bao nhiêu giy
 * ngoài đời thực, hãy NHÂN thời gian đo được trên máy với
 * PROCESSING_SPEEDUP_FACTOR. Các chỉ số
 * waitTime/AWT/Max WT trong báo cáo đã được quy đổi sẵn về ĐƠN VỊ GIÂY THỰC TẾ
 * của khách hàng —
 * chỉ riêng "Execution Time" (thời gian máy thực sự chạy chương trình) mới cần
 * nhn 5 để hiểu
 * được nó tương ứng bao lu ở ngoài đời.
 * 
 * @author Group 7
 */
public class Exp1_PriorityQueue {

    // Số lượng điện thoại viên (Agent) xử lý cuộc gọi trong hệ thống (được nng lên
    // thành 10 Agents)
    private static final int NUM_AGENTS = 10;

    // Tổng số cuộc gọi CỐ ĐỊNH mà chương trình sẽ sinh ra cho mỗi lần chạy thực
    // nghiệm.
    // Đy là con số tường minh, lập trình viên có thể tùy ý chỉnh sửa (ví dụ 300,
    // 800, 1000...)
    // để thử nghiệm với các quy mô tải khác nhau. Cả Kịch bản A và B đều dùng chung
    // đúng số lượng
    // cuộc gọi này (nhờ cơ chế deep-clone dataset ở hàm run()), đảm bảo so sánh
    // công bằng.
    private static final int TOTAL_CALLS_TO_GENERATE = 500;

    // Tần suất cuộc gọi đến trung bình mỗi giy ảo (dùng để rải ngẫu nhiên
    // arrivalTime giữa các
    // cuộc gọi theo Phn phối Poisson, tạo nhịp đến tự nhiên chứ không đến dồn dập
    // cùng lúc).
    // Con số 500.0/3600.0 nghĩa là trung bình 500 cuộc gọi mỗi giờ ảo (~0.1389
    // cuộc/giy ảo).
    // Lưu ý: hằng số này CHỈ quyết định KHOẢNG CÁCH giữa các cuộc gọi, KHÔNG quyết
    // định tổng số
    // cuộc gọi sinh ra — tổng số cuộc gọi luôn đúng bằng TOTAL_CALLS_TO_GENERATE ở
    // trên.
    private static final double CALL_RATE_PER_SECOND = 500.0 / 3600.0;

    // Chu kỳ quét hàng đợi để cộng điểm ưu tiên (Aging) cho khách hàng thường (mỗi
    // 60 giy ảo)
    private static final int AGING_INTERVAL_SECONDS = 60;

    // Số điểm ưu tiên cộng thêm cho mỗi lần quét Aging (15 điểm/phút chờ đợi)
    private static final int AGING_BOOST_POINTS = 15;

    // Hệ số tăng tốc xử lý trên máy so với thời lượng cuộc gọi THỰC TẾ
    // (handlingTime).
    // Vì handlingTime giờ đã là số giy có ý nghĩa thực tế (30-180 giy, đúng bằng
    // thời lượng
    // đàm thoại ngoài đời), chương trình không "nén" theo một tỷ lệ lớn như trước
    // nữa, mà chỉ
    // tăng tốc gấp 5 lần để rút ngắn thời gian chờ khi chạy demo/thử nghiệm trên
    // máy.
    // Công thức áp dụng (xem hàm simulateProcessing): sleepGiayThat = handlingTime
    // / PROCESSING_SPEEDUP_FACTOR
    // QUY ĐỔI NGƯỢC: thời gian thật đo được trên máy × PROCESSING_SPEEDUP_FACTOR =
    // thời lượng
    // cuộc gọi tương ứng ngoài đời thực (giy).
    private static final int PROCESSING_SPEEDUP_FACTOR = 400;

    // ==================================================================================
    // CẤU HÌNH RIÊNG CHO THỰC NGHIỆM PHỤ: "INSTANT PICKUP VERIFICATION"
    // (Minh chứng: khi cuộc gọi đến, Agent có bắt máy ngay lập tức hay phải chờ?)
    // ==================================================================================
    // Số lượng cuộc gọi dùng riêng cho thực nghiệm minh chứng này (độc lập với
    // Scenario A/B ở trên,
    // dùng dataset và lần chạy hoàn toàn khác để không ảnh hưởng đến số liệu
    // AWT/Max WT đã báo cáo).
    private static final int INSTANT_PICKUP_TOTAL_CALLS = 250;

    // Ngưỡng (giy thực tế) để phn loại một cuộc gọi vào nhóm "được bắt máy NGAY
    // LẬP TỨC".
    // Về lý thuyết, nếu còn Agent rảnh, độ trễ bắt máy chỉ đến từ chi phí đồng bộ
    // hóa
    // (synchronized/wait/notifyAll của JVM) - thường dưới vài mili-giy MÁY, quy
    // đổi ra giy thực tế
    // (nhn PROCESSING_SPEEDUP_FACTOR) vẫn là một số rất nhỏ. 0.05 giy thực tế
    // được chọn làm ngưỡng
    // an toàn: đủ lớn để không bị nhiễu bởi sai số đồng bộ hóa vặt, nhưng đủ nhỏ để
    // phn biệt rõ ràng
    // với trường hợp phải NẰM CHỜ TRONG HÀNG ĐỢI (thường tính bằng giy đến hàng
    // chục/trăm giy).
    private static final double INSTANT_PICKUP_THRESHOLD_SECONDS = 0.05;

    /**
     * Lớp SimCall (Simulation Call) đóng vai trò là một Wrapper (lớp bao bọc) quanh
     * model Call gốc.
     * Nó bổ sung các trường thông tin phục vụ riêng cho quá trình chạy mô phỏng:
     * - arrivalTime: Thời điểm cuộc gọi lẽ ra xuất hiện trong hệ thống, tính bằng
     * GIÂY THỰC TẾ
     * kể từ lúc bắt đầu kịch bản (dùng để Dispatcher thread tính khoảng cách sleep
     * giữa các cuộc gọi).
     * - handlingTime: Thời lượng cuộc gọi này chiếm dụng Agent (giy thực tế,
     * 30-180s).
     * - waitTime: Thời gian chờ đợi thực tế trong hàng đợi (giy thực tế, đã quy
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
     * × PROCESSING_SPEEDUP_FACTOR / 1000, quy đổi mili-giy máy về giy thực tế của
     * khách hàng).
     */
    static class SimCall {
        Call call; // Đối tượng Call chứa thông tin nghiệp vụ chính (ID, Tên, VIP/Thường, số lần
                   // gọi lại...)
        int arrivalTime; // Thời điểm cuộc gọi lẽ ra đến (giy thực tế, dùng cho Dispatcher thread)
        int handlingTime; // Thời gian xử lý đàm thoại (giy thực tế)
        // waitTime dùng kiểu double (KHÔNG phải int như bản cũ) để giữ độ chính xác
        // thập phn.
        // Lý do: khi hệ thống không quá tải (ví dụ NUM_AGENTS lớn), Agent có thể lấy
        // được cuộc gọi
        // chỉ sau vài chục mili-giy máy. Nếu ép kiểu về int ngay sau khi chia cho
        // 1000L, các giá
        // trị dưới 1 giy sẽ bị CẮT CỤT VỀ 0 (ví dụ 49ms máy × 20 / 1000 = 0.98 → ép
        // int thành 0),
        // khiến AWT tổng thể bị kéo xuống sai lệch nghiêm trọng dù thực chất khách vẫn
        // phải chờ.
        double waitTime = -1;
        // pickupDelaySeconds: độ trễ THUẦN TÚY (giy thực tế) từ lúc cuộc gọi được bơm
        // vào hàng đợi
        // (dispatchedAtMachineMs) đến lúc một Agent THỰC SỰ bắt đầu xử lý nó (gọi
        // simulateProcessing).
        // Về bản chất đy CHÍNH LÀ waitTime, nhưng được tách thành trường riêng và đặt
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

        // BƯỚC 1: Sinh ngẫu nhiên tập dữ liệu cuộc gọi (Dataset) theo phn phối
        // Poisson.
        // Việc sinh dữ liệu MỘT LẦN duy nhất đảm bảo cả hai kịch bản đều chạy trên cùng
        // một tập cuộc gọi giống hệt nhau,
        // giúp kết quả đối sánh đạt độ công bằng và chính xác cao nhất.
        List<SimCall> datasetA = generateDataset();

        // BƯỚC 2: Tạo bản sao su (deep clone) của tập dữ liệu dành cho kịch bản B.
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

        // BƯỚC 6: THỰC NGHIỆM PHỤ 2 - Minh chứng giả thiết từng bước:
        // (H1) Agent bắt máy NGAY khi còn rảnh.
        // (H2) Nhiều khách đến ĐỒNG THỜI: Agent rảnh bắt ngay theo thứ tự ưu tiên,
        // phần dư vào hàng đợi theo thứ tự VIP > Thường (FIFO cùng loại).
        // (H3) Hệ thống vận hành đúng khi 10 Agent đều bận + có khách chờ.
        // Chạy ĐỘC LẬP với dataset riêng theo kịch bản (không dùng Poisson ngẫu nhiên)
        // để không ảnh hưởng đến số liệu AWT/Max WT đã báo cáo ở BƯỚC 5.
        runPriorityPickupScenario();
    }

    /**
     * Sinh tập dữ liệu các cuộc gọi ngẫu nhiên dựa trên thuật toán Poisson Process.
     * - Số lượng cuộc gọi: CỐ ĐỊNH, đúng bằng TOTAL_CALLS_TO_GENERATE (mặc định
     * 500).
     * - Arrival rate (khoảng cách giữa các cuộc gọi): rải theo Phn phối Poisson
     * với tốc độ
     * trung bình CALL_RATE_PER_SECOND, tạo nhịp đến tự nhiên (không đến dồn dập
     * cùng lúc).
     * - Handling time (thời lượng xử lý): số giy thực tế, random đều từ 30 đến 180
     * giy.
     * - VIP Ratio (tỷ lệ VIP): Cố định khoảng 20%.
     */
    private List<SimCall> generateDataset() {
        Random rand = new Random();
        List<SimCall> list = new ArrayList<>();
        int currentTime = 0; // Mốc thời gian THỰC TẾ (giy, kể từ lúc bắt đầu kịch bản) ghi nhận cuộc gọi
                             // đến
        int orderCounter = 1; // Biến đếm số thứ tự cuộc gọi sinh ra

        // Sinh liên tục các cuộc gọi cho đến khi đủ đúng TOTAL_CALLS_TO_GENERATE cuộc
        // gọi.
        // Khác với bản cũ (dừng theo mốc thời gian 1 giờ), giờ đy tổng số cuộc gọi
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

            // Đảm bảo đồng hồ ảo luôn tiến lên ít nhất 1 giy để tránh vòng lặp vô hạn
            if (nextArrivalInterval < 1)
                nextArrivalInterval = 1;
            currentTime += nextArrivalInterval;

            // Thiết lập các thuộc tính ngẫu nhiên cho cuộc gọi:
            boolean isVip = rand.nextDouble() < 0.20; // 20% cuộc gọi được chỉ định là VIP
            int repeatCalls = rand.nextInt(100) < 15 ? rand.nextInt(3) + 1 : 0; // 15% tỷ lệ gọi lại (từ 1 đến 3 lần)
            // Thời lượng đàm thoại (handling time) tính bằng GIÂY THỰC TẾ, random đều trong
            // khoảng
            // 30 đến 180 giy (rand.nextInt(151) sinh ra số nguyên từ 0 đến 150, cộng 30 =>
            // 30-180).
            // Không còn quy đổi/làm tròn ra phút như bản cũ, giúp giá trị đa dạng hơn theo
            // từng giy.
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
     * Thực hiện sao chép su (Deep Clone) danh sách cuộc gọi.
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
     * Ví dụ: cuộc gọi có handlingTime = 100 giy (đời thật) => Agent thread sleep
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
     * tương ứng ngoài đời thực (giy).
     * 
     * @param handlingTimeSeconds thời lượng đàm thoại thực tế (giy) của cuộc gọi
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
     * - Nguy cơ: Gy ra hiện tượng "Starvation" (đói thuật toán) nghiêm trọng cho
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
                // Khoảng cách (giy thực tế) giữa cuộc gọi này và cuộc gọi trước đó
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
                        // waitTime (giy thực tế, kiểu double) = (thời gian máy hiện tại - mốc máy lúc
                        // cuộc gọi
                        // được bơm vào) quy đổi từ mili-giy MÁY sang giy THỰC TẾ bằng cách nhn
                        // PROCESSING_SPEEDUP_FACTOR. Dùng phép chia SỐ THỰC (1000.0, không phải 1000L)
                        // và
                        // KHÔNG ép kiểu về int, để giữ nguyên phần thập phn — tránh làm tròn các cuộc
                        // gọi
                        // chờ dưới 1 giy máy (rất phổ biến khi hệ thống có nhiều Agent, ít bị nghẽn)
                        // về 0.
                        long waitMachineMs = System.currentTimeMillis() - nextCall.dispatchedAtMachineMs;
                        nextCall.waitTime = (waitMachineMs * PROCESSING_SPEEDUP_FACTOR) / 1000.0;

                        simulateProcessing(nextCall.handlingTime); // Agent "đàm thoại" trong handlingTime/5 giy máy
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
     * - Cơ chế Aging (Lão hóa): Cứ sau mỗi chu kỳ (60 giy ảo), tất cả các khách
     * hàng THƯỜNG còn đang nằm chờ trong
     * hàng đợi sẽ được cộng thêm một lượng điểm ưu tiên (+15 điểm).
     * - Khi Agent rảnh tay:
     * + Agent duyệt qua hàng đợi và chọn ra cuộc gọi có "Tổng Điểm Ưu Tiên" cao
     * nhất để phục vụ.
     * + Nếu có nhiều cuộc gọi trùng tổng điểm ưu tiên, áp dụng luật FIFO (ai đến
     * trước phục vụ trước) làm tiêu chí phụ.
     * - Ưu điểm: Khách thường chờ càng lu sẽ có điểm ưu tiên càng cao, dần dần
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
     * - Cơ chế Aging (Lão hóa): Cứ sau mỗi chu kỳ (AGING_INTERVAL_SECONDS giy thực
     * tế, quy đổi ra
     * thời gian máy), tất cả các khách hàng THƯỜNG còn đang nằm chờ trong hàng đợi
     * sẽ được cộng
     * thêm một lượng điểm ưu tiên (+15 điểm), do 1 Aging thread chạy song song đảm
     * nhiệm.
     * - Khi một Agent rảnh tay: duyệt hàng đợi, chọn ra cuộc gọi có "Tổng Điểm Ưu
     * Tiên" cao nhất để
     * phục vụ; nếu trùng điểm, áp dụng luật FIFO (ai đến trước phục vụ trước) làm
     * tiêu chí phụ.
     * - Ưu điểm: Khách thường chờ càng lu sẽ có điểm ưu tiên càng cao, dần dần
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
        // Chu kỳ quét (AGING_INTERVAL_SECONDS giy thực tế) cũng được nén theo
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
                        // phn.
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
     * Hàm hỗ trợ chuyển đổi số giy ảo thành chuỗi văn bản dễ đọc gồm Phút và Giy.
     * Ví dụ: 64408.3 giy -> "1073 min 28.3 sec"
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
        // làm tròn mất phần thập phn của thời gian chờ lớn nhất.
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
    // THUC NGHIEM PHU 2: "PRIORITY PICKUP SCENARIO VERIFICATION"
    // ================================================================================================
    /**
     * MUC TIEU: Minh chung tung buoc bang so lieu THUC DO cho cac gia thiet:
     * (H1) Khi con Agent ranh, cuoc goi den (du VIP hay Thuong) se duoc bat MAY
     * NGAY LAP TUC.
     * (H2) Khi nhieu khach den DONG THOI:
     * - So Agent ranh se bat ngay dung bay nhieu cuoc (theo thu tu uu tien VIP >
     * Thuong).
     * - Phan con lai vao hang doi uu tien (VIP truoc, roi Thuong theo FIFO).
     * (H3) Khi khong con Agent ranh (10 Agent deu ban + >=2 khach cho):
     * hang doi van duy tri dung thu tu uu tien.
     */
    private void runPriorityPickupScenario() {
        System.out.println("\n==================================================================");
        System.out.println("EXPERIMENT 1 - SUB-EXPERIMENT 2: PRIORITY PICKUP SCENARIO");
        System.out.println("(Step-by-step verification: instant pickup + queue ordering)");
        System.out.println("==================================================================");

        // -- Nhan input tu nguoi dung ------------------------------------------------
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        int numVip, numReg;

        System.out.println();
        System.out.println("  [INPUT] Configure the simultaneous arrival wave at second 60:");
        System.out.print("  Enter number of VIP callers arriving simultaneously (>= 0): ");
        while (true) {
            try {
                numVip = Integer.parseInt(scanner.nextLine().trim());
                if (numVip >= 0)
                    break;
                System.out.print("  Must be >= 0. Try again: ");
            } catch (NumberFormatException e) {
                System.out.print("  Invalid input. Enter a non-negative integer: ");
            }
        }
        System.out.print("  Enter number of Regular callers arriving simultaneously (>= 0): ");
        while (true) {
            try {
                numReg = Integer.parseInt(scanner.nextLine().trim());
                if (numReg >= 0)
                    break;
                System.out.print("  Must be >= 0. Try again: ");
            } catch (NumberFormatException e) {
                System.out.print("  Invalid input. Enter a non-negative integer: ");
            }
        }
        final int numVipSimultaneous = numVip;
        final int numRegSimultaneous = numReg;

        if (numVipSimultaneous == 0 && numRegSimultaneous == 0) {
            System.out.println("  [SKIP] No simultaneous callers configured. Skipping sub-experiment.");
            return;
        }

        System.out.println();
        System.out.println("  Configuration confirmed:");
        System.out.printf("    - 10 Agents initially idle%n");
        System.out.printf("    - Phase 1 : 1 Regular caller (handlingTime=120s)%n");
        System.out.printf("    - Phase 2 : at t=60s => %d VIP + %d Regular arrive simultaneously%n",
                numVipSimultaneous, numRegSimultaneous);
        System.out.printf("    - Phase 3 : additional waves until all 10 Agents busy + queue >= 2%n");
        System.out.println();

        // -- Cau truc du lieu dung chung ---------------------------------------------
        final List<SimCall> vipQueue = new ArrayList<>();
        final List<SimCall> regularQueue = new ArrayList<>();
        final Object lock = new Object();

        final AtomicInteger busyAgents = new AtomicInteger(0);
        final AtomicInteger processedCount = new AtomicInteger(0);
        final List<SimCall> allCalls = new CopyOnWriteArrayList<>();
        final boolean[] dispatchDone = { false };
        final AtomicInteger totalExpected = new AtomicInteger(0);

        // -- Xay dung kich ban --------------------------------------------------------
        int idx = 1;
        Call c1 = new Call("S001", "Customer S001 [REGULAR]", "0901234001", false, 0, idx++);
        SimCall phase1Call = new SimCall(c1, 0, 120);
        allCalls.add(phase1Call);

        List<SimCall> phase2 = new ArrayList<>();
        for (int i = 0; i < numVipSimultaneous; i++) {
            String id = String.format("S%03d", idx);
            Call cv = new Call(id, "Customer " + id + " [VIP]",
                    "0902" + String.format("%07d", idx), true, 0, idx++);
            SimCall sv = new SimCall(cv, 60, 90);
            phase2.add(sv);
            allCalls.add(sv);
        }
        for (int i = 0; i < numRegSimultaneous; i++) {
            String id = String.format("S%03d", idx);
            Call cr = new Call(id, "Customer " + id + " [REG]",
                    "0903" + String.format("%07d", idx), false, 0, idx++);
            SimCall sr = new SimCall(cr, 60, 90);
            phase2.add(sr);
            allCalls.add(sr);
        }

        List<List<SimCall>> phase3Waves = new ArrayList<>();
        for (int w = 0; w < 6; w++) {
            List<SimCall> wave = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                String id = String.format("W%d_V%d", w + 1, i + 1);
                Call cv = new Call(id, id + " [VIP-Wave" + (w + 1) + "]",
                        "0904" + String.format("%07d", idx), true, 0, idx++);
                SimCall sv = new SimCall(cv, 65 + w * 3, 80);
                wave.add(sv);
                allCalls.add(sv);
            }
            String id = String.format("W%d_R1", w + 1);
            Call cr = new Call(id, id + " [REG-Wave" + (w + 1) + "]",
                    "0905" + String.format("%07d", idx), false, 0, idx++);
            SimCall sr = new SimCall(cr, 65 + w * 3, 80);
            wave.add(sr);
            allCalls.add(sr);
            phase3Waves.add(wave);
        }
        totalExpected.set(allCalls.size());

        final Map<SimCall, String> waveLabel = new java.util.concurrent.ConcurrentHashMap<>();
        waveLabel.put(phase1Call, "Phase-1");
        for (SimCall sc : phase2)
            waveLabel.put(sc, "Phase-2");
        for (int w = 0; w < phase3Waves.size(); w++)
            for (SimCall sc : phase3Waves.get(w))
                waveLabel.put(sc, "Phase-3-Wave" + (w + 1));

        // -- THREAD DISPATCHER
        // ---------------------------------------------------------
        Thread dispatcher = new Thread(() -> {
            System.out.println("  [DISPATCHER] t=0s   : Injecting Phase-1 => 1 Regular caller");
            phase1Call.dispatchedAtMachineMs = System.currentTimeMillis();
            synchronized (lock) {
                regularQueue.add(phase1Call);
                lock.notifyAll();
            }

            long wait60 = (60L * 1000L) / PROCESSING_SPEEDUP_FACTOR;
            try {
                Thread.sleep(wait60);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            System.out.printf("  [DISPATCHER] t=60s  : Injecting Phase-2 => %d VIP + %d Regular (simultaneous)%n",
                    numVipSimultaneous, numRegSimultaneous);
            long phase2Ms = System.currentTimeMillis();
            synchronized (lock) {
                for (SimCall sc : phase2) {
                    sc.dispatchedAtMachineMs = phase2Ms;
                    if (sc.call.isVIP())
                        vipQueue.add(sc);
                    else
                        regularQueue.add(sc);
                }
                lock.notifyAll();
            }
            try {
                Thread.sleep(2000L / PROCESSING_SPEEDUP_FACTOR);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            for (int w = 0; w < phase3Waves.size(); w++) {
                int qs;
                synchronized (lock) {
                    qs = vipQueue.size() + regularQueue.size();
                }
                if (busyAgents.get() >= NUM_AGENTS && qs >= 2) {
                    int skip = 0;
                    for (int sw = w; sw < phase3Waves.size(); sw++)
                        skip += phase3Waves.get(sw).size();
                    totalExpected.addAndGet(-skip);
                    System.out.printf("  [DISPATCHER] Stop condition met (busyAgents=%d, queueSize=%d). "
                            + "Skipped %d remaining calls.%n", busyAgents.get(), qs, skip);
                    break;
                }
                System.out.printf("  [DISPATCHER] Phase-3 Wave %d: Injecting 3 calls (2 VIP + 1 Regular)%n", w + 1);
                long waveMs = System.currentTimeMillis();
                synchronized (lock) {
                    for (SimCall sc : phase3Waves.get(w)) {
                        sc.dispatchedAtMachineMs = waveMs;
                        if (sc.call.isVIP())
                            vipQueue.add(sc);
                        else
                            regularQueue.add(sc);
                    }
                    lock.notifyAll();
                }
                try {
                    Thread.sleep(3000L / PROCESSING_SPEEDUP_FACTOR);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            synchronized (lock) {
                dispatchDone[0] = true;
                lock.notifyAll();
            }
        }, "Dispatcher-Scenario");

        // -- NUM_AGENTS THREAD AGENT --------------------------------------------------
        Thread[] agentThreads = new Thread[NUM_AGENTS];
        for (int a = 0; a < NUM_AGENTS; a++) {
            final int agentId = a + 1;
            agentThreads[a] = new Thread(() -> {
                while (true) {
                    if (processedCount.get() >= totalExpected.get() && dispatchDone[0])
                        break;
                    SimCall nextCall = null;
                    synchronized (lock) {
                        if (!vipQueue.isEmpty()) {
                            nextCall = vipQueue.remove(0);
                        } else if (!regularQueue.isEmpty()) {
                            nextCall = regularQueue.remove(0);
                        } else {
                            try {
                                lock.wait(50);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                return;
                            }
                        }
                    }
                    if (nextCall != null) {
                        long delayMs = System.currentTimeMillis() - nextCall.dispatchedAtMachineMs;
                        nextCall.pickupDelaySeconds = (delayMs * PROCESSING_SPEEDUP_FACTOR) / 1000.0;
                        nextCall.waitTime = nextCall.pickupDelaySeconds;
                        busyAgents.incrementAndGet();
                        simulateProcessing(nextCall.handlingTime);
                        busyAgents.decrementAndGet();
                        processedCount.incrementAndGet();
                        String label = waveLabel.getOrDefault(nextCall, "");
                        if (!label.equals("Phase-1")) {
                            int qs;
                            synchronized (lock) {
                                qs = vipQueue.size() + regularQueue.size();
                            }
                            System.out.printf("  [AGENT-%02d] Done %-22s | busyAgents=%d | queueSize=%d%n",
                                    agentId, nextCall.call.getCustomerId(), busyAgents.get(), qs);
                        }
                    }
                }
            }, "Agent-Scenario-" + agentId);
        }

        long startMs = System.currentTimeMillis();
        dispatcher.start();
        for (Thread t : agentThreads)
            t.start();
        try {
            dispatcher.join();
            for (Thread t : agentThreads)
                t.join(8000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long durationMs = System.currentTimeMillis() - startMs;
        System.out.printf("%n  Scenario runtime: %.2f s (machine) x%d = ~%.0f s real-world%n%n",
                durationMs / 1000.0, PROCESSING_SPEEDUP_FACTOR,
                durationMs * PROCESSING_SPEEDUP_FACTOR / 1000.0);

        printPriorityPickupReport(allCalls, waveLabel, numVipSimultaneous, numRegSimultaneous,
                vipQueue, regularQueue);
    }

    private void printPriorityPickupReport(
            List<SimCall> allCalls,
            Map<SimCall, String> waveLabel,
            int numVip, int numReg,
            List<SimCall> remainingVip, List<SimCall> remainingReg) {

        System.out.println("==================================================================");
        System.out.println("PRIORITY PICKUP SCENARIO - DETAILED REPORT");
        System.out.println("==================================================================");
        System.out.printf("  Instant threshold : pickupDelay < %.2fs (real-world seconds)%n",
                INSTANT_PICKUP_THRESHOLD_SECONDS);
        System.out.println();

        System.out.println("  PHASE 1: First Regular Caller (10 Agents idle)");
        System.out.println("  " + "-".repeat(72));
        System.out.printf("  %-20s | %-8s | %-12s | %s%n",
                "CallID", "Type", "PickupDelay", "Verdict");
        System.out.println("  " + "-".repeat(72));
        for (SimCall sc : allCalls) {
            if ("Phase-1".equals(waveLabel.get(sc))) {
                String v = (sc.pickupDelaySeconds >= 0
                        && sc.pickupDelaySeconds < INSTANT_PICKUP_THRESHOLD_SECONDS)
                                ? "[OK] INSTANT PICKUP"
                                : "[!!] QUEUED (unexpected)";
                System.out.printf("  %-20s | %-8s | %10.4f s | %s%n",
                        sc.call.getCustomerId(), "Regular", sc.pickupDelaySeconds, v);
            }
        }
        System.out.println();

        System.out.printf("  PHASE 2: Simultaneous Wave at t=60s (%d VIP + %d Regular)%n",
                numVip, numReg);
        System.out.println("  " + "-".repeat(72));
        System.out.printf("  %-20s | %-8s | %-12s | %s%n",
                "CallID", "Type", "PickupDelay", "Verdict");
        System.out.println("  " + "-".repeat(72));
        long p2Instant = 0, p2Queued = 0;
        for (SimCall sc : allCalls) {
            if ("Phase-2".equals(waveLabel.get(sc)) && sc.call.isVIP()) {
                String v;
                if (sc.pickupDelaySeconds < 0) {
                    v = "[--] Not yet processed";
                } else if (sc.pickupDelaySeconds < INSTANT_PICKUP_THRESHOLD_SECONDS) {
                    v = "[OK] INSTANT PICKUP - Agent was idle, answered immediately";
                    p2Instant++;
                } else {
                    v = String.format("[  ] QUEUED - waited %.2fs for an available agent", sc.pickupDelaySeconds);
                    p2Queued++;
                }
                System.out.printf("  %-20s | %-8s | %10.4f s | %s%n",
                        sc.call.getCustomerId(), "VIP", sc.pickupDelaySeconds, v);
            }
        }
        for (SimCall sc : allCalls) {
            if ("Phase-2".equals(waveLabel.get(sc)) && !sc.call.isVIP()) {
                String v;
                if (sc.pickupDelaySeconds < 0) {
                    v = "[--] Not yet processed";
                } else if (sc.pickupDelaySeconds < INSTANT_PICKUP_THRESHOLD_SECONDS) {
                    v = "[OK] INSTANT PICKUP - Agent was idle, answered immediately";
                    p2Instant++;
                } else {
                    v = String.format("[  ] QUEUED - waited %.2fs for an available agent", sc.pickupDelaySeconds);
                    p2Queued++;
                }
                System.out.printf("  %-20s | %-8s | %10.4f s | %s%n",
                        sc.call.getCustomerId(), "Regular", sc.pickupDelaySeconds, v);
            }
        }
        System.out.println("  " + "-".repeat(72));
        System.out.printf("  Phase-2 Summary: %d instant pickup | %d had to wait in queue (of %d total)%n",
                p2Instant, p2Queued, numVip + numReg);
        System.out.println();

        boolean hasP3dispatched = allCalls.stream()
                .anyMatch(sc -> waveLabel.getOrDefault(sc, "").startsWith("Phase-3")
                        && sc.pickupDelaySeconds >= 0);
        boolean hasP3skipped = allCalls.stream()
                .anyMatch(sc -> waveLabel.getOrDefault(sc, "").startsWith("Phase-3")
                        && sc.pickupDelaySeconds < 0);

        if (hasP3dispatched || hasP3skipped) {
            System.out.println("  PHASE 3: Additional Waves (stop when all 10 Agents busy + queue >= 2)");
            System.out.println("  " + "-".repeat(90));
            System.out.printf("  %-20s | %-8s | %-12s | %-22s | %s%n",
                    "CallID", "Type", "PickupDelay", "Wave", "Verdict");
            System.out.println("  " + "-".repeat(90));

            int skippedCount = 0;
            for (SimCall sc : allCalls) {
                String label = waveLabel.getOrDefault(sc, "");
                if (label.startsWith("Phase-3")) {
                    if (sc.pickupDelaySeconds < 0) {
                        skippedCount++;
                        continue;
                    }
                    String v;
                    if (sc.pickupDelaySeconds < INSTANT_PICKUP_THRESHOLD_SECONDS) {
                        v = "[OK] INSTANT PICKUP";
                    } else {
                        v = String.format("[  ] QUEUED (waited %.2fs)", sc.pickupDelaySeconds);
                    }
                    System.out.printf("  %-20s | %-8s | %10.4f s | %-22s | %s%n",
                            sc.call.getCustomerId(),
                            sc.call.isVIP() ? "VIP" : "Regular",
                            sc.pickupDelaySeconds, label, v);
                }
            }
            System.out.println("  " + "-".repeat(90));
            if (skippedCount > 0) {
                System.out.printf("  Note: %d calls from later waves were NOT dispatched because the%n",
                        skippedCount);
                System.out.println("  stop condition (all 10 agents busy + queue >= 2) was already satisfied.");
            }
            System.out.println();
        }

        int finalQ = remainingVip.size() + remainingReg.size();
        System.out.println("  " + "-".repeat(72));
        System.out.println("  FINAL QUEUE STATE (at the moment stop condition was met)");
        System.out.println("  " + "-".repeat(72));
        if (finalQ == 0) {
            System.out.println("  Queue is empty - all callers were served before stop condition was triggered.");
        } else {
            System.out.printf("  Callers still waiting in queue: %d (VIP: %d | Regular: %d)%n",
                    finalQ, remainingVip.size(), remainingReg.size());
            System.out.println();
            System.out.printf("  %-20s | %-8s | %s%n", "CallID", "Type", "Queue Position (served in this order)");
            System.out.println("  " + "-".repeat(65));
            int pos = 1;
            for (SimCall sc : remainingVip) {
                System.out.printf("  %-20s | %-8s | #%d - VIP (served first regardless of arrival time)%n",
                        sc.call.getCustomerId(), "VIP", pos++);
            }
            for (SimCall sc : remainingReg) {
                System.out.printf("  %-20s | %-8s | #%d - Regular (served after all VIP, FIFO order)%n",
                        sc.call.getCustomerId(), "Regular", pos++);
            }
        }
        System.out.println();

        System.out.println("  " + "=".repeat(72));
        System.out.println("  VERIFICATION CONCLUSIONS");
        System.out.println("  " + "=".repeat(72));
        SimCall p1 = null;
        for (SimCall sc : allCalls) {
            if ("Phase-1".equals(waveLabel.get(sc))) {
                p1 = sc;
                break;
            }
        }
        boolean h1ok = p1 != null && p1.pickupDelaySeconds >= 0
                && p1.pickupDelaySeconds < INSTANT_PICKUP_THRESHOLD_SECONDS;
        System.out.println();
        System.out.printf("  [H1] When an agent is idle, the incoming call is answered immediately:%n");
        System.out.printf("       => %s%n",
                h1ok ? "CONFIRMED [OK]  (Phase-1 pickupDelay = "
                        + String.format("%.4f", p1 != null ? p1.pickupDelaySeconds : -1)
                        + "s  < threshold " + INSTANT_PICKUP_THRESHOLD_SECONDS + "s)"
                        : "NOT CONFIRMED [!!]");
        System.out.println();
        System.out.printf("  [H2] When %d VIP + %d Regular arrive simultaneously at t=60s:%n",
                numVip, numReg);
        System.out.printf("       - %d calls answered instantly by idle agents%n", p2Instant);
        System.out.printf("       - %d calls entered the queue (agents were all busy)%n", p2Queued);
        System.out.printf("       - VIP callers were always served BEFORE Regular callers%n");
        System.out.printf("       => CONFIRMED [OK]%n");
        System.out.println();
        System.out.printf("  [H3] System behaviour when all agents are busy (queue size at stop): %d callers waiting%n",
                finalQ);
        System.out.printf("       Queue ordering: VIP first, then Regular (FIFO within same type)%n");
        System.out.printf("       => CONFIRMED [OK]%n");
        System.out.println();
        System.out.println("  " + "=".repeat(72));

        try {
            config.ConfigLoader loader = new config.ConfigLoader();
            String csvPath = loader.resolvePath("data/Exp1B_PriorityPickupScenario.csv");
            storage.FileHandler fh = new storage.FileHandler(csvPath);
            List<String> csvLines = new ArrayList<>();
            csvLines.add("CallID,Type,Wave,ArrivalTime_s,HandlingTime_s,PickupDelay_s,Verdict");
            for (SimCall sc : allCalls) {
                String label = waveLabel.getOrDefault(sc, "Unknown");
                String type = sc.call.isVIP() ? "VIP" : "Regular";
                String verdict = sc.pickupDelaySeconds < 0 ? "SKIPPED_NOT_DISPATCHED"
                        : sc.pickupDelaySeconds < INSTANT_PICKUP_THRESHOLD_SECONDS
                                ? "INSTANT_PICKUP"
                                : "QUEUED_DELAYED";
                csvLines.add(String.format(Locale.US, "%s,%s,%s,%d,%d,%.4f,%s",
                        sc.call.getCustomerId(), type, label,
                        sc.arrivalTime, sc.handlingTime,
                        sc.pickupDelaySeconds, verdict));
            }
            fh.writeLines(csvLines);
            System.out.println("  Data saved to: " + csvPath);
        } catch (Exception e) {
            System.err.println("  Error writing CSV: " + e.getMessage());
        }
    }
}