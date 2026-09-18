package com.Propertmanagement.dao;

import com.Propertmanagement.model.Building;
import com.Propertmanagement.model.Floor;
import com.Propertmanagement.model.Lease;
import com.Propertmanagement.model.Property;
import com.Propertmanagement.model.Tenant;
import com.Propertmanagement.model.Unit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Tests hasOverlappingLease() against one existing Lease on a Unit
// (2026-01-01 to 2026-06-30), checking date ranges that should and
// shouldn't be flagged as overlapping, plus the excludeLeaseId behavior
// used when editing a lease. Runs against the real dev database; each
// test builds its own Property->Building->Floor->Unit chain and Tenant,
// then deletes the Property (cascades down) and the Tenant afterward.
class LeaseDAOTest {

    private final PropertyDAO propertyDAO = new PropertyDAO();
    private final BuildingDAO buildingDAO = new BuildingDAO();
    private final FloorDAO floorDAO = new FloorDAO();
    private final UnitDAO unitDAO = new UnitDAO();
    private final TenantDAO tenantDAO = new TenantDAO();
    private final leasedao leaseDAO = new leasedao();

    private int propertyId;
    private int unitId;
    private int tenantId;
    private int existingLeaseId;

    @BeforeEach
    void setUp() {
        propertyId = propertyDAO.createProperty(new Property("Test Property", "1 Test St"));
        int buildingId = buildingDAO.createBuilding(new Building(propertyId, "Test Building"));
        int floorId = floorDAO.createFloor(new Floor(buildingId, 1));
        unitId = unitDAO.createUnit(new Unit(floorId, "101", "OCCUPIED", new BigDecimal("500.00")));
        tenantId = tenantDAO.createTenant(new Tenant("Jane", "Doe", "jane@example.com", "0770000000"));

        existingLeaseId = leaseDAO.createLease(new Lease(
                tenantId, unitId,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30),
                new BigDecimal("450.00"), "ACTIVE"));
    }

    @AfterEach
    void tearDown() {
        propertyDAO.deleteProperty(propertyId); // cascades Building -> Floor -> Unit -> Lease
        tenantDAO.deleteTenant(tenantId);
    }

    @Test
    void rangeFullyInsideExistingLease_isOverlapping() {
        boolean overlaps = leaseDAO.hasOverlappingLease(
                unitId, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1), -1);
        assertTrue(overlaps);
    }

    @Test
    void rangeEntirelyBeforeExistingLease_isNotOverlapping() {
        boolean overlaps = leaseDAO.hasOverlappingLease(
                unitId, LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 31), -1);
        assertFalse(overlaps);
    }

    @Test
    void rangeEntirelyAfterExistingLease_isNotOverlapping() {
        boolean overlaps = leaseDAO.hasOverlappingLease(
                unitId, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 1), -1);
        assertFalse(overlaps);
    }

    @Test
    void rangeOverlappingTheEndBoundary_isOverlapping() {
        // Starts before the existing lease ends (2026-06-30) and finishes after it.
        boolean overlaps = leaseDAO.hasOverlappingLease(
                unitId, LocalDate.of(2026, 6, 15), LocalDate.of(2026, 8, 1), -1);
        assertTrue(overlaps);
    }

    @Test
    void editingTheSameLeaseWithExcludeId_isNotFlaggedAgainstItself() {
        // Same exact range as the existing lease, but excluding its own id -
        // this is the case that matters when a user opens a lease to edit it
        // without changing its dates; it must not flag itself as an overlap.
        boolean overlaps = leaseDAO.hasOverlappingLease(
                unitId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), existingLeaseId);
        assertFalse(overlaps);
    }
}
