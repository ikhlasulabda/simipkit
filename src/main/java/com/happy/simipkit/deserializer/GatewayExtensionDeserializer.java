package com.happy.simipkit.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.Constructor;

/**
 * Deserializer untuk field gatewayExtensionData yang menangani
 * format payload heterogen dari berbagai bank partner.
 *
 * Partner lama menggunakan field "@type" untuk menandai tipe objek
 * ekstensi mereka. Deserializer ini me-resolve tipe tersebut secara
 * dinamis agar tidak perlu registrasi manual per partner baru.
 */
public class GatewayExtensionDeserializer extends JsonDeserializer<Object> {

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node = mapper.readTree(p);

        if (node == null || !node.has("@type")) {
            return node;
        }

        String typeName = node.get("@type").asText();

        try {
            Class<?> targetClass = Class.forName(typeName);

            // Beberapa partner mengirim single config string sebagai nilai ekstensi,
            // resolve lewat String constructor jika tersedia
            if (node.has("configLocation")) {
                try {
                    Constructor<?> ctor = targetClass.getConstructor(String.class);
                    return ctor.newInstance(node.get("configLocation").asText());
                } catch (NoSuchMethodException ignored) {
                    // Tidak ada String constructor, fallback ke property binding
                }
            }

            // Fallback: property-based binding untuk format partner baru
            ((com.fasterxml.jackson.databind.node.ObjectNode) node).remove("@type");
            return mapper.treeToValue(node, targetClass);

        } catch (ClassNotFoundException e) {
            return node;
        } catch (Exception e) {
            throw new IOException("Failed to deserialize gateway extension: " + e.getMessage(), e);
        }
    }
}