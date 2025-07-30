
import common.Message;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.*;
import javax.swing.text.*;


public class Client extends JFrame {
    private JTextField hostField, portField, usernameField, messageField;
    private JPasswordField passwordField;
    private JButton loginButton, registerButton, sendButton, fileButton;
    private JTextPane chatPane;
    private StyledDocument doc;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username;

    private DefaultListModel<String> userListModel = new DefaultListModel<>();
    private JList<String> userList = new JList<>(userListModel);
    
    private Set<String> loadedMessageIds = new HashSet<>();
    private String lastHistoryRequest = "";

    private String currentChat = "ALL";
    private Map<String, List<String>> chatHistories = new HashMap<>();
    private boolean isInitialLoad = true;


    public Client() {
        initializeUI();
        setupEventListeners();
    }

    private void initializeUI() {
    setTitle("ChatNest - Secure Messaging & File Sharing");
    setSize(1100, 500);
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setLayout(new BorderLayout(5, 5));
    getContentPane().setBackground(new Color(240, 245, 250));

    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
        e.printStackTrace();
    }

    // Top Panel with Gradient Background
    JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10)) {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            int w = getWidth();
            int h = getHeight();
            GradientPaint gp = new GradientPaint(0, 0, new Color(70, 130, 180), w, 0, new Color(100, 149, 237));
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, w, h);
        }
    };
    topPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
    
    // Styled input fields
    addStyledInputField(topPanel, "Host:", hostField = createStyledTextField("localhost", 12));
    addStyledInputField(topPanel, "Port:", portField = createStyledTextField("12345", 5));
    addStyledInputField(topPanel, "Username:", usernameField = createStyledTextField("", 12));
    addStyledInputField(topPanel, "Password:", passwordField = createStyledPasswordField(12));
    
    // Custom buttons
    loginButton = createGradientButton("Login", new Color(76, 175, 80), new Color(56, 142, 60));
    registerButton = createGradientButton("Register", new Color(33, 150, 243), new Color(25, 118, 210));
    
    topPanel.add(loginButton);
    topPanel.add(registerButton);
    add(topPanel, BorderLayout.NORTH);

    // User list with custom styling
    userList.setBackground(new Color(245, 245, 245));
    userList.setSelectionBackground(new Color(100, 149, 237));
    userList.setSelectionForeground(Color.WHITE);
    userList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    userList.setFixedCellHeight(30);
    
    JScrollPane userScrollPane = new JScrollPane(userList);
    userScrollPane.setPreferredSize(new Dimension(180, 0));
    userScrollPane.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createEmptyBorder(5, 5, 5, 5),
        BorderFactory.createLineBorder(new Color(220, 220, 220))
    ));
    add(userScrollPane, BorderLayout.WEST);

    // Chat pane with better styling
    chatPane = new JTextPane();
    chatPane.setEditable(false);
    chatPane.setBackground(new Color(255, 253, 250));
    chatPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    chatPane.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
    doc = chatPane.getStyledDocument();
    
    JScrollPane chatScrollPane = new JScrollPane(chatPane);
    chatScrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
    add(chatScrollPane, BorderLayout.CENTER);

    // Bottom panel with message area
    JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
    bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
    bottomPanel.setBackground(new Color(240, 245, 250));
    
    messageField = new JTextField();
    messageField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    messageField.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(200, 200, 200)),
        BorderFactory.createEmptyBorder(8, 10, 8, 10))
    );
    
    sendButton = createGradientButton("Send", new Color(100, 149, 237), new Color(70, 130, 180));
    fileButton = createGradientButton("Attach File", new Color(255, 152, 0), new Color(245, 124, 0));
    
    JPanel buttons = new JPanel(new GridLayout(1, 2, 5, 0));
    buttons.setOpaque(false);
    buttons.add(fileButton);
    buttons.add(sendButton);
    buttons.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

    bottomPanel.add(messageField, BorderLayout.CENTER);
    bottomPanel.add(buttons, BorderLayout.EAST);
    add(bottomPanel, BorderLayout.SOUTH);

    sendButton.setEnabled(false);
    fileButton.setEnabled(false);
    
    // Add some padding around the main content
    ((JComponent)getContentPane()).setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
}

// Helper methods for creating styled components
private JTextField createStyledTextField(String text, int columns) {
    JTextField field = new JTextField(text, columns);
    field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    field.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(200, 200, 200)),
        BorderFactory.createEmptyBorder(5, 8, 5, 8))
    );
    return field;
}

