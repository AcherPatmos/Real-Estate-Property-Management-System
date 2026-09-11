package com.Propertmanagement.PropertyManagerApp;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.Map;

// PropertyManagerApp
// Skeleton application shell:
// 1. One main window (JFrame) that opens on launch.
// 2. A sidebar of buttons for navigation.
// 3. Placeholder screens swapped in/out via CardLayout.
// No CRUD logic yet.

public class PropertyManagerApp extends JFrame {

    // Names of every section in the app. Add/remove entries here
    // and both the sidebar and the CardLayout deck update automatically.
    private static final String[] SECTIONS = {
            "Properties",
            "Units",
            "Buildings",
            "Floors",
            "Maintenance",
            "Tenants",
            "Leases"
    };

    // Color palette
    private static final Color SIDEBAR_BG      = new Color(30, 42, 56);
    private static final Color SIDEBAR_TITLE   = new Color(255, 255, 255);
    private static final Color NAV_TEXT        = new Color(198, 210, 223);
    private static final Color NAV_TEXT_ACTIVE = Color.WHITE;
    private static final Color NAV_ACTIVE_BG   = new Color(52, 120, 191);
    private static final Color NAV_HOVER_BG    = new Color(44, 58, 75);

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel screenContainer = new JPanel(cardLayout);
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();

    public PropertyManagerApp() {
        super("Property Manager");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 600));
        setLayout(new BorderLayout());

        add(buildSidebar(), BorderLayout.WEST);
        add(buildScreens(), BorderLayout.CENTER);

        // opens on section one by default
        selectSection(SECTIONS[0]);

        pack();
        setLocationRelativeTo(null); // center on screen
    }

    // Sidebar
    private JComponent buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(20, 14, 16, 14));
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(220, 0));

        JLabel title = new JLabel("Property Manager");
        title.setForeground(SIDEBAR_TITLE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 19f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.setBorder(new EmptyBorder(0, 4, 22, 0));
        sidebar.add(title);

        for (String section : SECTIONS) {
            JButton button = createNavButton(section);
            navButtons.put(section, button);
            sidebar.add(button);
            sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        }

        sidebar.add(Box.createVerticalGlue());
        return sidebar;
    }

    private JButton createNavButton(String section) {
        JButton button = new JButton(section);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        button.setPreferredSize(new Dimension(0, 42));
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(new EmptyBorder(9, 14, 9, 12));
        button.setFont(button.getFont().deriveFont(Font.BOLD, 15f));
        button.setForeground(NAV_TEXT);
        button.setBackground(SIDEBAR_BG);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Hover effect on the buttons
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (!section.equals(currentSection)) {
                    button.setBackground(NAV_HOVER_BG);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (!section.equals(currentSection)) {
                    button.setBackground(SIDEBAR_BG);
                }
            }
        });

        button.addActionListener((ActionEvent e) -> selectSection(section));
        return button;
    }

    private String currentSection = null;

//  Updates which screen is showing and highlights the active nav button
    private void selectSection(String section) {
        currentSection = section;
        cardLayout.show(screenContainer, section);

        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            boolean active = entry.getKey().equals(section);
            JButton btn = entry.getValue();
            btn.setBackground(active ? NAV_ACTIVE_BG : SIDEBAR_BG);
            btn.setForeground(active ? NAV_TEXT_ACTIVE : NAV_TEXT);
        }
        setTitle("Property Manager — " + section);
    }

    // Screens (CardLayout deck)
    private JComponent buildScreens() {
        for (String section : SECTIONS) {
            screenContainer.add(buildPlaceholderScreen(section), section);
        }
        return screenContainer;
    }

//  Blank placeholder screen for now
    private JPanel buildPlaceholderScreen(String sectionName) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);

        JLabel label = new JLabel(sectionName + " screen — coming soon");
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 20f));
        label.setForeground(new Color(120, 120, 120));
        panel.add(label);

        return panel;
    }

    // Entry point
    public static void main(String[] args) {
        // Use the platform look and feel so it fits the OS it runs on.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fall back to the default cross-platform L&F if this fails.
        }

        SwingUtilities.invokeLater(() -> {
            PropertyManagerApp app = new PropertyManagerApp();
            app.setVisible(true);
        });
    }
}