package itm.oss.splitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;

class SplitterTest {
    private final Splitter splitter = new Splitter();
    @Test
    @DisplayName("Test for compute balance")
    void testComputeBalance() {
        ArrayList<Expense> expenses = new ArrayList<Expense>();

        ArrayList<String> names = new ArrayList<>();
        names.add("Alice");
        names.add("Bob");
        names.add("Cara");

        Expense expense01 = new Expense("2025-10-01",
                "Alice", BigDecimal.valueOf(70.00),
                "USD",names,"Food","Pizza night");


        Expense expense02 = new Expense("2025-10-02",
                "Bob", BigDecimal.valueOf(120.00),
                "USD",names,"Transport","Taxi + subway");

        Expense expense03 = new Expense("2025-10-03",
                "Cara", BigDecimal.valueOf(90.00),
                "USD",names,"Groceries","Market");

        expenses.add(expense01);
        expenses.add(expense02);
        expenses.add(expense03);

        // TODO: Implement Test case at least one happy-path
        Balance result = splitter.computeBalances(expenses);
        for(String name : names) {
            System.out.println(name + " : " + result.getAmount(name));
        }
        // TODO: Implement Test case at least one edge-case test
    }
}