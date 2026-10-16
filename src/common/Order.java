import java.io.Serializable;
import java.time.LocalDate;

/** A row of the orders table. Serializable, so Spark can shuffle it. */
public record Order(long orderId, int customerId, LocalDate orderDate, double amount) implements Serializable {

    /** Parses "order_id,customer_id,order_date,amount", e.g. "0,17730,2025-01-01,34.75". */
    public static Order fromCsv(String line) {
        String[] f = line.split(",");
        return new Order(Long.parseLong(f[0]), Integer.parseInt(f[1]), LocalDate.parse(f[2]), Double.parseDouble(f[3]));
    }
}
