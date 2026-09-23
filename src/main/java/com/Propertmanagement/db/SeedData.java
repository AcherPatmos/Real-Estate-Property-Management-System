package com.Propertmanagement.db;

import com.Propertmanagement.dao.*;
import com.Propertmanagement.factory.MaintenanceTaskFactory;
import com.Propertmanagement.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;

// Fills an empty database with demo records so every screen has something to show:
// properties -> buildings -> floors -> units, tenants and their leases, payments,
// expenses, and maintenance requests whose tasks nest several levels deep.
// runs when the app is run first

public final class SeedData {

    private static final String COMPLETED = "Completed";

    private final PropertyDAO propertyDAO = new PropertyDAO();
    private final BuildingDAO buildingDAO = new BuildingDAO();
    private final FloorDAO floorDAO = new FloorDAO();
    private final UnitDAO unitDAO = new UnitDAO();
    private final TenantDAO tenantDAO = new TenantDAO();
    private final leasedao leaseDAO = new leasedao();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final ExpenseDAO expenseDAO = new ExpenseDAO();
    private final Maintenance_RequestDAO requestDAO = new Maintenance_RequestDAO();
    private final MaintenanceTaskDAO taskDAO = new MaintenanceTaskDAO();
    private final MaintenanceTaskFactory taskFactory = new MaintenanceTaskFactory(taskDAO);

    private SeedData() {
    }

    // Entry point called from PropertyManagerApp after the schema exists.
    // Returns true when demo data was inserted, false when the database already had data.
    public static boolean seedIfEmpty() {
        SeedData seeder = new SeedData();
        if (!seeder.propertyDAO.getAllProperties().isEmpty()) {
            return false;
        }
        seeder.seed();
        return true;
    }

    private void seed() {
        // Property 1: two buildings, the first with two floors
        int riverside = propertyDAO.createProperty(new Property("Riverside Apartments", "12 River Road, Kigali"));
        int blockA = buildingDAO.createBuilding(new Building(riverside, "Block A"));
        int blockB = buildingDAO.createBuilding(new Building(riverside, "Block B"));

        int a1 = floorDAO.createFloor(new Floor(blockA, 1));
        int a2 = floorDAO.createFloor(new Floor(blockA, 2));
        int b1 = floorDAO.createFloor(new Floor(blockB, 1));

        int unitA101 = unit(a1, "A1-01", "OCCUPIED", "450.00");
        int unitA102 = unit(a1, "A1-02", "OCCUPIED", "450.00");
        int unitA201 = unit(a2, "A2-01", "VACANT", "500.00");
        int unitA202 = unit(a2, "A2-02", "MAINTENANCE", "500.00");
        int unitB101 = unit(b1, "B1-01", "OCCUPIED", "380.00");
        unit(b1, "B1-02", "VACANT", "380.00");

        // Property 2: one building with two floors
        int hillview = propertyDAO.createProperty(new Property("Hillview Residences", "45 Hill Street, Kigali"));
        int mainBuilding = buildingDAO.createBuilding(new Building(hillview, "Main Building"));

        int h1 = floorDAO.createFloor(new Floor(mainBuilding, 1));
        int h2 = floorDAO.createFloor(new Floor(mainBuilding, 2));

        int unitH101 = unit(h1, "H1-01", "OCCUPIED", "620.00");
        unit(h1, "H1-02", "VACANT", "620.00");
        unit(h2, "H2-01", "VACANT", "700.00");

        // Tenants
        int amina = tenantDAO.createTenant(new Tenant("Amina", "Uwase", "amina.uwase@example.com", "0781234501"));
        int brian = tenantDAO.createTenant(new Tenant("Brian", "Mugisha", "brian.mugisha@example.com", "0781234502"));
        int claire = tenantDAO.createTenant(new Tenant("Claire", "Ingabire", "claire.ingabire@example.com", "0781234503"));
        int david = tenantDAO.createTenant(new Tenant("David", "Niyonzima", "david.niyonzima@example.com", "0781234504"));
        int esther = tenantDAO.createTenant(new Tenant("Esther", "Mukamana", "esther.mukamana@example.com", "0781234505"));

        // Leases: one per occupied unit for 2026, plus an ended 2025 lease on A1-01
        // so the unit shows a lease history without any overlapping dates
        lease(esther, unitA101, "2025-01-01", "2025-12-31", "430.00", "ENDED");
        int leaseAmina = lease(amina, unitA101, "2026-01-01", "2026-12-31", "450.00", "ACTIVE");
        int leaseBrian = lease(brian, unitA102, "2026-02-01", "2027-01-31", "450.00", "ACTIVE");
        int leaseClaire = lease(claire, unitB101, "2026-01-01", "2026-12-31", "380.00", "ACTIVE");
        int leaseDavid = lease(david, unitH101, "2026-03-01", "2027-02-28", "620.00", "ACTIVE");

        // Payments: a few months of rent on each active lease
        payments(leaseAmina, "450.00", "2026-01-05", 3, "Bank transfer");
        payments(leaseBrian, "450.00", "2026-02-03", 2, "Mobile money");
        payments(leaseClaire, "380.00", "2026-01-02", 3, "Cash");
        payments(leaseDavid, "620.00", "2026-03-01", 1, "Bank transfer");

        // Expenses: one at property level and one at unit level (the table allows one or the other)
        expenseDAO.createExpense(new Expense(riverside, null, "Security guard wages, January",
                new BigDecimal("300.00"), LocalDate.parse("2026-01-31")));
        expenseDAO.createExpense(new Expense(hillview, null, "Garden and grounds upkeep",
                new BigDecimal("120.00"), LocalDate.parse("2026-02-15")));
        expenseDAO.createExpense(new Expense(null, unitA202, "Plumber call-out fee",
                new BigDecimal("35.00"), LocalDate.parse("2026-03-10")));

        seedMaintenance(hillview, unitA202, unitB101);
    }

