package com.Propertmanagement.model;

import java.math.BigDecimal;

// Not a database entity - carries the result of recursively walking a
// Property -> Building -> Floor -> Unit subtree (see PropertyHierarchyService).
public class HierarchyStats {

    private final int totalUnits;
    private final int occupiedUnits;
    private final int vacantUnits;
    private final BigDecimal totalIncome;   // sum of rent_amount for OCCUPIED units in this subtree
    private final BigDecimal totalExpenses; // sum of Expense rows attached anywhere in this subtree

    public HierarchyStats(int totalUnits, int occupiedUnits, int vacantUnits,
                           BigDecimal totalIncome, BigDecimal totalExpenses) {
        this.totalUnits = totalUnits;
        this.occupiedUnits = occupiedUnits;
        this.vacantUnits = vacantUnits;
        this.totalIncome = totalIncome;
        this.totalExpenses = totalExpenses;
    }

    public static HierarchyStats empty() {
        return new HierarchyStats(0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public int getTotalUnits() {
        return totalUnits;
    }

    public int getOccupiedUnits() {
        return occupiedUnits;
    }

    public int getVacantUnits() {
        return vacantUnits;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }

    public BigDecimal getNetIncome() {
        return totalIncome.subtract(totalExpenses);
    }

    // Returns 0.0 for a subtree with no units instead of dividing by zero.
    public double getOccupancyPercentage() {
        if (totalUnits == 0) {
            return 0.0;
        }
        return (occupiedUnits / (double) totalUnits) * 100.0;
    }

    // Combines this subtree's stats with a sibling subtree's stats - used by the
    // recursive traversal to fold children's results together as it walks back up.
    public HierarchyStats plus(HierarchyStats other) {
        return new HierarchyStats(
                this.totalUnits + other.totalUnits,
                this.occupiedUnits + other.occupiedUnits,
                this.vacantUnits + other.vacantUnits,
                this.totalIncome.add(other.totalIncome),
                this.totalExpenses.add(other.totalExpenses)
        );
    }

    @Override
    public String toString() {
        return "HierarchyStats{units=" + totalUnits + " (" + occupiedUnits + " occupied, "
                + vacantUnits + " vacant), income=" + totalIncome + ", expenses=" + totalExpenses
                + ", net=" + getNetIncome() + "}";
    }
}
