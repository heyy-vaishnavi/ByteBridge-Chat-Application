
import common.Message;
import db.DatabaseManager;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List; 
import javax.swing.*;
import javax.swing.text.*; 

public class Server extends JFrame {
    private JTextField portField, messageField;
    private JButton startButton, broadcastButton;
    private JTextPane chatPane;
    private StyledDocument doc;
    private ServerSocket serverSocket;
    private final Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> onlineUsers = Collections.synchronizedSet(new HashSet<>());
    
    public Server() {
        DatabaseManager.initialize();

        // Enhanced UI Setup
        setTitle("ChatNest Server Control");
        setSize(900, 650);  // Slightly larger window
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(5, 5));
        getContentPane().setBackground(new Color(245, 248, 250));

        // Improved Top Panel
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        topPanel.setBackground(new Color(230, 240, 250));
        
        // Port Field with better styling
        topPanel.add(new JLabel("Port:"));
        portField = new JTextField("12345", 10);
        portField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        portField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        topPanel.add(portField);
        
        // Start Button with modern style (KEEPING FUNCTIONALITY)
        startButton = new JButton("Start Server");
        startButton.setBackground(new Color(76, 175, 80)); // Green
        startButton.setForeground(Color.WHITE);
        startButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        startButton.setFocusPainted(false);
        topPanel.add(startButton);
        
        add(topPanel, BorderLayout.NORTH);

        // Enhanced Chat Area
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setBackground(Color.WHITE);
        chatPane.setFont(new Font("Consolas", Font.PLAIN, 13));
        chatPane.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        doc = chatPane.getStyledDocument();
        
        JScrollPane scrollPane = new JScrollPane(chatPane);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        add(scrollPane, BorderLayout.CENTER);

        // Improved Bottom Panel
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        
        messageField = new JTextField();
        messageField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        // Broadcast Button with modern style (KEEPING FUNCTIONALITY)
        broadcastButton = new JButton("Broadcast");
        broadcastButton.setBackground(new Color(70, 130, 180)); // Blue
        broadcastButton.setForeground(Color.WHITE);
        broadcastButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        broadcastButton.setFocusPainted(false);
        
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(broadcastButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // KEEP ALL ORIGINAL EVENT HANDLERS
        startButton.addActionListener(e -> startServer());
        broadcastButton.addActionListener(e -> {
            String msg = messageField.getText().trim();
            if (!msg.isEmpty()) {
                Message serverMsg = new Message(Message.Type.TEXT, "Server", msg);
                broadcastMessage(serverMsg, true);
                DatabaseManager.saveMessage("Server", msg);
                messageField.setText("");
            }
        });
    }


