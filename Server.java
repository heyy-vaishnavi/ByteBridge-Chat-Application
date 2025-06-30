import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.text.*;

public class Server extends JFrame {
    private JTextField portField, messageField;
    private JLabel ipLabel;
    private JButton startButton, broadcastButton, clearButton;
    private JTextPane chatPane;
    private StyledDocument doc;
    private ServerSocket serverSocket;
    private Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());

    public Server() {
        setTitle("Chat + File Server");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top Panel
        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(new JLabel("Port:"));
        portField = new JTextField("12345", 8);
        startButton = new JButton("Start Server");
        topPanel.add(portField);
        topPanel.add(startButton);

        ipLabel = new JLabel(" IP: Not started");
        topPanel.add(ipLabel);

        add(topPanel, BorderLayout.NORTH);

        // Chat Display
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chatPane.setBackground(new Color(245, 245, 245)); // Light grey background
        doc = chatPane.getStyledDocument();
        add(new JScrollPane(chatPane), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        bottomPanel.add(messageField, BorderLayout.CENTER);

        // Create a new panel for buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        broadcastButton = new JButton("Broadcast");
        clearButton = new JButton("Clear Chat");  // ✅ No redeclaration

        clearButton.setToolTipText("Clear chat window");
        clearButton.addActionListener(e -> chatPane.setText(""));

        buttonPanel.add(clearButton);
        buttonPanel.add(broadcastButton);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);
        // Button Actions
        startButton.addActionListener(e -> startServer());
        broadcastButton.addActionListener(e -> {
            String msg = messageField.getText().trim();
            if (!msg.isEmpty()) {
                broadcastMessage("Server: " + msg, true);
                messageField.setText("");
            }
        });

        startButton.setToolTipText("Start listening on the port");
        broadcastButton.setToolTipText("Broadcast message to all clients");
    }

    private void startServer() {
        int port = Integer.parseInt(portField.getText());
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                String ip = InetAddress.getLocalHost().getHostAddress();
                ipLabel.setText(" IP: " + ip);
                appendStyledMessage("Server started on " + ip + ":" + port, true);

                while (true) {
                    Socket socket = serverSocket.accept();
                    appendStyledMessage("Client connected: " + socket.getInetAddress(), false);
                    ClientHandler handler = new ClientHandler(socket);
                    clients.add(handler);
                    new Thread(handler).start();
                }
            } catch (IOException e) {
                appendStyledMessage("Error: " + e.getMessage(), false);
            }
        }).start();
    }

    private void broadcastMessage(String message, boolean fromServer) {
        appendStyledMessage(message, fromServer);
        synchronized (clients) {
            for (ClientHandler c : clients) {
                c.sendMessage(message);
            }
        }
    }

    private void appendStyledMessage(String message, boolean alignRight) {
        SimpleAttributeSet set = new SimpleAttributeSet();
        StyleConstants.setForeground(set, alignRight ? Color.BLUE : Color.DARK_GRAY);
        StyleConstants.setBold(set, true);

        StyleConstants.setAlignment(set, alignRight ? StyleConstants.ALIGN_RIGHT : StyleConstants.ALIGN_LEFT);
        StyleConstants.setFontSize(set, 14);
        StyleConstants.setSpaceAbove(set, 5);
        StyleConstants.setSpaceBelow(set, 5);
        doc.setParagraphAttributes(doc.getLength(), 1, set, false);

        try {
            String time = new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date());
            doc.insertString(doc.getLength(), "[" + time + "] " + message + "\n", set);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private DataInputStream dis;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void sendMessage(String msg) {
            if (out != null) out.println(msg);
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                dis = new DataInputStream(socket.getInputStream());
                out = new PrintWriter(socket.getOutputStream(), true);

                String msg;
                while ((msg = in.readLine()) != null) {
                    if (msg.startsWith("FILE:")) {
                        receiveFile(msg.substring(5));
                    } else {
                        broadcastMessage(msg, false);
                    }
                }
            } catch (IOException e) {
                appendStyledMessage("A client disconnected.", false);
            } finally {
                try {
                    clients.remove(this);
                    socket.close();
                } catch (IOException ignored) {}
            }
        }

        private void receiveFile(String fileMeta) {
        try {
            String[] parts = fileMeta.split(":");
            String fileName = parts[0];
            long fileSize = Long.parseLong(parts[1]);

            FileOutputStream fos = new FileOutputStream("Received_" + fileName);
            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalRead = 0;

            while (totalRead < fileSize &&
                (bytesRead = dis.read(buffer, 0, (int)Math.min(buffer.length, fileSize - totalRead))) != -1) {
                fos.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }

            fos.close();
            appendStyledMessage("File received: " + fileName, false);
        } catch (IOException | NumberFormatException e) {
            appendStyledMessage("Error receiving file.", false);
            e.printStackTrace();
        }
    }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Server().setVisible(true));
    }
}
