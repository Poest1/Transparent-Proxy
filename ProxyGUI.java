import javax.swing.*;
import java.awt.*;
import java.io.*;

public class ProxyGUI extends JFrame {
    private JMenuBar menuBar;
    private JTextArea logArea;

    public ProxyGUI() {
        setTitle("Transparent Proxy Control Panel");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        initComponents();
    }

    private void initComponents() {
        menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem startHttpItem = new JMenuItem("Start HTTP Proxy");
        startHttpItem.addActionListener(e -> startHttpProxy());
        JMenuItem stopHttpItem = new JMenuItem("Stop HTTP Proxy");
        stopHttpItem.addActionListener(e -> stopHttpProxy());
        JMenuItem startHttpsItem = new JMenuItem("Start HTTPS Proxy");
        startHttpsItem.addActionListener(e -> startHttpsProxy());
        JMenuItem stopHttpsItem = new JMenuItem("Stop HTTPS Proxy");
        stopHttpsItem.addActionListener(e -> stopHttpsProxy());
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(startHttpItem);
        fileMenu.add(stopHttpItem);
        fileMenu.add(startHttpsItem);
        fileMenu.add(stopHttpsItem);
        fileMenu.add(new JSeparator());
        fileMenu.add(exitItem);

        JMenu reportMenu = new JMenu("Report");
        JMenuItem generateItem = new JMenuItem("Generate Report");
        generateItem.addActionListener(e -> generateReport());

        reportMenu.add(generateItem);

        JMenu filterMenu = new JMenu("Filter");
        JMenuItem addHostItem = new JMenuItem("Add Host to Filter");
        addHostItem.addActionListener(e -> addHostToFilter());
        JMenuItem displayHostsItem = new JMenuItem("Display Current Filtered Hosts");
        displayHostsItem.addActionListener(e -> displayFilteredHosts());

        filterMenu.add(addHostItem);
        filterMenu.add(displayHostsItem);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAboutDialog());

        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(reportMenu);
        menuBar.add(filterMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void startHttpProxy() {
        logArea.append("HTTP Proxy Server Started.\n");
        ProxyServer.main(new String[0]);
    }

    private void stopHttpProxy() {
        logArea.append("HTTP Proxy Server Stopped.\n");
        ProxyServer.stopServer();
    }

    private void startHttpsProxy() {
        logArea.append("HTTPS Proxy Server Started.\n");
        HTTPSProxyServer.startHttpsProxyServer();
    }

    private void stopHttpsProxy() {
        logArea.append("HTTPS Proxy Server Stopped.\n");
        HTTPSProxyServer.stopServer();
    }

    private void generateReport() {
        logArea.append("Generating report...\n");
        StringBuilder report = new StringBuilder();

        report.append("HTTP Proxy Filtered Domains:\n");
        for (String domain : ProxyServer.filteredDomains) {
            report.append(domain).append("\n");
        }

        report.append("\nHTTPS Proxy Filtered Domains:\n");
        for (String domain : HTTPSProxyServer.filteredDomains) {
            report.append(domain).append("\n");
        }

        // Optionally, add cache information here

        try (FileWriter writer = new FileWriter("/Users/berk/Desktop/471_final/src/proxy_report.txt")) {
            writer.write(report.toString());
            logArea.append("Report saved to proxy_report.txt\n");
        } catch (IOException e) {
            logArea.append("Error generating report: " + e.getMessage() + "\n");
        }
    }

    private void addHostToFilter() {
        String host = JOptionPane.showInputDialog(this, "Enter host to filter:");
        if (host != null && !host.trim().isEmpty()) {
            try (FileWriter fw = new FileWriter("/Users/berk/Desktop/471_final/src/filtered_domains.txt", true);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter out = new PrintWriter(bw)) {
                out.println(host.trim());
                ProxyServer.loadFilteredDomains();
                HTTPSProxyServer.loadFilteredDomains(); // Update the in-memory filter list
                logArea.append("Host added to filter list: " + host + "\n");
            } catch (IOException e) {
                logArea.append("Error adding host to filter list: " + e.getMessage() + "\n");
            }
        }
    }

    private void displayFilteredHosts() {
        logArea.append("Displaying filtered hosts:\n");

        logArea.append("HTTP Proxy Filtered Hosts:\n");
        for (String host : ProxyServer.filteredDomains) {
            logArea.append(host + "\n");
        }

        logArea.append("\nHTTPS Proxy Filtered Hosts:\n");
        for (String host : HTTPSProxyServer.filteredDomains) {
            logArea.append(host + "\n");
        }
    }

    private void showAboutDialog() {
        JOptionPane.showMessageDialog(this,
                "Transparent Proxy Control Panel\nVersion 1.0\nDeveloped by Berk",
                "About", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ProxyGUI().setVisible(true);
        });
    }
}
