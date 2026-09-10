package com.Propertmanagement.model;

import java.math.BigDecimal;

// Not a database entity - just carries the result of recursively walking a
// MaintenanceTask subtree (see MaintenanceTaskDAO.computeStats).
public class MaintenanceStats {

    private final BigDecimal totalCost;
    private final int completedCount;
    private final int outstandingCount;

    public MaintenanceStats(BigDecimal totalCost, int completedCount, int outstandingCount) {
        this.totalCost = totalCost;
        this.completedCount = completedCount;
        this.outstandingCount = outstandingCount;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public int getOutstandingCount() {
        return outstandingCount;
    }

    public int getTotalCount() {
        return completedCount + outstandingCount;
    }

    // Returns 0.0 for an empty subtree instead of dividing by zero.
    public double getCompletionPercentage() {
        int total = getTotalCount();
        if (total == 0) {
            return 0.0;
        }
        return (completedCount / (double) total) * 100.0;
    }

    // Combines this subtree's stats with a sibling subtree's stats, plus this
    // node's own contribution - used by the recursive DAO method to fold
    // children's results together as it walks back up.
    public MaintenanceStats plus(MaintenanceStats other) {
        return new MaintenanceStats(
                this.totalCost.add(other.totalCost),
                this.completedCount + other.completedCount,
                this.outstandingCount + other.outstandingCount
        );
    }

    @Override
    public String toString() {
        return "MaintenanceStats{totalCost=" + totalCost + ", completed=" + completedCount
                + ", outstanding=" + outstandingCount + ", completion="
                + String.format("%.1f", getCompletionPercentage()) + "%}";
    }
}
