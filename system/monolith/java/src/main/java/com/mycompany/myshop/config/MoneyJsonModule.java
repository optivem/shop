package com.mycompany.myshop.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Writes every {@link BigDecimal} in an API response as a JSON string with an explicit scale, so money
 * and rates reach consumers exactly as the domain states them ({@code "22.00"}, {@code "0.2000"}).
 * The scale is keyed by property name and mirrors the {@code DECIMAL_FIELDS} map in the TypeScript
 * backends and the DB columns ({@code NUMERIC(10,2)} amounts, {@code NUMERIC(5,4)} rates). Any other
 * {@code BigDecimal} is written as {@link BigDecimal#toPlainString()}.
 *
 * <p>Spring Boot registers this module on its auto-configured {@code ObjectMapper} only; the outbound
 * ERP/Tax/Clock gateways build their own mapper and are unaffected. Reading is unchanged.
 */
@Component
public class MoneyJsonModule extends SimpleModule {

    static final Map<String, Integer> DECIMAL_FIELDS = Map.of(
            "price", 2,
            "unitPrice", 2,
            "basePrice", 2,
            "discountRate", 4,
            "discountAmount", 2,
            "subtotalPrice", 2,
            "taxRate", 4,
            "taxAmount", 2,
            "totalPrice", 2);

    public MoneyJsonModule() {
        super("MoneyJsonModule");
        addSerializer(BigDecimal.class, new DecimalStringSerializer(null));
    }

    static final class DecimalStringSerializer extends StdSerializer<BigDecimal> implements ContextualSerializer {

        private final transient Integer scale;

        DecimalStringSerializer(Integer scale) {
            super(BigDecimal.class);
            this.scale = scale;
        }

        @Override
        public JsonSerializer<?> createContextual(SerializerProvider provider, BeanProperty property) {
            if (property == null) {
                return this;
            }
            return new DecimalStringSerializer(DECIMAL_FIELDS.get(property.getName()));
        }

        @Override
        public void serialize(BigDecimal value, JsonGenerator generator, SerializerProvider provider) throws IOException {
            var scaled = scale == null ? value : value.setScale(scale, RoundingMode.HALF_UP);
            generator.writeString(scaled.toPlainString());
        }
    }
}
