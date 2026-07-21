package model;

/**
 * Class representing a call in the Call Center system.
 * 
 * Each call contains customer information, priority status,
 * and attributes for computing the Priority Score.
 * 
 * Basic priority score formula:
 *   priorityScore = (isVIP ? VIP_BONUS : 0) + (repeatCalls * REPEAT_MULTIPLIER) + waitTime
 */
public class Call {

    // --- Attributes ---
    private String customerId;
    private String customerName;
    private String phoneNumber;
    private boolean isVIP;
    private int repeatCalls;
    private int orderNumber;
    private int waitTime;
    private int priorityScore;
    private CallStatus status;
    private long entryTime;

    private boolean allowExceedVIP = false;

    // --- Constants for priority score calculation (defaults, can be overridden via config) ---
    private static int VIP_BONUS = 400;
    private static int REPEAT_MULTIPLIER = 5;

    /**
     * Full Constructor.
     */
    public Call(String customerId, String customerName, String phoneNumber,
                boolean isVIP, int repeatCalls, int orderNumber) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.phoneNumber = phoneNumber;
        this.isVIP = isVIP;
        this.repeatCalls = repeatCalls;
        this.orderNumber = orderNumber;
        this.waitTime = 0;
        this.status = CallStatus.WAITING;
        this.entryTime = System.currentTimeMillis();
        this.priorityScore = calculateBasePriority();
    }

    /**
     * Calculates base priority score (excluding aging).
     * VIP gets +50, each repeat call gets +10.
     */
    private int calculateBasePriority() {
        int score = 0;
        if (isVIP) {
            score += VIP_BONUS;
        }
        score += repeatCalls * REPEAT_MULTIPLIER;
        if (!isVIP && !allowExceedVIP) {
            score = Math.min(score, VIP_BONUS);
        }
        return score;
    }

    /**
     * Gets base priority score (excluding waitTime).
     */
    public int getBasePriority() {
        return calculateBasePriority();
    }

    /**
     * Gets aged priority score (including waitTime).
     */
    public int getAgedPriority() {
        int total = priorityScore + waitTime;
        if (!isVIP && !allowExceedVIP) {
            return Math.min(total, VIP_BONUS);
        }
        return total;
    }

    /**
     * Increments wait time by 1 unit.
     */
    public void incrementWaitTime() {
        this.waitTime++;
    }

    /**
     * Increments wait time by a custom amount.
     */
    public void incrementWaitTime(int amount) {
        this.waitTime += amount;
    }

    // --- Static configuration for priority score ---
    public static void setVipBonus(int bonus) {
        VIP_BONUS = bonus;
    }

    public static void setRepeatMultiplier(int multiplier) {
        REPEAT_MULTIPLIER = multiplier;
    }

    // --- Getters & Setters ---

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
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

    public void setVIP(boolean VIP) {
        isVIP = VIP;
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

    public int getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(int priorityScore) {
        this.priorityScore = priorityScore;
    }

    public boolean isAllowExceedVIP() {
        return allowExceedVIP;
    }

    public void setAllowExceedVIP(boolean allowExceedVIP) {
        this.allowExceedVIP = allowExceedVIP;
        // Recalculate priorityScore to apply the cap if needed
        this.priorityScore = calculateBasePriority();
    }

    public CallStatus getStatus() {
        return status;
    }

    public void setStatus(CallStatus status) {
        this.status = status;
    }

    public long getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(long entryTime) {
        this.entryTime = entryTime;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | Phone: %s | VIP: %s | Repeats: %d | Score: %d | Status: %s",
                customerId, customerName, phoneNumber,
                isVIP ? "Yes" : "No", repeatCalls,
                getAgedPriority(), status);
    }
}
