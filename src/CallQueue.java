import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Lớp hàng đợi cuộc gọi (CallQueue) sử dụng PriorityQueue để tự động sắp xếp
 * cuộc gọi
 * dựa trên mức độ ưu tiên (VIP, số lần gọi lại) và thứ tự thời gian gọi vào
 * (FIFO).
 */
public class CallQueue {
    // PriorityQueue lưu trữ các cuộc gọi đang chờ phục vụ
    private final PriorityQueue<Call> waitingCalls;

    /**
     * Hàm khởi tạo thiết lập bộ so sánh (Comparator) để quyết định thứ tự ưu tiên:
     * 1. Cuộc gọi có điểm ưu tiên (priority point) cao hơn sẽ xếp trước.
     * 2. Nếu điểm ưu tiên bằng nhau, cuộc gọi nào gọi trước (orderNumber nhỏ hơn)
     * sẽ được phục vụ trước (FIFO).
     */
    public CallQueue() {
        waitingCalls = new PriorityQueue<>(new Comparator<Call>() {
            @Override
            public int compare(Call first, Call second) {
                // 1. So sánh theo điểm ưu tiên: Khách có điểm cao hơn được phục vụ trước (sắp
                // xếp giảm dần).
                // Quy tắc Java: Nếu hàm trả về số DƯƠNG (> 0), phần tử thứ hai (second) sẽ được
                // xếp TRƯỚC phần tử thứ nhất (first).
                // Vì vậy, ta lấy (Điểm của second - Điểm của first):
                // - Nếu second có điểm cao hơn (ví dụ: 100 - 0 = 100 > 0) -> second (khách VIP)
                // được đẩy lên trước.
                if (first.getPriorityPoint() != second.getPriorityPoint()) {
                    return second.getPriorityPoint() - first.getPriorityPoint();
                }

                // 2. Nếu điểm ưu tiên bằng nhau: Áp dụng FIFO, cuộc gọi nào vào hệ thống trước
                // được phục vụ trước (sắp xếp tăng dần theo số thứ tự).
                // Quy tắc Java: Nếu hàm trả về số ÂM (< 0), phần tử thứ nhất (first) sẽ được
                // giữ đứng TRƯỚC phần tử thứ hai (second).
                // Vì vậy, ta lấy (Số thứ tự của first - Số thứ tự của second):
                // - Nếu first vào trước (ví dụ: STT 1 - STT 2 = -1 < 0) -> first được giữ đứng
                // trước.
                return first.getOrderNumber() - second.getOrderNumber();
            }
        });
    }

    /**
     * Thêm một cuộc gọi mới vào hàng đợi. PriorityQueue sẽ tự động chèn vào đúng vị
     * trí ưu tiên.
     */
    public void addCall(Call call) {
        waitingCalls.add(call);
    }

    /**
     * Lấy ra và xóa cuộc gọi ở đầu hàng đợi (có độ ưu tiên cao nhất).
     * 
     * @return Đối tượng Call có độ ưu tiên cao nhất, hoặc null nếu hàng đợi trống
     */
    public Call getNextCall() {
        return waitingCalls.poll(); // Trả về và loại bỏ cuộc gọi ưu tiên cao nhất
    }

    /**
     * Kiểm tra hàng đợi có trống hay không.
     */
    public boolean isEmpty() {
        return waitingCalls.isEmpty();
    }

    /**
     * Hiển thị danh sách các cuộc gọi đang chờ theo đúng thứ tự ưu tiên hiện tại.
     */
    public void showWaitingCalls() {
        if (waitingCalls.isEmpty()) {
            System.out.println("No waiting calls.");
            return;
        }

        // Tạo một danh sách tạm thời từ PriorityQueue để sắp xếp và hiển thị mà không
        // ảnh hưởng tới hàng đợi gốc
        List<Call> sortedCalls = new ArrayList<>(waitingCalls);
        sortedCalls.sort(waitingCalls.comparator()); // Sử dụng bộ so sánh của PriorityQueue để sắp xếp danh sách

        System.out.println("\nWaiting calls:");
        for (int i = 0; i < sortedCalls.size(); i++) {
            System.out.println((i + 1) + ". " + sortedCalls.get(i));
        }
    }
}
