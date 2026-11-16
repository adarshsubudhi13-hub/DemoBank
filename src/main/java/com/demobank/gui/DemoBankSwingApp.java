package com.demobank.gui;

import com.demobank.account.Account;
import com.demobank.account.AccountRepository;
import com.demobank.account.Transaction;
import com.demobank.auth.AuthService;
import com.demobank.auth.PasswordHasher;
import com.demobank.auth.SessionStore;
import com.demobank.auth.UserRepository;
import com.demobank.database.DatabaseManager;
import com.demobank.database.JdbcUserRepository;
import com.demobank.logging.AuthLogger;
import com.demobank.security.SecurityState;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

/**
 * Full-featured Java Swing Graphical User Interface (GUI) for DemoBank.
 * Demonstrates:
 * - GUI: Swing components (JFrame, JTabbedPane, JTable, JTextField, JPasswordField, JButton)
 * - OOP: Classes, Encapsulation, Polymorphism, Abstraction
 * - Advanced Java: Multithreading, Exception Handling, Collections, Streams
 * - Database: JDBC SQLite operations via JdbcUserRepository
 */
public class DemoBankSwingApp extends JFrame {

    private final AuthService authService;
    private final UserRepository userRepo;
    private final AccountRepository accountRepo;
    private final SessionStore sessionStore;
    private final SecurityState securityState;
    private final JdbcUserRepository jdbcUserRepo;

    // Login Components
    private JTextField txtUser;
    private JPasswordField txtPass;
    private JLabel lblLoginStatus;

    // Account Components
    private JLabel lblOwner;
    private JLabel lblBalance;
    private JLabel lblCard;
    private JTable txTable;
    private DefaultTableModel txModel;

    // Admin Components
    private JTable usersTable;
    private DefaultTableModel usersModel;

    public DemoBankSwingApp() {
        this.securityState = new SecurityState();
        this.userRepo = new UserRepository();
        this.accountRepo = new AccountRepository();
        this.sessionStore = new SessionStore();
        AuthLogger authLogger = new AuthLogger("data/auth.log");
        this.authService = new AuthService(userRepo, securityState, sessionStore, authLogger);
        this.jdbcUserRepo = new JdbcUserRepository(DatabaseManager.getInstance());

        initUI();
        seedJdbcUsers();
        refreshUsersTable();
    }

    private void initUI() {
        setTitle("DemoBank — Online Banking & Security Terminal (Java Swing GUI)");
        setSize(950, 680);
        setMinimumSize(new Dimension(800, 550));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        mainPanel.setBackground(new Color(248, 250, 252));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(11, 95, 255));
        header.setBorder(new EmptyBorder(14, 20, 14, 20));

        JLabel title = new JLabel("DEMOBANK SECURE CLIENT");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(Color.WHITE);

