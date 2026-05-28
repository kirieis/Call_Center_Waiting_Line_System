public class Call {
    private final String customerName;
    private final String phoneNumber;
    private final boolean vip;
    private final int repeatCalls;
    private final int orderNumber;

    public Call(String customerName, String phoneNumber, boolean vip, int repeatCalls, int orderNumber) {
        this.customerName = customerName;
        this.phoneNumber = phoneNumber;
        this.vip = vip;
        this.repeatCalls = repeatCalls;
        this.orderNumber = orderNumber;
    }

    public int getPriorityPoint() {
        int point = 0;

        if (vip) {
            point += 100;
        }

        point += repeatCalls * 10;
        return point;
    }

    public int getOrderNumber() {
        return orderNumber;
    }

    @Override
    public String toString() {
        return customerName
                + " | Phone: " + phoneNumber
                + " | VIP: " + (vip ? "Yes" : "No")
                + " | Repeat calls: " + repeatCalls
                + " | Priority point: " + getPriorityPoint();
    }
}
