package com.Propertmanagement.service;

import com.Propertmanagement.dao.BuildingDAO;
import com.Propertmanagement.dao.ExpenseDAO;
import com.Propertmanagement.dao.FloorDAO;
import com.Propertmanagement.dao.UnitDAO;
import com.Propertmanagement.model.Building;
import com.Propertmanagement.model.Expense;
import com.Propertmanagement.model.Floor;
import com.Propertmanagement.model.HierarchyStats;
import com.Propertmanagement.model.Unit;

import java.math.BigDecimal;
import java.util.List;

/**
 * THE RECURSIVE FEATURE for the Property hierarchy.
 * Walks Property -> Building -> Floor -> Unit and folds unit counts,
 * occupancy, rental income, and expenses back up into one HierarchyStats.
 *
 * Unlike MaintenanceTaskDAO.computeStats() (which recurses through ONE
 * self-referencing table), this hierarchy spans four separate tables, so
 * the recursion here is MUTUAL RECURSION across four methods - one per
 * level - each level's method fetching its children from the right DAO
 * and calling the next level's method. computeUnitStats() is the base
 * case: a Unit is a leaf, it never recurses further.
 *
 * This is a new, standalone class - it only calls existing DAOs through
 * their public methods and does not modify PropertyDAO, BuildingDAO,
 * FloorDAO, UnitDAO, or ExpenseDAO in any way.
 *
 * ASSUMPTION worth confirming: "rental income" here means the rent_amount
 * of currently OCCUPIED units (projected income), not a sum of actual
 * Payment records received. If the project wants realized payments
 * instead, this class would need a PaymentDAO/LeaseDAO dependency added
 * to walk Unit -> Lease -> Payment instead of reading Unit.rentAmount
 * directly.
 */
public class PropertyHierarchyService {

    private final BuildingDAO buildingDAO;
    private final FloorDAO floorDAO;
    private final UnitDAO unitDAO;
    private final ExpenseDAO expenseDAO;

    public PropertyHierarchyService(BuildingDAO buildingDAO, FloorDAO floorDAO,
                                     UnitDAO unitDAO, ExpenseDAO expenseDAO) {
        this.buildingDAO = buildingDAO;
        this.floorDAO = floorDAO;
        this.unitDAO = unitDAO;
        this.expenseDAO = expenseDAO;
    }

    // Entry point: stats for an entire Property, including expenses attached
    // directly to the Property itself (not just ones attached to its Units).
    public HierarchyStats computeStats(int propertyId) {
        HierarchyStats combined = HierarchyStats.empty();

        List<Building> buildings = buildingDAO.getBuildingsByPropertyId(propertyId);
        for (Building building : buildings) {
            combined = combined.plus(computeBuildingStats(building.getId()));
        }

        BigDecimal propertyLevelExpenses = sumExpenses(expenseDAO.getExpensesByPropertyId(propertyId));
        combined = combined.plus(new HierarchyStats(0, 0, 0, BigDecimal.ZERO, propertyLevelExpenses));

        return combined;
    }

    // Recursive case: a Building's stats = the combined stats of every Floor beneath it.
    private HierarchyStats computeBuildingStats(int buildingId) {
        HierarchyStats combined = HierarchyStats.empty();
        List<Floor> floors = floorDAO.getFloorsByBuildingId(buildingId);
        for (Floor floor : floors) {
            combined = combined.plus(computeFloorStats(floor.getId()));
        }
        return combined;
    }

    // Recursive case: a Floor's stats = the combined stats of every Unit beneath it.
    private HierarchyStats computeFloorStats(int floorId) {
        HierarchyStats combined = HierarchyStats.empty();
        List<Unit> units = unitDAO.getUnitsByFloorId(floorId);
        for (Unit unit : units) {
            combined = combined.plus(computeUnitStats(unit));
        }
        return combined;
    }

    // BASE CASE: a Unit is a leaf in this hierarchy - it never recurses further.
    // Its own contribution is: itself counted once, its rent if occupied, and
    // any Expense rows attached directly to it.
    private HierarchyStats computeUnitStats(Unit unit) {
        boolean occupied = "OCCUPIED".equalsIgnoreCase(unit.getStatus());

        int totalUnits = 1;
        int occupiedUnits = occupied ? 1 : 0;
        int vacantUnits = occupied ? 0 : 1;
        BigDecimal income = occupied ? unit.getRentAmount() : BigDecimal.ZERO;
        BigDecimal expenses = sumExpenses(expenseDAO.getExpensesByUnitId(unit.getId()));

        return new HierarchyStats(totalUnits, occupiedUnits, vacantUnits, income, expenses);
    }

    private BigDecimal sumExpenses(List<Expense> expenses) {
        BigDecimal total = BigDecimal.ZERO;
        for (Expense expense : expenses) {
            total = total.add(expense.getAmount());
        }
        return total;
    }
}