        JLabel sub = new JLabel("Target Endpoint: 127.0.0.1:9090 | JDBC SQLite Database Active");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(new Color(224, 231, 255));

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);
        titlePanel.add(title);
        titlePanel.add(sub);
        header.add(titlePanel, BorderLayout.WEST);

        mainPanel.add(header, BorderLayout.NORTH);

        // Tabbed Pane
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));

        tabs.addTab("🔑 User Sign In", createLoginPanel());
        tabs.addTab("💳 Account Portal", createAccountPanel());
        tabs.addTab("🛡️ Admin & JDBC Database (CRUD)", createAdminPanel());

        mainPanel.add(tabs, BorderLayout.CENTER);
        setContentPane(mainPanel);
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        // Quick fill chips
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
        chips.setBackground(new Color(241, 245, 249));
        chips.setBorder(BorderFactory.createTitledBorder("Click to Autofill Registered Credentials"));

        chips.add(createChip("adarsh", "Adarsh@123"));
        chips.add(createChip("alice", "Wonder@2024"));
        chips.add(createChip("bob", "Builder#77"));
        chips.add(createChip("charlie", "Choc0late!"));
        chips.add(createChip("admin", "Adm1n$ecure"));
        chips.add(createChip("guest", "guest123"));

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(chips, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1; gbc.gridx = 0;
        panel.add(new JLabel("Username:"), gbc);
        txtUser = new JTextField(15);
        gbc.gridx = 1;
        panel.add(txtUser, gbc);

        gbc.gridy = 2; gbc.gridx = 0;
        panel.add(new JLabel("Password:"), gbc);
        txtPass = new JPasswordField(15);
        gbc.gridx = 1;
        panel.add(txtPass, gbc);

        JButton btnLogin = new JButton("Sign In to DemoBank");
        btnLogin.setBackground(new Color(11, 95, 255));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnLogin.addActionListener(this::handleLogin);

        gbc.gridy = 3; gbc.gridx = 0; gbc.gridwidth = 2;
        panel.add(btnLogin, gbc);

        lblLoginStatus = new JLabel(" ");
        lblLoginStatus.setHorizontalAlignment(SwingConstants.CENTER);
        lblLoginStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        gbc.gridy = 4;
        panel.add(lblLoginStatus, gbc);

        return panel;
    }

    private JButton createChip(String user, String pass) {
        JButton b = new JButton(user);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        b.setFocusPainted(false);
        b.addActionListener(e -> {
            txtUser.setText(user);
            txtPass.setText(pass);
        });
        return b;
    }

    private JPanel createAccountPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel infoCard = new JPanel(new GridLayout(3, 1, 4, 4));
        infoCard.setBackground(new Color(238, 242, 255));
        infoCard.setBorder(new EmptyBorder(12, 16, 12, 16));

        lblOwner = new JLabel("Account Owner: Not Signed In");
        lblOwner.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblBalance = new JLabel("Current Balance: $0.00");
        lblBalance.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblBalance.setForeground(new Color(16, 185, 129));
        lblCard = new JLabel("Card: **** **** **** ****");

        infoCard.add(lblOwner);
        infoCard.add(lblBalance);
        infoCard.add(lblCard);
        panel.add(infoCard, BorderLayout.NORTH);

        // Transaction History Table
        String[] cols = {"Date", "Description", "Amount", "Running Balance"};
        txModel = new DefaultTableModel(cols, 0);
        txTable = new JTable(txModel);
        txTable.setRowHeight(24);
        panel.add(new JScrollPane(txTable), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createAdminPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topBar.setBackground(Color.WHITE);

        JButton btnRefresh = new JButton("🔄 Refresh JDBC Users");
        btnRefresh.addActionListener(e -> refreshUsersTable());

        JButton btnLock = new JButton("🔒 Lock Selected User");
        btnLock.addActionListener(e -> toggleLockUser(true));

        JButton btnUnlock = new JButton("🔓 Unlock Selected User");
        btnUnlock.addActionListener(e -> toggleLockUser(false));

        JButton btnDelete = new JButton("❌ Delete Selected User");
        btnDelete.addActionListener(this::handleDeleteUser);

        topBar.add(btnRefresh);
        topBar.add(btnLock);
        topBar.add(btnUnlock);
        topBar.add(btnDelete);
        panel.add(topBar, BorderLayout.NORTH);

        String[] cols = {"Username", "Full Name", "Account ID", "Locked Status", "Created Timestamp"};
        usersModel = new DefaultTableModel(cols, 0);
        usersTable = new JTable(usersModel);
        usersTable.setRowHeight(24);
        panel.add(new JScrollPane(usersTable), BorderLayout.CENTER);

        // SQL Query Executor Section
        JPanel sqlSection = new JPanel(new BorderLayout(6, 6));
        sqlSection.setBorder(BorderFactory.createTitledBorder("SQL Query Console (JDBC SQLite - demobank.db)"));

        JPanel sqlBar = new JPanel(new BorderLayout(6, 6));
        JTextField txtSql = new JTextField("SELECT username, full_name, account_id, is_locked FROM users");
        txtSql.setFont(new Font("Consolas", Font.PLAIN, 12));
        JButton btnExec = new JButton("▶️ Run SQL Query");
        sqlBar.add(new JLabel(" SQL: "), BorderLayout.WEST);
        sqlBar.add(txtSql, BorderLayout.CENTER);
        sqlBar.add(btnExec, BorderLayout.EAST);
        sqlSection.add(sqlBar, BorderLayout.NORTH);

        JLabel lblSqlOut = new JLabel(" Ready. Enter any SQL SELECT/UPDATE/INSERT/DELETE query to run via JDBC.");
        lblSqlOut.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSqlOut.setForeground(new Color(100, 116, 139));
        sqlSection.add(lblSqlOut, BorderLayout.SOUTH);

        btnExec.addActionListener(e -> {
            String query = txtSql.getText().trim();
            if (query.isEmpty()) return;
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 Statement stmt = conn.createStatement()) {
                boolean isRs = stmt.execute(query);
                if (isRs) {
                    try (ResultSet rs = stmt.getResultSet()) {
                        ResultSetMetaData md = rs.getMetaData();
                        int cc = md.getColumnCount();
                        Vector<String> colNames = new Vector<>();
                        for (int i = 1; i <= cc; i++) colNames.add(md.getColumnLabel(i));
                        Vector<Vector<Object>> data = new Vector<>();
                        while (rs.next()) {
                            Vector<Object> row = new Vector<>();
                            for (int i = 1; i <= cc; i++) row.add(rs.getObject(i));
                            data.add(row);
                        }
                        usersModel.setDataVector(data, colNames);
                        lblSqlOut.setText("SQL Query returned " + data.size() + " row(s) successfully.");
                        lblSqlOut.setForeground(new Color(16, 185, 129));
                    }
                } else {
                    int aff = stmt.getUpdateCount();
                    lblSqlOut.setText("SQL Statement affected " + aff + " row(s).");
                    lblSqlOut.setForeground(new Color(11, 95, 255));
                    refreshUsersTable();
                }
            } catch (SQLException ex) {
                lblSqlOut.setText("SQL Error: " + ex.getMessage());
                lblSqlOut.setForeground(new Color(225, 29, 72));
            }
        });

        panel.add(sqlSection, BorderLayout.SOUTH);

        return panel;
    }

    private void handleLogin(ActionEvent e) {
        String u = txtUser.getText().trim();
        String p = new String(txtPass.getPassword());

        AuthService.LoginResult res = authService.login(u, p, "127.0.0.1");
        if (res.status() == 200) {
            lblLoginStatus.setForeground(new Color(16, 185, 129));
            lblLoginStatus.setText("Authentication Successful! Session: " + (res.token() != null ? res.token().substring(0, Math.min(12, res.token().length())) : "active") + "...");

            // Load account
            userRepo.findByUsername(u).ifPresent(record -> {
                Optional<Account> accOpt = accountRepo.findById(record.accountId());
                accOpt.ifPresent(acc -> {
                    lblOwner.setText("Account Owner: " + acc.owner());
                    lblBalance.setText(String.format("Current Balance: $%,.2f", acc.balance()));
                    lblCard.setText("Masked Card: " + acc.maskedNumber());

                    txModel.setRowCount(0);
                    for (Transaction tx : acc.transactions()) {
                        txModel.addRow(new Object[]{tx.date(), tx.description(), String.format("$%.2f", tx.amount()), String.format("$%.2f", tx.balance())});
                    }
                });
            });
        } else {
            lblLoginStatus.setForeground(new Color(225, 29, 72));
            lblLoginStatus.setText("Authentication Failed (HTTP " + res.status() + "): " + res.message());
        }
    }

    private void seedJdbcUsers() {
        try {
            if (jdbcUserRepo.count() == 0) {
                String[][] seeds = {
                        {"adarsh", "salt_adarsh", "Adarsh@123", "Adarsh Subudhi", "ACC-1006"},
                        {"alice", "salt_alice", "Wonder@2024", "Alice Johnson", "ACC-1001"},
                        {"bob", "salt_bob", "Builder#77", "Bob Smith", "ACC-1002"},
                        {"charlie", "salt_charlie", "Choc0late!", "Charlie Brown", "ACC-1003"},
                        {"admin", "salt_admin", "Adm1n$ecure", "Admin User", "ACC-1004"},
                        {"guest", "salt_guest", "guest123", "Guest User", "ACC-1005"}
                };
                for (String[] s : seeds) {
                    String hash = PasswordHasher.hash(s[2], s[1]);
                    jdbcUserRepo.save(new JdbcUserRepository.DbUser(s[0], s[1], hash, s[3], s[4], false, System.currentTimeMillis()));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void refreshUsersTable() {
        try {
            List<JdbcUserRepository.DbUser> list = jdbcUserRepo.findAll();
            usersModel.setRowCount(0);
            for (JdbcUserRepository.DbUser u : list) {
                usersModel.addRow(new Object[]{u.username(), u.fullName(), u.accountId(), u.locked() ? "LOCKED" : "ACTIVE", u.createdAt()});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void toggleLockUser(boolean lock) {
        int row = usersTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a user first.", "Info", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String user = (String) usersModel.getValueAt(row, 0);
        try {
            jdbcUserRepo.updateLockStatus(user, lock);
            securityState.lockUser(user, 3600);
            refreshUsersTable();
            JOptionPane.showMessageDialog(this, "User " + user + " is now " + (lock ? "LOCKED" : "UNLOCKED"));
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void handleDeleteUser(ActionEvent ev) {
        int row = usersTable.getSelectedRow();
        if (row < 0) return;
        String user = (String) usersModel.getValueAt(row, 0);
        try {
            jdbcUserRepo.deleteByUsername(user);
            refreshUsersTable();
            JOptionPane.showMessageDialog(this, "User " + user + " deleted from JDBC database.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DemoBankSwingApp app = new DemoBankSwingApp();
            app.setVisible(true);
        });
    }
}
