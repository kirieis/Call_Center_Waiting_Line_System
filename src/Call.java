/**
 * Lớp đại diện cho một cuộc gọi của khách hàng (Call).
 * Chứa thông tin khách hàng và tính toán điểm ưu tiên (priority points) phục vụ.
 */
public class Call {
    private final String customerName; // Tên khách hàng
    private final String phoneNumber;  // Số điện thoại
    private final boolean vip;         // Trạng thái VIP (true: VIP, false: Thường)
    private final int repeatCalls;     // Số lần cuộc gọi lặp lại (gọi nhỡ/gọi lại trước đó)
    private final int orderNumber;     // Số thứ tự cuộc gọi vào hệ thống (dùng để phục vụ FIFO khi cùng độ ưu tiên)

    /**
     * Hàm khởi tạo (Constructor) tạo một cuộc gọi mới.
     */
    public Call(String customerName, String phoneNumber, boolean vip, int repeatCalls, int orderNumber) {
        this.customerName = customerName;
        this.phoneNumber = phoneNumber;
        this.vip = vip;
        this.repeatCalls = repeatCalls;
        this.orderNumber = orderNumber;
    }

    /**
     * Tính toán điểm ưu tiên của cuộc gọi.
     * Công thức:
     * - Khách VIP được cộng thêm 100 điểm.
     * - Mỗi lần gọi lại được cộng thêm 10 điểm (repeatCalls * 10).
     * @return Điểm ưu tiên của cuộc gọi dưới dạng số nguyên
     */
    public int getPriorityPoint() {
        int point = 0;

        if (vip) {
            point += 100; // Khách hàng VIP được ưu tiên hàng đầu
        }

        point += repeatCalls * 10; // Cộng điểm dựa trên số lần gọi nhỡ/gọi lại
        return point;
    }

    /**
     * Lấy số thứ tự cuộc gọi vào hệ thống.
     */
    public int getOrderNumber() {
        return orderNumber;
    }

    /**
     * Định dạng hiển thị thông tin cuộc gọi dưới dạng chuỗi ký tự.
     */
    @Override
    public String toString() {
        return customerName
                + " | Phone: " + phoneNumber
                + " | VIP: " + (vip ? "Yes" : "No")
                + " | Repeat calls: " + repeatCalls
                + " | Priority point: " + getPriorityPoint();
    }
}
