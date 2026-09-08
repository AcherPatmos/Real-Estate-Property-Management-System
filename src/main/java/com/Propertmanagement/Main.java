package com.Propertmanagement;

import com.Propertmanagement.dao.BuildingDAO;
import com.Propertmanagement.dao.FloorDAO;
import com.Propertmanagement.dao.PropertyDAO;
import com.Propertmanagement.dao.UnitDAO;
import com.Propertmanagement.db.SchemaCreation;
import com.Propertmanagement.model.Building;
import com.Propertmanagement.model.Floor;
import com.Propertmanagement.model.Property;
import com.Propertmanagement.model.Unit;

import java.math.BigDecimal;

public class Main {

    public static void main(String[] args) {

        System.out.println("--- Step 1: Initializing schema ---");
        SchemaCreation.initializeSchema();

        PropertyDAO propertyDAO = new PropertyDAO();
        BuildingDAO buildingDAO = new BuildingDAO();
        FloorDAO floorDAO = new FloorDAO();
        UnitDAO unitDAO = new UnitDAO();

        System.out.println("\n--- Step 2: CREATE across the full hierarchy ---");
        int propertyId = propertyDAO.createProperty(new Property("Riverside Apartments", "12 River Rd"));
        System.out.println("Created Property id=" + propertyId);

        int buildingId = buildingDAO.createBuilding(new Building(propertyId, "Block A"));
        System.out.println("Created Building id=" + buildingId);

        int floorId = floorDAO.createFloor(new Floor(buildingId, 1));
        System.out.println("Created Floor id=" + floorId);

        int unitId = unitDAO.createUnit(new Unit(floorId, "A1-01", "VACANT", new BigDecimal("450.00")));
        System.out.println("Created Unit id=" + unitId);

        System.out.println("\n--- Step 3: READ each record back ---");
        System.out.println(propertyDAO.getPropertyById(propertyId));
        System.out.println(buildingDAO.getBuildingById(buildingId));
        System.out.println(floorDAO.getFloorById(floorId));
        System.out.println(unitDAO.getUnitById(unitId));

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