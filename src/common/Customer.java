import java.io.Serializable;

/** A row of the customers table. Serializable, so Spark can shuffle it. */
public record Customer(int customerId, String name, String canton) implements Serializable {

    /** Parses "customer_id,name,canton", e.g. "0,Urs Bucher,BE". */
    public static Customer fromCsv(String line) {
        String[] f = line.split(",");
        return new Customer(Integer.parseInt(f[0]), f[1], f[2]);
    }
}
