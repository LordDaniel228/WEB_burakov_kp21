package lab4.lab4.Binance;
import java.util.LinkedList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "binance")
public class BinanceProperties {
    private List<String> currencies = new LinkedList<>();
    
    public List<String> getCurrencies() {
        return currencies;
    }
    
    public void setCurrencies(List<String> currencies) {
        this.currencies = currencies != null ? currencies : new LinkedList<>();
    }
}