import java.awt.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.text.*;

public class Client extends JFrame {
    private JTextField hostField, portField, messageField;
    private JButton connectButton, sendButton, fileButton;
    private JTextPane chatPane;
    private StyledDocument doc;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private DataOutputStream dataOut;

    public Client() {
        setTitle("Chat + File Client");
        setSize(550, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // === Top Panel ===
        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(new JLabel("Host:"));
        hostField = new JTextField("localhost", 10);
        topPanel.add(hostField);

        topPanel.add(new JLabel("Port:"));
        portField = new JTextField("12345", 5);
        topPanel.add(portField);

        connectButton = new JButton("Connect");
        topPanel.add(connectButton);
        add(topPanel, BorderLayout.NORTH);

        // === Chat Area ===
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chatPane.setBackground(new Color(245, 245, 245)); // Light grey background
        doc = chatPane.getStyledDocument();
        add(new JScrollPane(chatPane), BorderLayout.CENTER);

        // === Bottom Panel ===
        JPanel bottomPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        messageField.addActionListener(e -> sendMessage());
        bottomPanel.add(messageField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        sendButton = new JButton("Send");
        fileButton = new JButton("Send File");
        buttonPanel.add(fileButton);
        buttonPanel.add(sendButton);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        JButton clearButton = new JButton("Clear Chat");
        clearButton.setToolTipText("Clear chat window");
        clearButton.addActionListener(e -> chatPane.setText(""));
        buttonPanel.add(clearButton);

        // === Event Listeners ===
        connectButton.addActionListener(e -> connectToServer());
        sendButton.addActionListener(e -> sendMessage());
        fileButton.addActionListener(e -> sendFile());

        // === Disable buttons until connected ===
        sendButton.setEnabled(false);
        fileButton.setEnabled(false);

        sendButton.setToolTipText("Send text message");
        fileButton.setToolTipText("Choose and send a file");
        connectButton.setToolTipText("Connect to the server");
    }

    private void connectToServer() {
        String host = hostField.getText();
        int port = Integer.parseInt(portField.getText());

        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            dataOut = new DataOutputStream(socket.getOutputStream());

            appendStyledMessage("Connected to server at " + host + ":" + port, false);
            sendButton.setEnabled(true);
            fileButton.setEnabled(true);
            connectButton.setEnabled(false);

            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        appendStyledMessage(msg, false); // Messages from server on left
                    }
                } catch (IOException e) {
                    appendStyledMessage("Disconnected from server.", false);
                }
            }).start();

        } catch (IOException e) {
            appendStyledMessage("Connection failed: " + e.getMessage(), false);
        }
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            out.println("Client: " + message);
            appendStyledMessage("You: " + message, true); // Right aligned
            messageField.setText("");
        }
    }

    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                FileInputStream fileIn = new FileInputStream(file);
                long fileSize = file.length();

                out.println("FILE:" + file.getName() + ":" + fileSize); // Send metadata

                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalSent = 0;

                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    dataOut.write(buffer, 0, bytesRead);
                    totalSent += bytesRead;
                }

                dataOut.flush();
                appendStyledMessage("You sent: " + file.getName(), true);

                fileIn.close();
            } catch (IOException e) {
                appendStyledMessage("File transfer failed: " + e.getMessage(), false);
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Client().setVisible(true));
    }
}
