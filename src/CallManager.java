import java.util.Scanner;

/**
 * Lớp điều phối và quản lý toàn bộ các nghiệp vụ liên quan đến cuộc gọi (Call)
 * và hàng đợi cuộc gọi (CallQueue). Cũng chứa các phương thức nhập liệu từ Console.
 */
public class CallManager {
    // Đối tượng Scanner để đọc dữ liệu nhập từ bàn phím
    private final Scanner scanner = new Scanner(System.in);
    
    // Hàng đợi cuộc gọi (áp dụng cấu trúc PriorityQueue để ưu tiên khách hàng)
    private final CallQueue callQueue = new CallQueue();
    
    // Thứ tự cuộc gọi vào hệ thống (tăng dần tự động)
    private int orderNumber = 1;

    /**
     * Thêm danh sách các cuộc gọi mẫu (Demo) vào hàng đợi
     * nhằm hỗ trợ việc thử nghiệm nhanh chức năng phân loại ưu tiên.
     */
    public void addDemoCalls() {
        // Thêm 3 khách hàng mẫu với các thuộc tính khác nhau:
        // 1. Khách thường, không gọi lại nhiều lần
        callQueue.addCall(new Call("Normal Customer", "0901000001", false, 0, orderNumber++));
        // 2. Khách VIP (sẽ được cộng điểm ưu tiên cao)
        callQueue.addCall(new Call("VIP Customer", "0901000002", true, 0, orderNumber++));
        // 3. Khách gọi lại nhiều lần (cũng được ưu tiên dựa trên số lần gọi lại)
        callQueue.addCall(new Call("Repeat Customer", "0901000003", false, 5, orderNumber++));

        System.out.println("Demo calls added.");
        callQueue.showWaitingCalls(); // Hiển thị danh sách cuộc gọi sau khi thêm demo
    }

    /**
     * Nhận thông tin từ bàn phím và thêm một cuộc gọi mới vào hệ thống hàng đợi.
     */
    public void addNewCall() {
        String name = readText("Customer name: ");
        String phone = readText("Phone number: ");
        boolean vip = readYesNo("Is VIP? (y/n): ");
        int repeatCalls = readInt("Repeat calls: ");

        // Tạo đối tượng Call với các thông tin đã nhập và orderNumber hiện tại, sau đó tăng orderNumber lên 1
        Call call = new Call(name, phone, vip, repeatCalls, orderNumber++);
        callQueue.addCall(call);

        System.out.println("New call added.");
    }

    /**
     * Lấy và phục vụ cuộc gọi tiếp theo có độ ưu tiên cao nhất từ hàng đợi.
     */
    public void serveNextCall() {
        if (callQueue.isEmpty()) {
            System.out.println("No waiting calls.");
            return;
        }

        // Lấy ra phần tử đầu tiên trong hàng đợi ưu tiên (đã được sắp xếp tự động)
        Call nextCall = callQueue.getNextCall();
        System.out.println("Serving: " + nextCall);
    }

    /**
     * Hiển thị danh sách các cuộc gọi đang chờ phục vụ trong hàng đợi.
     */
    public void showWaitingCalls() {
        callQueue.showWaitingCalls();
    }

    /**
     * Phương thức phụ trợ: Đọc một chuỗi văn bản nhập vào từ console.
     * @param message Tin nhắn hiển thị yêu cầu nhập liệu
     * @return Chuỗi văn bản người dùng đã nhập
     */
    public String readText(String message) {
        System.out.print(message);
        return scanner.nextLine();
    }

    /**
     * Phương thức phụ trợ: Đọc một số nguyên hợp lệ từ bàn phím (có xử lý ngoại lệ).
     * @param message Tin nhắn hiển thị yêu cầu nhập số
     * @return Giá trị số nguyên hợp lệ
     */
    public int readInt(String message) {
        while (true) {
            try {
                System.out.print(message);
                return Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException error) {
                System.out.println("Please enter a number.");
            }
        }
    }

    /**
     * Phương thức phụ trợ: Đọc câu trả lời Có/Không dưới dạng phím y/n.
     * @param message Tin nhắn hiển thị yêu cầu lựa chọn y/n
     * @return true nếu nhập 'y' hoặc 'Y', false nếu nhập 'n' hoặc 'N'
     */
    public boolean readYesNo(String message) {
        while (true) {
            System.out.print(message);
            String answer = scanner.nextLine();

            if (answer.equalsIgnoreCase("y")) {
                return true;
            }

            if (answer.equalsIgnoreCase("n")) {
                return false;
            }

            System.out.println("Please enter y or n.");
        }
    }
}
