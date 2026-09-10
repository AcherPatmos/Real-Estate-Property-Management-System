package com.Propertmanagement;

import com.Propertmanagement.dao.*;
import com.Propertmanagement.db.SchemaCreation;
import com.Propertmanagement.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class

Main {

    public static void main(String[] args) {

        System.out.println("--- Step 1: Initializing schema ---");
        SchemaCreation.initializeSchema();

        PropertyDAO propertyDAO = new PropertyDAO();
        BuildingDAO buildingDAO = new BuildingDAO();
        FloorDAO floorDAO = new FloorDAO();
        UnitDAO unitDAO = new UnitDAO();
        leasedao leasedao = new leasedao();
        TenantDAO TenantDAO = new TenantDAO();

        System.out.println("\n--- Step 2: CREATE across the full hierarchy ---");
        int propertyId = propertyDAO.createProperty(new Property("Riverside Apartments", "12 River Rd"));
        System.out.println("Created Property id=" + propertyId);

        int buildingId = buildingDAO.createBuilding(new Building(propertyId, "Block A"));
        System.out.println("Created Building id=" + buildingId);

        int floorId = floorDAO.createFloor(new Floor(buildingId, 1));
        System.out.println("Created Floor id=" + floorId);

        int unitId = unitDAO.createUnit(new Unit(floorId, "A1-01", "VACANT", new BigDecimal("450.00")));
        System.out.println("Created Unit id=" + unitId);

        Tenant tenant = new Tenant("John", "Doe", "john@example.com", "0771234567");
        int tenantId = TenantDAO.createTenant(tenant);
        System.out.println("Created Tenant id=" + tenantId);

        Lease lease = new Lease(
                tenantId,
                unitId,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                new BigDecimal("450.00"),
                "ACTIVE"
        );
        int leaseId = leasedao.createLease(lease);
        System.out.println("Created Lease id=" + leaseId);


        System.out.println("\n--- Step 3: READ each record back ---");
        System.out.println(propertyDAO.getPropertyById(propertyId));
        System.out.println(buildingDAO.getBuildingById(buildingId));
        System.out.println(floorDAO.getFloorById(floorId));
        System.out.println(unitDAO.getUnitById(unitId));
        System.out.println(leasedao.getLeaseById(leaseId));
        System.out.println(TenantDAO.getTenantById(tenantId));

        System.out.println("\n--- Step 4: UPDATE each record ---");
        Property property = propertyDAO.getPropertyById(propertyId);
        property.setName("Riverside Apartments (Renamed)");
        System.out.println("Property update succeeded: " + propertyDAO.updateProperty(property));

        Unit unit = unitDAO.getUnitById(unitId);
        unit.setStatus("OCCUPIED");
        unit.setRentAmount(new BigDecimal("475.00"));
        System.out.println("Unit update succeeded: " + unitDAO.updateUnit(unit));
        System.out.println("Unit after update: " + unitDAO.getUnitById(unitId));

        System.out.println("\n--- Step 5: Query by parent (hierarchy scoping) ---");
        System.out.println("Buildings under property " + propertyId + ": " +
                buildingDAO.getBuildingsByPropertyId(propertyId));
        System.out.println("Floors under building " + buildingId + ": " +
                floorDAO.getFloorsByBuildingId(buildingId));
        System.out.println("Units under floor " + floorId + ": " +
                unitDAO.getUnitsByFloorId(floorId));

        System.out.println("\n--- Step 6: DELETE the property and confirm cascade ---");
        System.out.println("Property delete succeeded: " + propertyDAO.deleteProperty(propertyId));
        System.out.println("Building still exists after cascade? " +
                (buildingDAO.getBuildingById(buildingId) != null));
        System.out.println("Floor still exists after cascade? " +
                (floorDAO.getFloorById(floorId) != null));
        System.out.println("Unit still exists after cascade? " +
                (unitDAO.getUnitById(unitId) != null));

        System.out.println("\n--- Test complete ---");
    }
}