    private void startServer() {
        int port = Integer.parseInt(portField.getText().trim());
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                appendStyledMessage("✅ Server started on port " + port, true);

                while (true) {
                    Socket socket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(socket);
                    new Thread(handler).start();
                }
            } catch (IOException e) {
                appendStyledMessage("❌ Error: " + e.getMessage(), false);
            }
        }).start();
    }
    
   private void broadcastMessage(Message message, boolean fromServer) {
        boolean isPrivate = message.isPrivate();
        
        // Server console display logic
        if (fromServer || !isPrivate) {
            String displayText = "[" + getCurrentTime() + "] " + message.getSender() + 
                        (isPrivate ? " → " + message.getRecipient() : "") + 
                        ": " + message.getContent();
            appendStyledMessage(displayText, fromServer);
        }
        
        // Message distribution to clients
        synchronized (clients) {
            for (ClientHandler c : clients) {
                if (!isPrivate) {
                    c.sendMessage(message); // Broadcast to all
                } else if (c.username.equals(message.getRecipient()) || 
                        c.username.equals(message.getSender())) {
                    // Only send to involved parties in private chats
                    c.sendMessage(message);
                }
            }
        }
    }
        
        // Message distribution to clients
       

    private void broadcastOnlineUsers() {
        String csv = String.join(",", onlineUsers);
        Message userListMsg = new Message(Message.Type.ONLINE_USERS, "Server", csv);
        synchronized (clients) {
            for (ClientHandler c : clients) {
                c.sendMessage(userListMsg);
            }
        }
    }

    private void appendStyledMessage(String message, boolean alignRight) {
        SimpleAttributeSet set = new SimpleAttributeSet();
        StyleConstants.setAlignment(set, alignRight ? StyleConstants.ALIGN_RIGHT : StyleConstants.ALIGN_LEFT);
        StyleConstants.setFontSize(set, 13);
        doc.setParagraphAttributes(doc.getLength(), 1, set, false);
        try {
            doc.insertString(doc.getLength(), message + "\n", set);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private String getCurrentTime() {
    return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
}

    class ClientHandler implements Runnable {
        private final Socket socket;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private String username;

        public ClientHandler(Socket socket) { this.socket = socket; }

        public void sendMessage(Message msg) {
            try {
                if (out != null) {
                    out.writeObject(msg);
                    out.flush();
                }
            } catch (IOException e) {
                appendStyledMessage("❌ Failed to send message to " + username, false);
            }
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                boolean authenticated = false;

                Message msg;
                while ((msg = (Message) in.readObject()) != null) {
                    switch (msg.getType()) {
                        case LOGIN:
                            String uname = msg.getSender();
                            String pass = msg.getContent();
                            boolean success = new DatabaseManager().loginUser(uname, pass);
                            if (success) {
                                username = uname;
                                authenticated = true;
                                clients.add(this);
                                onlineUsers.add(username);

                                out.writeObject(new Message(Message.Type.TEXT, "Server", "LOGIN_SUCCESS"));
                                appendStyledMessage("✅ " + username + " logged in.", false);

                                broadcastOnlineUsers();

                                List<String> history = DatabaseManager.getLastMessages(20);
                                for (String line : history) {
                                    Message historyMsg = new Message(Message.Type.CHAT_HISTORY, "Server", line);
                                    sendMessage(historyMsg);
                                }

                            } else {
                                out.writeObject(new Message(Message.Type.TEXT, "Server", "LOGIN_FAIL"));
                                socket.close();
                                return;
                            }
                            break;

                        case REGISTER:
                            uname = msg.getSender();
                            pass = msg.getContent();
                            boolean regSuccess = new DatabaseManager().registerUser(uname, pass);
                            if (regSuccess) {
                                out.writeObject(new Message(Message.Type.TEXT, "Server", "REGISTER_SUCCESS"));
                            } else {
                                out.writeObject(new Message(Message.Type.TEXT, "Server", "REGISTER_FAIL"));
                            }
                            break;

                        case CHAT_HISTORY:
                            String otherUser = msg.getContent();
                            try {
                                if ("ALL".equals(otherUser)) {
                                        List<Message> history = DatabaseManager.getBroadcastMessagesAsMessages(20);
                                        for (Message m : history) {
                                            sendMessage(m); // already marked as CHAT_HISTORY
                                        }
                                    } else {
                                        List<Message> history = DatabaseManager.getPrivateMessagesAsMessages(username, otherUser, 20);
                                        for (Message m : history) {
                                            sendMessage(m); // already marked as CHAT_HISTORY and private
                                        }
}
                            } catch (Exception e) {
                                System.err.println("History load failed: " + e.getMessage());
                            }
                            break;

                        // In ClientHandler.run():
                            case TEXT:
                                if (authenticated) {
                                    msg.setSender(username);
                                    try {
                                        // Save to database
                                        DatabaseManager.saveMessage(
                                            username, 
                                            msg.getContent(), 
                                            msg.getRecipient()  // NULL for broadcasts
                                        );
                                        
                                        // Add timestamp to message content
                                        String timestampedContent = "[" + getCurrentTime() + "] " + msg.getContent();
                                        msg.setContent(timestampedContent);
                                        
                                        broadcastMessage(msg, false);
                                    } catch (Exception e) {
                                        System.err.println("❌ Failed to save message: " + e.getMessage());
                                    }
                                }
                                break;

                        case FILE:
                            if (authenticated) {
                                receiveFile(msg);
                                broadcastMessage(msg, false);
                            }
                            break;

                        default:
                            appendStyledMessage("❓ Unknown message type received.", false);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                appendStyledMessage("ℹ️ " + (username != null ? username : "Client") + " disconnected.", false);
            } finally {
                try {
                    clients.remove(this);
                    if (username != null) onlineUsers.remove(username);
                    broadcastOnlineUsers();
                    socket.close();
                } catch (IOException ignored) {}
            }
        }

        private void receiveFile(Message msg) {
            try {
                new File("server_received_files").mkdirs();
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String fileName = "[" + username + "_" + timestamp + "]_" + msg.getFileName();
                File receivedFile = new File("server_received_files", fileName);
                FileOutputStream fos = new FileOutputStream(receivedFile);
                fos.write(msg.getFileData());
                fos.close();
                appendStyledMessage("File received: " + receivedFile.getPath(), false);
            } catch (IOException e) {
                appendStyledMessage("File receive error: " + e.getMessage(), false);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Server().setVisible(true));
    }
}