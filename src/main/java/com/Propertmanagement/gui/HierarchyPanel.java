package com.Propertmanagement.gui;

import com.Propertmanagement.dao.BuildingDAO;
import com.Propertmanagement.dao.FloorDAO;
import com.Propertmanagement.dao.PropertyDAO;
import com.Propertmanagement.dao.UnitDAO;
import com.Propertmanagement.model.Building;
import com.Propertmanagement.model.Floor;
import com.Propertmanagement.model.Property;
import com.Propertmanagement.model.Unit;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import java.awt.*;
import java.util.List;

// Read-only Property -> Building -> Floor -> Unit hierarchy as a JTree.

public class HierarchyPanel extends JPanel {

    // Every possible role a tree node can play, including the two synthetic ones (LOADING, EMPTY)
    private enum NodeType { PROPERTY, BUILDING, FLOOR, UNIT, LOADING, EMPTY }

    // One DAO per level of the hierarchy; each is only queried when its level's nodes are expanded
    private final PropertyDAO propertyDAO;
    private final BuildingDAO buildingDAO;
    private final FloorDAO floorDAO;
    private final UnitDAO unitDAO;

    // Hidden root node; just holds top-level Property nodes
    private final DefaultMutableTreeNode root = new DefaultMutableTreeNode(new NodeData(NodeType.PROPERTY, "Properties", null));
    // Bridges the tree node structure to the JTree UI component; call reload()/nodeStructureChanged() to repaint after mutating nodes
    private final DefaultTreeModel treeModel = new DefaultTreeModel(root);
    private final JTree tree = new JTree(treeModel);
    // Shows load errors or "no data" messages instead of failing silently
    private final JLabel statusLabel = new JLabel(" ");

