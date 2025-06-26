package lab4.lab4.Binance;

public class PriceUpdate {
    private final String symbol;
    private final String price;
    
    public PriceUpdate(String symbol, String price) {
        this.symbol = symbol;
        this.price = price;
    }

    public String getSymbol() {
        return symbol;
    }
    
    public String getPrice() {
        return price;
    }
    
    
    @Override
    public String toString() {

        return String.format("PriceUpdate{symbol='%s', price='%s'}", symbol, price);
    }
}