# 💬 Java TCP Chat + File Transfer Application

This project implements a simple TCP-based client-server chat application in Java using Swing GUI. It supports real-time messaging and file transfer (PDF, DOCX, images, etc.) between a server and multiple clients.

---

## 🚀 Features

- ✅ Real-time chat between server and multiple clients
- ✅ File transfer support (PDF, DOCX, TXT, ZIP, etc.)
- ✅ GUI built using Java Swing
- ✅ Styled message display with timestamps
- ✅ Chat clear button and tooltips for better UX
- ✅ Portable `.jar` files for easy deployment

---

## 📁 Project Structure

MultiThreadedChatFileApp/
├── Client.java # Client application code
├── Server.java # Server application code
├── ClientManifest.txt # Manifest for client jar
├── ServerManifest.txt # Manifest for server jar
├── ClientApp.jar # Runnable JAR for client (after build)
├── ServerApp.jar # Runnable JAR for server (after build)

---

## 🛠️ How to Compile & Run

### 🔧 Compile
javac Client.java Server.java
📝 Create Manifest Files
ClientManifest.txt
ServerManifest.txt

📦 Create JAR Files
Client:
jar cfm ClientApp.jar ClientManifest.txt *.class

Server:
jar cfm ServerApp.jar ServerManifest.txt *.class

▶️ How to Use
Start the Server:
java -jar ServerApp.jar

Choose the port (default: 12345)

Click Start Server

Start the Client:
java -jar ClientApp.jar

Enter host (e.g., localhost) and port (12345)

Click Connect

Send messages or choose Send File to transfer documents

💡 Future Improvements
Add file transfer progress bar

Support drag-and-drop files

Save chat history locally

Add emojis and message formatting

👨‍💻 Author
Vaishnavi Kainthola