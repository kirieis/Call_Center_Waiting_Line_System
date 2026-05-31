/**
 * Lớp khởi chạy chương trình (Entry Point) của hệ thống hàng đợi cuộc gọi Call
 * Center.
 * Quản lý vòng lặp chương trình và menu tương tác với người dùng.
 */
public class Main {
    // Quản lý nghiệp vụ và xử lý logic liên quan đến cuộc gọi
    private static final CallManager callManager = new CallManager();

    private static void showMenu() {
        System.out.println("\n===== Call Center Waiting Line System =====");
        System.out.println("1. Add demo calls");
        System.out.println("2. Add new call");
        System.out.println("3. Serve next call");
        System.out.println("4. Show waiting calls");
        System.out.println("0. Exit");
    }

    public static void main(String[] args) {
        int choice;

        do {
            showMenu(); // Hiển thị menu chức năng
            choice = callManager.readInt("Choose: "); // Đọc lựa chọn từ người dùng

            switch (choice) {
                case 1:
                    // Thêm danh sách cuộc gọi mẫu để kiểm thử nhanh
                    callManager.addDemoCalls();
                    break;
                case 2:
                    // Nhập thông tin và thêm cuộc gọi mới vào hệ thống
                    callManager.addNewCall();
                    break;
                case 3:
                    // Tiến hành phục vụ cuộc gọi có độ ưu tiên cao nhất
                    callManager.serveNextCall();
                    break;
                case 4:
                    // Hiển thị toàn bộ danh sách cuộc gọi đang chờ phục vụ
                    callManager.showWaitingCalls();
                    break;
                case 0:
                    // Thông báo thoát ứng dụng
                    System.out.println("Exit program.");
                    break;
                default:
                    System.out.println("Invalid choice.");
                    break;
            }
        } while (choice != 0); // Vòng lặp tiếp tục cho tới khi người dùng chọn 0 để thoát
    }

    /**
     * Hiển thị danh sách menu chức năng ra màn hình console.
     */

}
