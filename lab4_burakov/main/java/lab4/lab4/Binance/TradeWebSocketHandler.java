package lab4.lab4.Binance;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;


import jakarta.annotation.PreDestroy;
import lab4.lab4.protobuf.PriceUpdateClass;

@Component
public class TradeWebSocketHandler extends BinaryWebSocketHandler {

    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<BinaryMessage>> sessionQueues = new ConcurrentHashMap<>();
    private final ExecutorService messageSenderExecutor = Executors.newCachedThreadPool();
    
    private final Lock sendLock = new ReentrantLock();
    private long lastSendTime = 0;
    private static final long MIN_SEND_INTERVAL = 100; // мс

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        System.out.println("WebSocket client connected: " + session.getId());
        
        ConcurrentLinkedQueue<BinaryMessage> queue = new ConcurrentLinkedQueue<>();
        sessionQueues.put(session.getId(), queue);
        messageSenderExecutor.submit(() -> processSessionMessages(session, queue));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        System.out.println("WebSocket client disconnected: " + session.getId());

        sessionQueues.remove(session.getId());
    }

    public void broadcastPriceUpdate(PriceUpdateClass.PriceUpdate protoUpdate) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSendTime < MIN_SEND_INTERVAL) {
            return;
        }
        lastSendTime = currentTime;

        byte[] bytes = protoUpdate.toByteArray();
        BinaryMessage message = new BinaryMessage(bytes);
        

        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {

                ConcurrentLinkedQueue<BinaryMessage> queue = sessionQueues.get(session.getId());
                if (queue != null) {
                    queue.offer(message); 
                }
            }
        }
    }


    private void processSessionMessages(WebSocketSession session, ConcurrentLinkedQueue<BinaryMessage> queue) {
        while (session.isOpen()) {
            BinaryMessage message = queue.poll();
            if (message != null) {
                try {
                    session.sendMessage(message);
                } catch (IOException e) {
                    System.err.println("Failed to send message for session " + session.getId() + ": " + e.getMessage());
                    
                    if (!session.isOpen()) {
                        sessions.remove(session);
                        sessionQueues.remove(session.getId());
                        break;
                    }
                } catch (IllegalStateException e) {
                    System.err.println("State error sending message for session " + session.getId() + ": " + e.getMessage());

                }
            } else {

                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Message sender thread interrupted for session " + session.getId());
                    break;
                }
            }
        }
        System.out.println("Message sender thread stopped for session " + session.getId());
    }

    @PreDestroy
    public void shutdownExecutor() {
        messageSenderExecutor.shutdown();
    }
}