    public HierarchyPanel(PropertyDAO propertyDAO, BuildingDAO buildingDAO, FloorDAO floorDAO, UnitDAO unitDAO) {
        super(new BorderLayout(12, 12));
        this.propertyDAO = propertyDAO;
        this.buildingDAO = buildingDAO;
        this.floorDAO = floorDAO;
        this.unitDAO = unitDAO;

        setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel heading = new JLabel("Property Hierarchy");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));

        // Lets the user manually re-fetch the top level (e.g. after data changes elsewhere in the app)
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> reloadRoot());

        JPanel top = new JPanel(new BorderLayout());
        top.add(heading, BorderLayout.WEST);
        top.add(refreshButton, BorderLayout.EAST);

        tree.setRootVisible(false);       // hide the synthetic "Properties" root, show only real Property nodes
        tree.setShowsRootHandles(true);   // still draw expand arrows on those top-level nodes
        // fires right before a node opens, giving us a chance to fetch its children first
        tree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                Object last = event.getPath().getLastPathComponent();
                if (last instanceof DefaultMutableTreeNode) {
                    loadChildrenIfNeeded((DefaultMutableTreeNode) last);
                }
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
                // collapsing doesn't discard loaded children, so re-expanding is instant
            }
        });

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(tree), BorderLayout.CENTER);
        statusLabel.setForeground(new Color(178, 34, 34));
        add(statusLabel, BorderLayout.SOUTH);

        reloadRoot(); // initial fetch: load the Property level immediately, everything below stays lazy
    }

    // Fetches all Properties and rebuilds the top level of the tree; each Property node gets a placeholder so it's expandable
    private void reloadRoot() {
        root.removeAllChildren();
        try {
            List<Property> properties = propertyDAO.getAllProperties();
            for (Property property : properties) {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(
                        new NodeData(NodeType.PROPERTY, property.getName(), property));
                addLoadingPlaceholder(node);
                root.add(node);
            }
            statusLabel.setText(properties.isEmpty() ? "No properties yet." : " ");
        } catch (RuntimeException e) {
            statusLabel.setText("Could not load properties: " + rootMessage(e));
        }
        treeModel.reload(); // tell the JTree the whole structure changed, since we rebuilt root from scratch
    }

    // Adds a fake child so Swing draws an expand arrow before real data has been fetched
    private void addLoadingPlaceholder(DefaultMutableTreeNode node) {
        node.add(new DefaultMutableTreeNode(new NodeData(NodeType.LOADING, "Loading...", null)));
    }

    // Called on first expansion of a node, fetches that node's real children from the right DAO, and replaces the placeholder
    private void loadChildrenIfNeeded(DefaultMutableTreeNode node) {
        NodeData data = (NodeData) node.getUserObject();
        if (data.childrenLoaded) {
            return; // already fetched once and does not revisit DAO on subsequent visits
        }
        data.childrenLoaded = true;

        node.removeAllChildren(); // discard the empty loading placeholder
        try {
            switch (data.type) {
                case PROPERTY: {
                    // Property expanded -> fetch its Buildings
                    Property property = (Property) data.model;
                    List<Building> buildings = buildingDAO.getBuildingsByPropertyId(property.getId());
                    for (Building b : buildings) {
                        DefaultMutableTreeNode child = new DefaultMutableTreeNode(
                                new NodeData(NodeType.BUILDING, b.getName(), b));
                        addLoadingPlaceholder(child); // each Building also gets its own placeholder, staying lazy one level down
                        node.add(child);
                    }
                    if (buildings.isEmpty()) {
                        node.add(emptyNode("No buildings"));
                    }
                    break;
                }
                case BUILDING: {
                    // Building expanded -> fetch its Floors
                    Building building = (Building) data.model;
                    List<Floor> floors = floorDAO.getFloorsByBuildingId(building.getId());
                    for (Floor f : floors) {
                        DefaultMutableTreeNode child = new DefaultMutableTreeNode(
                                new NodeData(NodeType.FLOOR, "Floor " + f.getFloorNumber(), f));
                        addLoadingPlaceholder(child);
                        node.add(child);
                    }
                    if (floors.isEmpty()) {
                        node.add(emptyNode("No floors"));
                    }
                    break;
                }
                case FLOOR: {
                    // Floor expanded -> fetch its Units (the bottom of the hierarchy)
                    Floor floor = (Floor) data.model;
                    List<Unit> units = unitDAO.getUnitsByFloorId(floor.getId());
                    for (Unit u : units) {
                        String label = "Unit " + u.getUnitNumber() + " (" + u.getStatus() + ", " + u.getRentAmount() + ")";
                        DefaultMutableTreeNode child = new DefaultMutableTreeNode(
                                new NodeData(NodeType.UNIT, label, u));
                        child.setAllowsChildren(false); // Units are leaves — no further expansion, no placeholder needed
                        node.add(child);
                    }
                    if (units.isEmpty()) {
                        node.add(emptyNode("No units"));
                    }
                    break;
                }
                default:
                    break; // LOADING/EMPTY nodes never get expanded themselves, nothing to fetch
            }
        } catch (RuntimeException e) {
            node.add(emptyNode("Could not load: " + rootMessage(e))); // surface DAO failures inline in the tree, not as a crash
        }
        treeModel.nodeStructureChanged(node);
    }

    // Builds a non-expandable placeholder used for "no data" messages at any level
    private DefaultMutableTreeNode emptyNode(String text) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(new NodeData(NodeType.EMPTY, text, null));
        node.setAllowsChildren(false);
        return node;
    }

    // Unwraps nested exceptions to show the most specific underlying error message to the user
    private static String rootMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : t.getMessage();
    }

    // Wraps whatever a tree node represents: its role, display label, backing model object, and whether it's been fetched yet
    private static class NodeData {
        final NodeType type;
        final String label;
        final Object model; // Property, Building, Floor, or Unit null for synthetic nodes
        boolean childrenLoaded = false; // the flag that makes lazy loading true after the first expansion

        NodeData(NodeType type, String label, Object model) {
            this.type = type;
            this.label = label;
            this.model = model;
        }

        @Override
        public String toString() {
            return label; // JTree calls this to render the node's text
        }
    }
}