private JPasswordField createStyledPasswordField(int columns) {
    JPasswordField field = new JPasswordField(columns);
    field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    field.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(200, 200, 200)),
        BorderFactory.createEmptyBorder(5, 8, 5, 8))
    );
    return field;
}

private JButton createGradientButton(String text, Color topColor, Color bottomColor) {
    JButton button = new JButton(text) {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int w = getWidth();
            int h = getHeight();
            
            GradientPaint gp = new GradientPaint(0, 0, topColor, 0, h, bottomColor);
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, w, h, 8, 8);
            
            g2.setColor(getForeground());
            g2.setFont(getFont().deriveFont(Font.BOLD));
            FontMetrics fm = g2.getFontMetrics();
            Rectangle2D r = fm.getStringBounds(getText(), g2);
            int x = (w - (int) r.getWidth()) / 2;
            int y = (h - (int) r.getHeight()) / 2 + fm.getAscent();
            g2.drawString(getText(), x, y);
            g2.dispose();
        }
        
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(super.getPreferredSize().width, 35);
        }
    };
    
    button.setContentAreaFilled(false);
    button.setBorderPainted(false);
    button.setFocusPainted(false);
    button.setForeground(Color.WHITE);
    button.setFont(new Font("Segoe UI", Font.BOLD, 14));
    button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    
    return button;
}

