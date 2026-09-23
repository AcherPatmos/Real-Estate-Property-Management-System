package com.Propertmanagement.service;

import com.Propertmanagement.dao.BuildingDAO;
import com.Propertmanagement.dao.ExpenseDAO;
import com.Propertmanagement.dao.FloorDAO;
import com.Propertmanagement.dao.PropertyDAO;
import com.Propertmanagement.dao.UnitDAO;
import com.Propertmanagement.db.SchemaCreation;
import com.Propertmanagement.model.Building;
import com.Propertmanagement.model.Expense;
import com.Propertmanagement.model.Floor;
import com.Propertmanagement.model.HierarchyStats;
import com.Propertmanagement.model.Property;
import com.Propertmanagement.model.Unit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Tests the recursive property-hierarchy traversal: a Property with no
// Buildings at all (empty hierarchy - the base case with zero levels
// beneath it), a Property with one Building/Floor/Unit (single unit), and
// a Property with two Buildings each with their own Floors/Units and a
// mix of occupied/vacant status plus expenses at both the Property level
// and the Unit level (multi-level). Runs against the real dev database;
// each test deletes the Property it created afterward, which cascades
// down through Building -> Floor -> Unit -> Expense automatically.
class PropertyHierarchyServiceTest {

    // The tests may run before the app has ever been launched on this machine,
    // so create the database and its tables first, exactly as the app does.
    @BeforeAll
    static void createSchema() throws SQLException {
        SchemaCreation.initializeSchema();
    }

    private final PropertyDAO propertyDAO = new PropertyDAO();
    private final BuildingDAO buildingDAO = new BuildingDAO();
    private final FloorDAO floorDAO = new FloorDAO();
    private final UnitDAO unitDAO = new UnitDAO();
    private final ExpenseDAO expenseDAO = new ExpenseDAO();
    private final PropertyHierarchyService service =
            new PropertyHierarchyService(buildingDAO, floorDAO, unitDAO, expenseDAO);

    private int propertyId;

    @BeforeEach
    void setUp() {
        propertyId = propertyDAO.createProperty(new Property("Test Property", "1 Test St"));
    }

    @AfterEach
    void tearDown() {
        // Cascades: Property -> Building -> Floor -> Unit -> Expense (property- and unit-level).
        propertyDAO.deleteProperty(propertyId);
    }

    @Test
    void emptyHierarchy_noBuildings_returnsAllZeros() {
        HierarchyStats stats = service.computeStats(propertyId);

        assertEquals(0, stats.getTotalUnits());
        assertEquals(0, stats.getOccupiedUnits());
        assertEquals(0, stats.getVacantUnits());
        assertEquals(0, new BigDecimal("0.00").compareTo(stats.getTotalIncome()));
        assertEquals(0, new BigDecimal("0.00").compareTo(stats.getTotalExpenses()));
        assertEquals(0.0, stats.getOccupancyPercentage(), 0.001);
    }

    @Test
    void singleOccupiedUnit_reflectsThatUnitsRentAndExpense() {
        int buildingId = buildingDAO.createBuilding(new Building(propertyId, "Test Building"));
        int floorId = floorDAO.createFloor(new Floor(buildingId, 1));
        int unitId = unitDAO.createUnit(new Unit(floorId, "101", "OCCUPIED", new BigDecimal("500.00")));
        expenseDAO.createExpense(new Expense(
                null, unitId, "Plumbing repair", new BigDecimal("75.00"), LocalDate.of(2026, 1, 15)));

        HierarchyStats stats = service.computeStats(propertyId);

        assertEquals(1, stats.getTotalUnits());
        assertEquals(1, stats.getOccupiedUnits());
        assertEquals(0, stats.getVacantUnits());
        assertEquals(0, new BigDecimal("500.00").compareTo(stats.getTotalIncome()));
        assertEquals(0, new BigDecimal("75.00").compareTo(stats.getTotalExpenses()));
        assertEquals(100.0, stats.getOccupancyPercentage(), 0.001);
    }

    @Test
    void multiLevelMixedOccupancy_aggregatesAcrossWholeTree() {
        // Property-level expense (not tied to any specific unit).
        expenseDAO.createExpense(new Expense(
                propertyId, null, "Property insurance", new BigDecimal("200.00"), LocalDate.of(2026, 1, 1)));

        // Building A: one floor, two units (one occupied, one vacant).
        int buildingA = buildingDAO.createBuilding(new Building(propertyId, "Building A"));
        int floorA1 = floorDAO.createFloor(new Floor(buildingA, 1));
        unitDAO.createUnit(new Unit(floorA1, "A101", "OCCUPIED", new BigDecimal("400.00")));
        int vacantUnitId = unitDAO.createUnit(new Unit(floorA1, "A102", "VACANT", new BigDecimal("400.00")));
        expenseDAO.createExpense(new Expense(
                null, vacantUnitId, "Repaint before re-letting", new BigDecimal("60.00"), LocalDate.of(2026, 2, 1)));

        // Building B: one floor, one occupied unit.
        int buildingB = buildingDAO.createBuilding(new Building(propertyId, "Building B"));
        int floorB1 = floorDAO.createFloor(new Floor(buildingB, 1));
        unitDAO.createUnit(new Unit(floorB1, "B101", "OCCUPIED", new BigDecimal("350.00")));

        HierarchyStats stats = service.computeStats(propertyId);

        // 3 units total across both buildings: 2 occupied, 1 vacant.
        assertEquals(3, stats.getTotalUnits());
        assertEquals(2, stats.getOccupiedUnits());
        assertEquals(1, stats.getVacantUnits());

        // Income only counts occupied units: 400.00 (A101) + 350.00 (B101) = 750.00.
        // The vacant unit's rent_amount (A102, 400.00) is NOT counted as income.
        assertEquals(0, new BigDecimal("750.00").compareTo(stats.getTotalIncome()));

        // Expenses: 200.00 (property-level) + 60.00 (A102, unit-level) = 260.00.
        assertEquals(0, new BigDecimal("260.00").compareTo(stats.getTotalExpenses()));

        // Net income = 750.00 - 260.00 = 490.00.
        assertEquals(0, new BigDecimal("490.00").compareTo(stats.getNetIncome()));

        // 2 of 3 units occupied = 66.6...%.
        assertEquals(66.67, stats.getOccupancyPercentage(), 0.01);
    }
}
