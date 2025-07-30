package db;

import common.Message;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:data/chatapp.db";

    private static Connection connect() throws SQLException {
    return DriverManager.getConnection(DB_URL);
}


     public static void initialize() {
        new java.io.File("data").mkdirs(); // Ensure directory exists
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            
            // Create fresh tables with proper schema
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS messages (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "sender TEXT NOT NULL," +
                "recipient TEXT," +  // NULL = broadcast message
                "content TEXT NOT NULL," +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)"
            );

            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT UNIQUE NOT NULL," +
                "password_hash TEXT NOT NULL)"
            );

            System.out.println("✅ Database initialized with fresh schema");

        } catch (SQLException e) {
            System.err.println("❌ Database initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }


    

    public static List<String> getLastMessages(int limit) {
        List<String> messages = new ArrayList<>();
        String sql = "SELECT sender, recipient, content, timestamp FROM messages ORDER BY id DESC LIMIT ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String sender = rs.getString("sender");
                String recipient = rs.getString("recipient");
                String content = rs.getString("content");
                String ts = rs.getString("timestamp");
                if (recipient == null || recipient.isEmpty()) {
                    messages.add("[" + ts + "] " + sender + ": " + content);
                } else {
                    messages.add("[" + ts + "] (Private) " + sender + " ➜ " + recipient + ": " + content);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Collections.reverse(messages);
        return messages;
    }

    public static List<String> getBroadcastMessages(int limit) {
        List<String> messages = new ArrayList<>();
        String sql = "SELECT sender, content, timestamp FROM messages WHERE recipient IS NULL ORDER BY id DESC LIMIT ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String sender = rs.getString("sender");
                String content = rs.getString("content");
                String ts = rs.getString("timestamp");
                messages.add("[" + ts + "] " + sender + ": " + content);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Collections.reverse(messages);
        return messages;
    }

    public static List<String> getPrivateMessages(String userA, String userB, int limit) {
        List<String> messages = new ArrayList<>();
        String sql = "SELECT sender, recipient, content, timestamp FROM messages " +
                     "WHERE (sender = ? AND recipient = ?) OR (sender = ? AND recipient = ?) " +
                     "ORDER BY id DESC LIMIT ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userA);
            pstmt.setString(2, userB);
            pstmt.setString(3, userB);
            pstmt.setString(4, userA);
            pstmt.setInt(5, limit);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String sender = rs.getString("sender");
                String content = rs.getString("content");
                String ts = rs.getString("timestamp");
                messages.add("[" + ts + "] " + sender + ": " + content);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Collections.reverse(messages);
        return messages;
    }

    public static void saveMessage(String sender, String content) {
        saveMessage(sender, content, null);
    }

    public static List<Message> getPrivateMessagesAsMessages(String user1, String user2, int limit) {
    List<Message> messages = new ArrayList<>();
    try (Connection conn = connect();
         PreparedStatement stmt = conn.prepareStatement(
            "SELECT sender, recipient, content, timestamp FROM messages " +
            "WHERE ((sender=? AND recipient=?) OR (sender=? AND recipient=?)) AND recipient IS NOT NULL " +
            "ORDER BY timestamp DESC LIMIT ?")) {
        
        stmt.setString(1, user1);
        stmt.setString(2, user2);
        stmt.setString(3, user2);
        stmt.setString(4, user1);
        stmt.setInt(5, limit);

        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            String sender = rs.getString("sender");
            String recipient = rs.getString("recipient");
            String content = rs.getString("content");
            Timestamp ts = rs.getTimestamp("timestamp");
            String formatted = "[" + ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "] " + content;
            Message msg = new Message(Message.Type.CHAT_HISTORY, sender, formatted, recipient);
            msg.setPrivate(true);
            messages.add(0, msg); // reverse order to get oldest first
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return messages;
}

public static List<Message> getBroadcastMessagesAsMessages(int limit) {
    List<Message> messages = new ArrayList<>();
    try (Connection conn = connect();
         PreparedStatement stmt = conn.prepareStatement(
            "SELECT sender, content, timestamp FROM messages "  +
            "WHERE recipient IS NULL ORDER BY timestamp DESC LIMIT ?")) {

        stmt.setInt(1, limit);
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            String sender = rs.getString("sender");
            String content = rs.getString("content");
            Timestamp ts = rs.getTimestamp("timestamp");
            String formatted = "[" + ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "] " + content;
            Message msg = new Message(Message.Type.CHAT_HISTORY, sender, formatted);
            msg.setPrivate(false);
            messages.add(0, msg); // reverse order
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return messages;
}



    public static void saveMessage(String sender, String content, String recipient) {
    String sql = "INSERT INTO messages (sender, recipient, content, timestamp) VALUES (?, ?, ?, datetime('now'))";
    try (Connection conn = DriverManager.getConnection(DB_URL);
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, sender);
        pstmt.setString(2, recipient);
        pstmt.setString(3, content);
        pstmt.executeUpdate();
        System.out.println("Message saved to DB: " + sender + " -> " + 
                         (recipient != null ? recipient : "ALL") + ": " + content);
    } catch (SQLException e) {
        System.err.println("DB Error: " + e.getMessage());
        e.printStackTrace();
    }
}

    public boolean registerUser(String username, String password) {
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            System.out.println("✅ User registered: " + username);
            return true;
        } catch (SQLException e) {
            System.out.println("❌ Registration failed: " + e.getMessage());
            return false;
        }
    }

    public boolean loginUser(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password_hash = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            boolean exists = rs.next();
            System.out.println(exists ? "✅ Login successful: " + username : "❌ Login failed for: " + username);
            return exists;
        } catch (SQLException e) {
            System.out.println("❌ Login failed: " + e.getMessage());
            return false;
        }
    }
}