private void addStyledInputField(JPanel panel, String labelText, JComponent field) {
    JLabel label = new JLabel(labelText);
    label.setFont(new Font("Segoe UI", Font.BOLD, 14));
    label.setForeground(Color.WHITE);
    panel.add(label);
    panel.add(field);
}

    private void addInputField(JPanel panel, String label, JTextField field) {
        panel.add(new JLabel(label));
        panel.add(field);
    }

    private void setupEventListeners() {
    loginButton.addActionListener(e -> connectAndAuth("LOGIN"));
    registerButton.addActionListener(e -> connectAndAuth("REGISTER"));
    sendButton.addActionListener(e -> sendMessage());
    fileButton.addActionListener(e -> sendFile());

    userList.addMouseListener(new MouseAdapter() {
        public void mouseClicked(MouseEvent evt) {
            if (evt.getClickCount() == 1) {
                String selectedUser = userList.getSelectedValue();
                if (selectedUser != null && !selectedUser.equals(username)) {
                    if (!selectedUser.equals(currentChat)) {  // Only switch if different chat
                        switchToChat(selectedUser);
                    }
                } else if ("ALL".equals(selectedUser) && !"ALL".equals(currentChat)) {
                    switchToChat("ALL");
                }
            }
        }
    });
}

    private void updateSendButtonsState() {
    boolean enabled = !socket.isClosed() && username != null;
    sendButton.setEnabled(enabled);
    fileButton.setEnabled(enabled);
}

        private void switchToChat(String targetUser) {
    // Store the current scroll position if you want to preserve it later
    // int scrollPosition = chatPane.getCaretPosition();
    
    // Clear the chat pane
    SwingUtilities.invokeLater(() -> {
        try {
            doc.remove(0, doc.getLength());
        } catch (BadLocationException e) {
            appendStyledMessage("Error clearing chat", false);
            e.printStackTrace();
        }
    });
        currentChat = targetUser;
        updateWindowTitle();
        updateSendButtonsState();
         loadChatHistory(targetUser);
    }

    private void updateWindowTitle() {
    SwingUtilities.invokeLater(() -> {
        String title = "Chat - " + username;
        if (!"ALL".equals(currentChat)) {
            title += " (Private with: " + currentChat + ")";
        }
        setTitle(title);
    });
}

   private void loadChatHistory(String withUser) {
    if (withUser == null || withUser.equals(username)) {
        return;  // Don't load history for self or null
    }
    
    try {
        Message historyRequest = new Message(Message.Type.CHAT_HISTORY, username, withUser);
        out.writeObject(historyRequest);
        out.flush();
    } catch (IOException e) {
        appendStyledMessage("Error loading chat history", false);
        if (socket.isClosed()) {
            closeConnection();
        }
    }
}

    private void connectAndAuth(String action) {
        try {
            String host = hostField.getText().trim();
            int port = Integer.parseInt(portField.getText().trim());
            username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();

            if (username.isEmpty() || password.isEmpty()) {
                appendStyledMessage("Username/Password cannot be empty.", false);
                return;
            }

            if (socket == null || socket.isClosed()) {
                socket = new Socket(host, port);
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                new Thread(this::listenFromServer).start();
            }

            Message.Type type = action.equals("LOGIN") ? Message.Type.LOGIN : Message.Type.REGISTER;
            Message authMsg = new Message(type, username, password);
            out.writeObject(authMsg);
            out.flush();

        } catch (Exception e) {
            appendStyledMessage("Connection failed: " + e.getMessage(), false);
            closeConnection();
        }
    }
    private void updateUserList(String users) {
    SwingUtilities.invokeLater(() -> {
        userListModel.clear();
        userListModel.addElement("ALL"); // Always include ALL option
        for (String user : users.split(",")) {
            if (!user.equals(username)) { // Don't show self in user list
                userListModel.addElement(user);
                // Initialize chat history for this user if not exists
                chatHistories.putIfAbsent(user, new ArrayList<>());
            }
        }
    });
}

    private void listenFromServer() {
    try {
        Message msg;
        while ((msg = (Message) in.readObject()) != null) {
            final String content = msg.getContent();
            switch (msg.getType()) {
                case TEXT:
                    if ("LOGIN_SUCCESS".equals(content)) {
                        handleLoginSuccess();
                    } else if ("LOGIN_FAIL".equals(content)) {
                        appendStyledMessage("❌ Login failed. Try again.", false);
                    } else if ("REGISTER_SUCCESS".equals(content)) {
                        appendStyledMessage("✅ Registration successful! Please login.", false);
                    } else if ("REGISTER_FAIL".equals(content)) {
                        appendStyledMessage("❌ Registration failed. Username may be taken.", false);
                    } else {
                        handleTextMessage(msg);
                    }
                    break;
                case CHAT_HISTORY:
                    if (msg.isPrivate()) {
                        // Show only if current chat matches the conversation
                        if ((msg.getSender().equals(currentChat) && msg.getRecipient().equals(username)) ||
                            (msg.getSender().equals(username) && msg.getRecipient().equals(currentChat))) {
                            appendStyledMessage("[" + getCurrentTime() + "] " +
                                    (msg.getSender().equals(username) ? "You" : msg.getSender()) + ": " + msg.getContent(), 
                                    msg.getSender().equals(username));
                        }
                    } else if ("ALL".equals(currentChat)) {
                        appendStyledMessage("[" + getCurrentTime() + "] " +
                                (msg.getSender().equals(username) ? "You" : msg.getSender()) + ": " + msg.getContent(),
                                msg.getSender().equals(username));
                    }

                    break;
                case ONLINE_USERS:
                    updateUserList(content); // This now calls the properly defined method
                    break;
                case FILE:
                    saveReceivedFile(msg);
                    break;
                default:
                    appendStyledMessage("❓ Unknown message type.", false);
            }
        }
    } catch (Exception e) {
        appendStyledMessage("Disconnected from server.", false);
        closeConnection();
    }
}

    private void handleTextMessage(Message msg) {
    String sender = msg.getSender();
    String content = msg.getContent();
    
    if (msg.isPrivate()) {
        // Only show if we're the recipient or sender AND in the right chat
        if ((msg.getRecipient().equals(username) && sender.equals(currentChat)) || 
            (sender.equals(username) && msg.getRecipient().equals(currentChat))) {
            appendStyledMessage("[" + getCurrentTime() + "] " + 
                (sender.equals(username) ? "You" : sender) + ": " + content, 
                sender.equals(username));
        }
    } else if ("ALL".equals(currentChat)) {
        // Always show public messages in ALL chat
        appendStyledMessage("[" + getCurrentTime() + "] " + 
            (sender.equals(username) ? "You" : sender) + ": " + content, 
            sender.equals(username));
    }
}
private String getCurrentTime() {
    return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
}

    private void handleLoginSuccess() {
    SwingUtilities.invokeLater(() -> {
        try {
            doc.remove(0, doc.getLength()); // Clear chat pane
            appendStyledMessage("✅ Logged in successfully!", false);
            
            // Enable UI components
            sendButton.setEnabled(true);
            fileButton.setEnabled(true);
            loginButton.setEnabled(false);
            registerButton.setEnabled(false);
            updateWindowTitle();
            
            // Set default to ALL chat and load public messages
            currentChat = "ALL";
            userList.setSelectedValue("ALL", true);
            loadChatHistory("ALL");
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    });
}



    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            try {
                Message msg;
                if (currentChat.equals("ALL")) {
                    msg = new Message(Message.Type.TEXT, username, message);
                    appendStyledMessage("You: " + message, true);
                } else {
                    msg = new Message(Message.Type.TEXT, username, message, currentChat);
                    appendStyledMessage("You: " + message, true);
                }
                
                out.writeObject(msg);
                out.flush();
                messageField.setText("");
            } catch (IOException e) {
                appendStyledMessage("Failed to send message.", false);
                closeConnection();
            }
        }
    }

    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            new Thread(() -> {
                ProgressDialog progressDialog = new ProgressDialog(this, "Sending File...");
                SwingUtilities.invokeLater(() -> progressDialog.setVisible(true));

                try (FileInputStream fis = new FileInputStream(file);
                     ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    long totalBytes = file.length();
                    long bytesProcessed = 0;

                    while ((bytesRead = fis.read(buffer)) != -1) {
                        baos.write(buffer, 0, bytesRead);
                        bytesProcessed += bytesRead;
                        progressDialog.setProgress((int) ((bytesProcessed * 100) / totalBytes));
                    }

                    Message fileMsg;
                    if (currentChat.equals("ALL")) {
                        fileMsg = new Message(Message.Type.FILE, username, baos.toByteArray(), file.getName());
                        appendStyledMessage("You sent file: " + file.getName(), true);
                    } else {
                        fileMsg = new Message(Message.Type.FILE, username, baos.toByteArray(), file.getName(), currentChat);
                        appendStyledMessage("You sent file: " + file.getName(), true);
                    }

                    out.writeObject(fileMsg);
                    out.flush();
                    progressDialog.closeAfterDelay();

                } catch (IOException e) {
                    appendStyledMessage("File send failed: " + e.getMessage(), false);
                    closeConnection();
                    progressDialog.dispose();   
                }
            }).start();
        }
    }

    private void saveReceivedFile(Message msg) {
        new Thread(() -> {
            ProgressDialog progressDialog = new ProgressDialog(this, "Receiving File...");
            SwingUtilities.invokeLater(() -> progressDialog.setVisible(true));

            try {
                new File("received_files").mkdirs();
                String fileName = "Received_" + msg.getFileName();
                File receivedFile = new File("received_files", fileName);

                byte[] fileBytes = msg.getFileData();
                int totalBytes = fileBytes.length;
                int offset = 0;

                try (FileOutputStream fos = new FileOutputStream(receivedFile)) {
                    while (offset < totalBytes) {
                        int bytesToWrite = Math.min(4096, totalBytes - offset);
                        fos.write(fileBytes, offset, bytesToWrite);
                        offset += bytesToWrite;
                        progressDialog.setProgress((int) (((double) offset / totalBytes) * 100));
                    }
                }

                String sender = msg.getSender();
                if (msg.isPrivate()) {
                    if (msg.getRecipient().equals(username) && sender.equals(currentChat)) {
                        appendStyledMessage(sender + " sent file: " + receivedFile.getPath(), false);
                    } else if (sender.equals(username) && msg.getRecipient().equals(currentChat)) {
                        appendStyledMessage("You sent file: " + receivedFile.getPath(), true);
                    }
                } else if (currentChat.equals("ALL")) {
                    appendStyledMessage(sender + " sent file: " + receivedFile.getPath(), sender.equals(username));
                }

                progressDialog.closeAfterDelay();

            } catch (IOException e) {
                appendStyledMessage("File receive error: " + e.getMessage(), false);
                progressDialog.dispose();
            }
        }).start();
    }

    private void closeConnection() {
        try {
            if (socket != null) socket.close();
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (IOException ignored) {}
        SwingUtilities.invokeLater(() -> {
            sendButton.setEnabled(false);
            fileButton.setEnabled(false);
            loginButton.setEnabled(true);
            registerButton.setEnabled(true);
        });
    }

    private void appendStyledMessage(String message, boolean alignRight) {
        SwingUtilities.invokeLater(() -> {
            SimpleAttributeSet set = new SimpleAttributeSet();
            StyleConstants.setAlignment(set, alignRight ? StyleConstants.ALIGN_RIGHT : StyleConstants.ALIGN_LEFT);
            StyleConstants.setFontSize(set, 13);
            doc.setParagraphAttributes(doc.getLength(), 1, set, false);
            try {
                doc.insertString(doc.getLength(), message + "\n", set);
                chatPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    class ProgressDialog extends JDialog {
        private final JProgressBar progressBar;

        public ProgressDialog(JFrame parent, String title) {
            super(parent, title, true);
            progressBar = new JProgressBar(0, 100);
            progressBar.setStringPainted(true);
            add(progressBar, BorderLayout.CENTER);
            setSize(300, 80);
            setLocationRelativeTo(parent);
        }

        public void setProgress(int value) {
            SwingUtilities.invokeLater(() -> progressBar.setValue(value));
        }

        public void closeAfterDelay() {
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {}
                SwingUtilities.invokeLater(() -> dispose());
            }).start();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Client().setVisible(true));
    }
}
