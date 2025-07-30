package common;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;


public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        LOGIN, REGISTER, TEXT, FILE, ONLINE_USERS, CHAT_HISTORY
    }

    private Type type;
    private String sender;
    private String content;
    private String recipient;
    private byte[] fileData;
    private String fileName;
    private Date timestamp = new Date();
    private boolean isPrivate = false;




    // Constructor for TEXT, LOGIN, REGISTER, CHAT_HISTORY (no recipient)
    public Message(Type type, String sender, String content) {
        this.type = type;
        this.sender = sender;
        this.content = content;
    }

    // Constructor for TEXT, CHAT_HISTORY (with recipient)
    public Message(Type type, String sender, String content, String recipient) {
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.recipient = recipient;
    }

    public void setContent(String content) {
        this.content = content;
    }

    // Add this setter
    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    // Add this getter
    public boolean isPrivate() {
        return isPrivate;
    }

    // Constructor for FILE (broadcast)
    public Message(Type type, String sender, byte[] fileData, String fileName) {
        this.type = type;
        this.sender = sender;
        this.fileData = fileData;
        this.fileName = fileName;
    }

    // Constructor for FILE (private)
    public Message(Type type, String sender, byte[] fileData, String fileName, String recipient) {
        this.type = type;
        this.sender = sender;
        this.fileData = fileData;
        this.fileName = fileName;
        this.recipient = recipient;
    }
    
    public String getTimestamp() {
    return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timestamp);
}


    // Getters
    public Type getType() {
        return type;
    }

    public String getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public String getRecipient() {
        return recipient;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public String getFileName() {
        return fileName;
    }

    // Setter needed by Server.java
    public void setSender(String sender) {
        this.sender = sender;
    }

    
}
