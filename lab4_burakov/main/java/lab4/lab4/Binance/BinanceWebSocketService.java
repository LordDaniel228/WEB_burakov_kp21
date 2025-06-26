package lab4.lab4.Binance;

import java.util.List;

import org.springframework.stereotype.Service;

import com.binance.connector.client.WebSocketStreamClient;
import com.binance.connector.client.impl.WebSocketStreamClientImpl;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class BinanceWebSocketService {
    private final BinanceProperties binanceProperties;
    private final WebSocketStreamClient client;
    private final TradeWebSocketHandler tradeWebSocketHandler;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    
    public BinanceWebSocketService(BinanceProperties binanceProperties, TradeWebSocketHandler tradeWebSocketHandler) {
        this.binanceProperties = binanceProperties;
        this.tradeWebSocketHandler = tradeWebSocketHandler;
        this.client = new WebSocketStreamClientImpl();
    }
    
    @PostConstruct
    public void init() {
        List<String> currencies = binanceProperties.getCurrencies();
        if (currencies.isEmpty()) {
            return;
        }
        currencies.stream().forEach(this::subscribeToTradeStream);
    }
    
    @PreDestroy
    public void cleanup() {
        client.closeAllConnections();
    }
    
    private void subscribeToTradeStream(String symbol) {
        String lowerCaseSymbol = symbol.toLowerCase();
        client.tradeStream(lowerCaseSymbol, event -> {
            try {
                JsonNode tradeEvent = objectMapper.readTree(event);
                final String TRADE_EVENT = "trade";
                if (tradeEvent.has("e") && TRADE_EVENT.equals(tradeEvent.get("e").asText()) &&
                        tradeEvent.has("s") && tradeEvent.has("p")) {
                    String receivedSymbol = tradeEvent.get("s").asText();
                    String price = tradeEvent.get("p").asText();
                    lab4.lab4.protobuf.PriceUpdateClass.PriceUpdate protoUpdate = lab4.lab4.protobuf.PriceUpdateClass.PriceUpdate
                            .newBuilder()
                            .setSymbol(receivedSymbol)
                            .setPrice(price)
                            .build();
                    tradeWebSocketHandler.broadcastPriceUpdate(protoUpdate);
                } else {
                    System.out.println(String.format("Received non-trade event or unexpected format for %s: %s",
                            lowerCaseSymbol, event));
                }
            } catch (Exception e) {
                System.out.println(
                        String.format("Error processing Binance trade event for %s: %s. Error: %s", 
                        lowerCaseSymbol, event, e.getMessage()));
            }
        });
    }
}