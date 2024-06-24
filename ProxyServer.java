import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyServer extends JFrame {
    private static final int PORT = 8080; // Proxy server'ın dinleyeceği port
    private static final Set<String> SUPPORTED_METHODS = Set.of("GET", "HEAD", "OPTIONS", "POST"); // Desteklenen HTTP metodları
    private static final String FILTERED_DOMAINS_PATH = "/Users/berk/Desktop/471_final/src/filtered_domains.txt"; // Filtrelenmiş domainlerin listesi
    public static Set<String> filteredDomains = new HashSet<>(); // Filtrelenmiş domainlerin saklanacağı set
    private static ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>(); // Önbellek için hashmap
    private static ConcurrentHashMap<String, Long> lastModifiedTimes = new ConcurrentHashMap<>(); // Son değişiklik zamanları için hashmap
    private static ServerSocket serverSocket; // Server socket
    private static volatile boolean running = false; // Proxy server'ın çalışma durumu
    private JTextArea logArea; // Log alanı
    private JButton startButton, stopButton; // Başlat ve durdur düğmeleri

    public ProxyServer() {
        createView(); // Görsel arayüzü oluşturur
        setTitle("HTTP Proxy Server Control Panel"); // Pencere başlığını ayarlar
        setSize(600, 400); // Pencere boyutunu ayarlar
        setDefaultCloseOperation(EXIT_ON_CLOSE); // Pencere kapatıldığında uygulamayı sonlandırır
        setLocationRelativeTo(null); // Pencereyi ekranın ortasına yerleştirir
    }

    private void createView() {
        JPanel panel = new JPanel(new BorderLayout()); // Ana paneli BorderLayout ile oluşturur
        logArea = new JTextArea(); // Log alanını oluşturur
        logArea.setEditable(false); // Log alanını düzenlenemez yapar
        JScrollPane scrollPane = new JScrollPane(logArea); // Log alanını kaydırma paneline ekler
        panel.add(scrollPane, BorderLayout.CENTER); // Kaydırma panelini ana panelin ortasına ekler

        startButton = new JButton("Start Proxy"); // Başlat düğmesini oluşturur
        startButton.addActionListener(this::startProxy); // Başlat düğmesine tıklama olayını ekler
        stopButton = new JButton("Stop Proxy"); // Durdur düğmesini oluşturur
        stopButton.addActionListener(this::stopProxy); // Durdur düğmesine tıklama olayını ekler

        JPanel buttonsPanel = new JPanel(); // Düğmeler panelini oluşturur
        buttonsPanel.add(startButton); // Başlat düğmesini düğmeler paneline ekler
        buttonsPanel.add(stopButton); // Durdur düğmesini düğmeler paneline ekler
        panel.add(buttonsPanel, BorderLayout.SOUTH); // Düğmeler panelini ana panelin altına ekler

        getContentPane().add(panel); // Ana paneli pencereye ekler
    }

    private void startProxy(ActionEvent e) {
        if (!running) { // Eğer proxy server çalışmıyorsa
            new Thread(() -> { // Yeni bir thread başlat
                try {
                    serverSocket = new ServerSocket(PORT); // Server socketi başlat
                    running = true; // Proxy server çalışıyor olarak ayarla
                    logArea.append("Proxy Server is listening on port " + PORT + "\n"); // Log alanına mesaj yaz
                    while (running) { // Proxy server çalıştığı sürece
                        try {
                            Socket clientSocket = serverSocket.accept(); // Gelen bağlantıyı kabul et
                            new Thread(() -> handleClient(clientSocket)).start(); // Yeni bir thread'de client işlemesini başlat
                        } catch (IOException ex) {
                            logArea.append("Error accepting client connection: " + ex.getMessage() + "\n"); // Hata mesajını log alanına yaz
                        }
                    }
                } catch (IOException ex) {
                    logArea.append("Error starting the proxy server: " + ex.getMessage() + "\n"); // Hata mesajını log alanına yaz
                }
            }).start();
        }
    }

    private void stopProxy(ActionEvent e) {
        try {
            running = false; // Proxy server'ı durdur
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); // Server socketi kapat
            }
            logArea.append("Proxy Server stopped.\n"); // Log alanına mesaj yaz
        } catch (IOException ex) {
            logArea.append("Error stopping the proxy server: " + ex.getMessage() + "\n"); // Hata mesajını log alanına yaz
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ProxyServer().setVisible(true)); // Arayüzü başlat
    }

    private static void handleClient(Socket clientSocket) {
        try (DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());
             BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

            handleClientRequest(input, output); // Client isteğini işle

        } catch (IOException e) {
            System.out.println("Error handling client request: " + e.getMessage()); // Hata mesajını konsola yaz
        } finally {
            try {
                clientSocket.close(); // Client socketi kapat
            } catch (IOException e) {
                System.out.println("Error closing client socket: " + e.getMessage()); // Hata mesajını konsola yaz
            }
        }
    }

    private static void handleClientRequest(BufferedReader input, DataOutputStream output) throws IOException {
        String requestLine = input.readLine(); // İstek satırını oku
        if (requestLine == null || requestLine.isBlank()) {
            sendErrorResponse(output, "400 Bad Request"); // Hatalı istek yanıtı gönder
            return;
        }

        String[] tokens = requestLine.split(" "); // İstek satırını boşluklardan ayır
        if (tokens.length < 3) {
            sendErrorResponse(output, "400 Bad Request"); // Hatalı istek yanıtı gönder
            return;
        }

        String method = tokens[0]; // HTTP metodunu al
        String url = tokens[1]; // URL'yi al

        System.out.println("Handling request: " + method + " " + url);

        if (url.isBlank()) {
            sendErrorResponse(output, "400 Bad Request"); // Hatalı istek yanıtı gönder
            return;
        }

        if (isFiltered(url)) {
            sendErrorResponse(output, "401 Unauthorized"); // Yetkisiz yanıtı gönder
            return;
        }

        if (!SUPPORTED_METHODS.contains(method)) {
            sendErrorResponse(output, "405 Method Not Allowed"); // Desteklenmeyen metod yanıtı gönder
            return;
        }

        if (method.equals("GET")) {
            handleGetRequest(url, output); // GET isteğini işle
        } else {
            sendSimpleResponse(output, url, method); // Diğer metodları işle
        }
    }

    public static void loadFilteredDomains() {
        filteredDomains.clear(); // Filtreli alanları temizle
        try (BufferedReader reader = new BufferedReader(new FileReader(FILTERED_DOMAINS_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    filteredDomains.add(line.trim()); // Filtreli alanları yükle
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading filtered domains: " + e.getMessage()); // Hata mesajını konsola yaz
        }
    }

    private static boolean isFiltered(String url) {
        return filteredDomains.stream().anyMatch(url::contains); // URL'nin filtreli olup olmadığını kontrol et
    }

    private static void sendErrorResponse(DataOutputStream output, String statusCode) throws IOException {
        output.writeBytes("HTTP/1.1 " + statusCode + "\r\n"); // Hata yanıtı gönder
        output.writeBytes("Content-Type: text/html\r\n");
        output.writeBytes("\r\n");
        output.writeBytes("<html><body><h1>" + statusCode + "</h1></body></html>\r\n");
        output.flush();
    }

    private static void sendSimpleResponse(DataOutputStream output, String url, String method) throws IOException {
        System.out.println("Sending response for method: " + method);
        output.writeBytes("HTTP/1.1 200 OK\r\n"); // Basit yanıt gönder
        if (method.equals("OPTIONS")) {
            output.writeBytes("Allow: GET, HEAD, OPTIONS, POST\r\n"); // Desteklenen metodları belirt
        }
        output.writeBytes("Content-Type: text/html; charset=utf-8\r\n");
        output.writeBytes("\r\n");
        if (!method.equals("HEAD") && !method.equals("OPTIONS")) {
            output.writeBytes("<html><body><h1>Requested URL: " + url + "</h1></body></html>\r\n"); // Yanıt gövdesini gönder
        }
        output.flush();
        System.out.println("Response sent for method: " + method);
    }

    private static void handleGetRequest(String url, DataOutputStream output) throws IOException {
        if (cache.containsKey(url) && !isResourceExpired(url)) {
            // Eğer istenen URL'nin yanıtı önbellekte varsa ve kaynak süresi dolmamışsa
            sendCachedResponse(output, url);
        } else {
            // Kaynak önbellekte yoksa veya süresi dolmuşsa
            HttpURLConnection conn = null;
            try {
                URL resourceUrl = new URI(url).toURL();
                conn = (HttpURLConnection) resourceUrl.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000); // Bağlantı süresini uzatmak
                conn.setReadTimeout(10000); // Okuma süresini uzatmak
                if (lastModifiedTimes.containsKey(url)) {
                    // Eğer önbellekte bu kaynağın son değişiklik zamanı varsa
                    conn.setIfModifiedSince(lastModifiedTimes.get(url));
                }
    
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String contentType = conn.getContentType();
                    long contentLength = conn.getContentLengthLong();
                    output.writeBytes("HTTP/1.1 200 OK\r\n");
                    output.writeBytes("Content-Type: " + contentType + "\r\n");
                    output.writeBytes("Content-Length: " + contentLength + "\r\n");
                    output.writeBytes("\r\n");
    
                    try (InputStream inputStream = conn.getInputStream()) {
                        byte[] buffer = new byte[8192]; // Daha büyük bir buffer kullanarak performans artırılabilir.
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            output.write(buffer, 0, bytesRead);
                        }
                    }
    
                    // Önbellek için yanıtı sakla, eğer içerik 500 MB'dan küçükse
                    if (contentLength <= 500 * 1024 * 1024) { // 500 MB'dan küçük dosyaları önbelleğe al
                        StringBuilder responseBody = new StringBuilder();
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                responseBody.append(line).append("\n");
                            }
                        }
                        cache.put(url, responseBody.toString());
                        lastModifiedTimes.put(url, conn.getLastModified());
                    } else {
                        // Büyük dosya indirildiğinde önbelleğe alma
                        System.out.println("Large file downloaded, not caching: " + url);
                    }
                } else if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                    // Kaynak değişmemişse, önbellekteki yanıtı kullan
                    sendCachedResponse(output, url);
                }
            } catch (URISyntaxException e) {
                sendErrorResponse(output, "400 Bad Request");
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
        output.flush();
    }
    

    private static boolean isResourceExpired(String url) {
        long cachedTime = lastModifiedTimes.get(url); // Önbellekteki zamanı al
        long currentTime = System.currentTimeMillis(); // Şu anki zamanı al
        long oneDayMillis = 24 * 60 * 60 * 1000; // Bir günün milisaniye cinsinden değeri
        return (currentTime - cachedTime) > oneDayMillis; // Kaynağın süresinin dolup dolmadığını kontrol et
    }

    private static void sendCachedResponse(DataOutputStream output, String url) throws IOException {
        String response = cache.get(url); // Önbellekteki yanıtı al
        output.writeBytes("HTTP/1.1 200 OK\r\n"); // Yanıt başlığını gönder
        output.writeBytes("Content-Type: text/html; charset=utf-8\r\n");
        output.writeBytes("\r\n");
        output.writeBytes(response); // Yanıt gövdesini gönder
        output.flush(); // Output'u temizle
    }

    public static void stopServer() {
        try {
            running = false; // Proxy server'ı durdur
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); // Server socketi kapat
            }
        } catch (IOException ex) {
            System.out.println("Error stopping the proxy server: " + ex.getMessage()); // Hata mesajını konsola yaz
        }
    }
}