    // Maintenance requests with task trees three levels deep, so the recursive
    // cost and progress calculation has real depth to work through
    private void seedMaintenance(int hillview, int unitA202, int unitB101) {
        // Request 1 (unit level, in progress): a leak repair broken into nested tasks
        int leak = requestDAO.createRequest(new Maintenance_Request(null, unitA202,
                "Water leak in bathroom", "Tenant reported water dripping through the ceiling.", "IN_PROGRESS"));

        int fixPlumbing = topTask(leak, "Fix bathroom plumbing", "0.00", false);
        subtask(fixPlumbing, "Diagnose the leak source", "25.00", true);
        int replacePipe = subtask(fixPlumbing, "Replace damaged pipe section", "0.00", false);
        subtask(replacePipe, "Buy replacement pipe and fittings", "45.00", true);
        subtask(replacePipe, "Shut off water and fit new pipe", "75.00", false);
        subtask(fixPlumbing, "Test for leaks after repair", "0.00", false);
        int ceiling = topTask(leak, "Repair water-damaged ceiling", "0.00", false);
        subtask(ceiling, "Replace ceiling board", "60.00", false);
        subtask(ceiling, "Repaint ceiling", "30.00", false);

        // Request 2 (property level, open): exterior repaint for the whole building
        int repaint = requestDAO.createRequest(new Maintenance_Request(hillview, null,
                "Repaint building exterior", "Paint is peeling on the north and east walls.", "OPEN"));

        topTask(repaint, "Collect three painter quotes", "0.00", true);
        int paintWork = topTask(repaint, "Paint exterior walls", "0.00", false);
        subtask(paintWork, "North wall", "600.00", false);
        subtask(paintWork, "East wall", "450.00", false);

        // Request 3 (unit level, closed): a finished job, so completed work shows too
        int lock = requestDAO.createRequest(new Maintenance_Request(null, unitB101,
                "Broken front door lock", "Lock jammed and key would not turn.", "CLOSED"));

        topTask(lock, "Replace door lock", "40.00", true);
    }

    // Helpers

    private int unit(int floorId, String number, String status, String rent) {
        return unitDAO.createUnit(new Unit(floorId, number, status, new BigDecimal(rent)));
    }

    private int lease(int tenantId, int unitId, String start, String end, String rent, String status) {
        return leaseDAO.createLease(new Lease(tenantId, unitId,
                LocalDate.parse(start), LocalDate.parse(end), new BigDecimal(rent), status));
    }

    // Records one payment per month, starting from the given date
    private void payments(int leaseId, String amount, String firstDate, int months, String method) {
        LocalDate date = LocalDate.parse(firstDate);
        for (int i = 0; i < months; i++) {
            paymentDAO.createPayment(new Payment(leaseId, new BigDecimal(amount), date.plusMonths(i), method));
        }
    }

    // Tasks go through the factory, the same way the Maintenance screen creates them
    private int topTask(int requestId, String title, String cost, boolean completed) {
        return save(taskFactory.createTopLevelTask(requestId, title, new BigDecimal(cost)), completed);
    }

    private int subtask(int parentId, String title, String cost, boolean completed) {
        return save(taskFactory.createSubtask(parentId, title, new BigDecimal(cost)), completed);
    }

    // The factory always starts tasks as PENDING, so finished ones are marked complete here
    private int save(MaintenanceTask task, boolean completed) {
        if (completed) {
            task.setStatus(COMPLETED);
        }
        return taskDAO.createTask(task);
    }
}