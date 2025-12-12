import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class App extends JFrame {
    private Connection conn;
    private int currentUserId;
    private String currentRole;

    private CardLayout card = new CardLayout();
    private JPanel mainPanel = new JPanel(card);

    public App() {
        setTitle("E-Learning Platform");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        try {
            conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/elearning?useSSL=false&serverTimezone=UTC",
                "root", ""); // update password if needed
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "DB Error: " + e.getMessage());
            System.exit(1);
        }

        mainPanel.add(buildLoginPanel(), "LOGIN");
        mainPanel.add(buildStudentPanel(), "STUDENT");
        mainPanel.add(buildTeacherPanel(), "TEACHER");
        mainPanel.add(buildAdminPanel(), "ADMIN");

        add(mainPanel);
        card.show(mainPanel, "LOGIN");
    }

    // ---------------- LOGIN ----------------
    private JPanel buildLoginPanel() {
        JPanel panel = new JPanel(new GridLayout(4,2,10,10));
        panel.setBackground(new Color(230, 240, 255));

        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JButton loginBtn = new JButton("Login");
        loginBtn.setBackground(new Color(0, 102, 204));
        loginBtn.setForeground(Color.WHITE);
        JLabel msg = new JLabel("", JLabel.CENTER);

        panel.add(new JLabel("Username:"));
        panel.add(userField);
        panel.add(new JLabel("Password:"));
        panel.add(passField);
        panel.add(new JLabel(""));
        panel.add(loginBtn);
        panel.add(msg);

        loginBtn.addActionListener(e -> {
            try {
                PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM users WHERE username=? AND password=?");
                ps.setString(1, userField.getText().trim());
                ps.setString(2, new String(passField.getPassword()).trim());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    currentUserId = rs.getInt("user_id");
                    currentRole = rs.getString("role");
                    msg.setText("Welcome " + rs.getString("name") + " (" + currentRole + ")");
                    card.show(mainPanel, currentRole);
                } else {
                    msg.setText("Invalid credentials.");
                }
            } catch (Exception ex) {
                msg.setText("Error: " + ex.getMessage());
            }
        });
        return panel;
    }

    // ---------------- STUDENT PANEL ----------------
    private JTabbedPane buildStudentPanel() {
        JTabbedPane tabs = new JTabbedPane();

        // All courses
        JPanel allCoursesPanel = new JPanel(new BorderLayout());
        JTextArea courseArea = new JTextArea();
        courseArea.setEditable(false);
        courseArea.setBackground(new Color(245, 255, 245));
        JButton enrollBtn = new JButton("Enroll in Course");
        enrollBtn.setBackground(new Color(0, 153, 76));
        enrollBtn.setForeground(Color.WHITE);

        allCoursesPanel.add(new JScrollPane(courseArea), BorderLayout.CENTER);
        allCoursesPanel.add(enrollBtn, BorderLayout.SOUTH);

        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM courses");
            while (rs.next()) {
                courseArea.append(rs.getInt("course_id") + " | " +
                        rs.getString("title") + " | " +
                        rs.getString("description") + "\n");
            }
        } catch (Exception e) {
            courseArea.setText("Error loading courses: " + e.getMessage());
        }

        enrollBtn.addActionListener(e -> {
            String courseId = JOptionPane.showInputDialog(this, "Enter Course ID to enroll:");
            try {
                PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO enrollments(user_id, course_id) VALUES (?,?)");
                ps.setInt(1, currentUserId);
                ps.setInt(2, Integer.parseInt(courseId));
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Enrolled successfully!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        // My courses
        JPanel myCoursesPanel = new JPanel(new BorderLayout());
        JTextArea myCoursesArea = new JTextArea();
        myCoursesArea.setEditable(false);
        myCoursesArea.setBackground(new Color(255, 255, 204));
        JButton refreshBtn = new JButton("Refresh My Courses");
        refreshBtn.setBackground(new Color(255, 153, 51));

        myCoursesPanel.add(new JScrollPane(myCoursesArea), BorderLayout.CENTER);
        myCoursesPanel.add(refreshBtn, BorderLayout.SOUTH);

        refreshBtn.addActionListener(e -> {
            myCoursesArea.setText("");
            try {
                PreparedStatement ps = conn.prepareStatement(
                    "SELECT c.course_id, c.title, c.description " +
                    "FROM enrollments e JOIN courses c ON e.course_id=c.course_id " +
                    "WHERE e.user_id=?");
                ps.setInt(1, currentUserId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    myCoursesArea.append(rs.getInt("course_id") + " | " +
                            rs.getString("title") + " | " +
                            rs.getString("description") + "\n");
                }
            } catch (Exception ex) {
                myCoursesArea.setText("Error loading your courses: " + ex.getMessage());
            }
        });

        tabs.addTab("All Courses", allCoursesPanel);
        tabs.addTab("My Courses", myCoursesPanel);

        return tabs;
    }

    // ---------------- TEACHER PANEL ----------------
    private JPanel buildTeacherPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JTextArea courseArea = new JTextArea();
        courseArea.setEditable(false);
        courseArea.setBackground(new Color(240, 255, 255));
        JButton addCourseBtn = new JButton("Add Course");
        addCourseBtn.setBackground(new Color(102, 0, 204));
        addCourseBtn.setForeground(Color.WHITE);

        panel.add(new JScrollPane(courseArea), BorderLayout.CENTER);
        panel.add(addCourseBtn, BorderLayout.SOUTH);

        addCourseBtn.addActionListener(e -> {
            String title = JOptionPane.showInputDialog(this, "Course Title:");
            String desc = JOptionPane.showInputDialog(this, "Description:");
            try {
                PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO courses(title, description, teacher_id) VALUES (?,?,?)");
                ps.setString(1, title);
                ps.setString(2, desc);
                ps.setInt(3, currentUserId);
                ps.executeUpdate();
                courseArea.append("Added: " + title + "\n");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        return panel;
    }

    // ---------------- ADMIN PANEL ----------------
    private JPanel buildAdminPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JTextArea userArea = new JTextArea();
        userArea.setEditable(false);
        userArea.setBackground(new Color(255, 240, 245));
        JButton listUsersBtn = new JButton("List Users");
        listUsersBtn.setBackground(new Color(204, 0, 0));
        listUsersBtn.setForeground(Color.WHITE);

        panel.add(new JScrollPane(userArea), BorderLayout.CENTER);
        panel.add(listUsersBtn, BorderLayout.SOUTH);

        listUsersBtn.addActionListener(e -> {
            try {
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT * FROM users");
                userArea.setText("");
                while (rs.next()) {
                    userArea.append(rs.getInt("user_id") + " | " +
                            rs.getString("username") + " | " +
                            rs.getString("role") + " | " +
                            rs.getString("email") + "\n");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        return panel;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new App().setVisible(true));
    }
}
