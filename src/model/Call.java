package model;

public class Call {
    private String customerName;
    private String phoneNumber;
    private boolean isVIP;
    private int repeatCalls;
    private int orderNumber;
    private int waitTime;
    private CallStatus status;

    public Call(String name, String phone, boolean vip, int repeat, int order) {
        this.customerName = name;
        this.phoneNumber = phone;
        this.isVIP = vip;
        this.repeatCalls = repeat;
        this.orderNumber = order;
        this.waitTime = 0;
        this.status = CallStatus.WAITING;
    }

    public int getBasePriority() {
        return (isVIP ? 100 : 0) + repeatCalls * 10;
    }

    public int getAgedPriority() {
        return getBasePriority() + (waitTime * 5);
    }

    public void incrementWaitTime() {
        this.waitTime++;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean isVIP() {
        return isVIP;
    }

    public void setVIP(boolean vip) {
        isVIP = vip;
    }

    public int getRepeatCalls() {
        return repeatCalls;
    }

    public void setRepeatCalls(int repeatCalls) {
        this.repeatCalls = repeatCalls;
    }

    public int getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(int orderNumber) {
        this.orderNumber = orderNumber;
    }

    public int getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(int waitTime) {
        this.waitTime = waitTime;
    }

    public CallStatus getStatus() {
        return status;
    }

    public void setStatus(CallStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return customerName + " | " + phoneNumber;
    }
}
