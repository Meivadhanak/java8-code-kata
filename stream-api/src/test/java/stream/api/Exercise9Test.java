package stream.api;

import common.test.tool.annotation.Difficult;
import common.test.tool.annotation.Easy;
import common.test.tool.dataset.ClassicOnlineStore;
import common.test.tool.entity.Customer;
import common.test.tool.util.CollectorImpl;

import org.junit.Test;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class Exercise9Test extends ClassicOnlineStore {

    @Easy @Test
    public void simplestStringJoin() {
        List<Customer> customerList = this.mall.getCustomerList();

        /**
         * Implement a {@link Collector} which can create a String with comma separated names shown in the assertion.
         * The collector will be used by serial stream.
         */
        Supplier<Object> supplier = StringBuilder::new;
        BiConsumer<Object, String> accumulator = (builder, name) -> {
            StringBuilder result = (StringBuilder) builder;
            if (result.length() > 0) {
                result.append(',');
            }
            result.append(name);
        };
        BinaryOperator<Object> combiner = (left, right) -> {
            StringBuilder result = (StringBuilder) left;
            StringBuilder other = (StringBuilder) right;
            if (other.length() > 0) {
                if (result.length() > 0) {
                    result.append(',');
                }
                result.append(other);
            }
            return result;
        };
        Function<Object, String> finisher = Object::toString;

        Collector<String, ?, String> toCsv =
            new CollectorImpl<>(supplier, accumulator, combiner, finisher, Collections.emptySet());
        String nameAsCsv = customerList.stream().map(Customer::getName).collect(toCsv);
        assertThat(nameAsCsv, is("Joe,Steven,Patrick,Diana,Chris,Kathy,Alice,Andrew,Martin,Amy"));
    }

    @Difficult @Test
    public void mapKeyedByItems() {
        List<Customer> customerList = this.mall.getCustomerList();

        /**
         * Implement a {@link Collector} which can create a {@link Map} with keys as item and
         * values as {@link Set} of customers who are wanting to buy that item.
         * The collector will be used by parallel stream.
         */
        Supplier<Object> supplier = ConcurrentHashMap::new;
        BiConsumer<Object, Customer> accumulator = (map, customer) -> {
            Map<String, Set<String>> result = (Map<String, Set<String>>) map;
            customer.getWantToBuy().forEach(item -> result
                    .computeIfAbsent(item.getName(), key -> ConcurrentHashMap.newKeySet())
                    .add(customer.getName()));
        };
        BinaryOperator<Object> combiner = (left, right) -> {
            Map<String, Set<String>> result = (Map<String, Set<String>>) left;
            Map<String, Set<String>> other = (Map<String, Set<String>>) right;
            other.forEach((item, customers) -> result
                    .computeIfAbsent(item, key -> ConcurrentHashMap.newKeySet())
                    .addAll(customers));
            return result;
        };
        Function<Object, Map<String, Set<String>>> finisher = result -> (Map<String, Set<String>>) result;

        Collector<Customer, ?, Map<String, Set<String>>> toItemAsKey =
            new CollectorImpl<>(supplier, accumulator, combiner, finisher, EnumSet.of(
                Collector.Characteristics.CONCURRENT,
                Collector.Characteristics.IDENTITY_FINISH));
        Map<String, Set<String>> itemMap = customerList.stream().parallel().collect(toItemAsKey);
        assertThat(itemMap.get("plane"), containsInAnyOrder("Chris"));
        assertThat(itemMap.get("onion"), containsInAnyOrder("Patrick", "Amy"));
        assertThat(itemMap.get("ice cream"), containsInAnyOrder("Patrick", "Steven"));
        assertThat(itemMap.get("earphone"), containsInAnyOrder("Steven"));
        assertThat(itemMap.get("plate"), containsInAnyOrder("Joe", "Martin"));
        assertThat(itemMap.get("fork"), containsInAnyOrder("Joe", "Martin"));
        assertThat(itemMap.get("cable"), containsInAnyOrder("Diana", "Steven"));
        assertThat(itemMap.get("desk"), containsInAnyOrder("Alice"));
    }

    @Difficult @Test
    public void bitList2BitString() {
        String bitList = "22-24,9,42-44,11,4,46,14-17,5,2,38-40,33,50,48";

        /**
         * Create a {@link String} of "n"th bit ON.
         * for example
         * "3" will be "001"
         * "1,3,5" will be "10101"
         * "1-3" will be "111"
         * "7,1-3,5" will be "1110101"
         */
        Supplier<Object> bitSetSupplier = BitSet::new;
        BiConsumer<Object, String> bitSetAccumulator = (bits, range) -> {
            BitSet result = (BitSet) bits;
            String[] bounds = range.split("-");
            int first = Integer.parseInt(bounds[0]);
            int last = bounds.length == 1 ? first : Integer.parseInt(bounds[1]);
            result.set(first - 1, last);
        };
        BinaryOperator<Object> bitSetCombiner = (left, right) -> {
            ((BitSet) left).or((BitSet) right);
            return left;
        };
        Function<Object, String> bitSetFinisher = bits -> {
            BitSet result = (BitSet) bits;
            StringBuilder value = new StringBuilder(result.length());
            for (int index = 0; index < result.length(); index++) {
                value.append(result.get(index) ? '1' : '0');
            }
            return value.toString();
        };
        Collector<String, ?, String> toBitString =
                new CollectorImpl<>(bitSetSupplier, bitSetAccumulator, bitSetCombiner,
                        bitSetFinisher, Collections.emptySet());

        String bitString = Arrays.stream(bitList.split(",")).collect(toBitString);
        assertThat(bitString, is("01011000101001111000011100000000100001110111010101")

        );
    }
}
