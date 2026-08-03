package com.happy.simipkit.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Deserializer untuk field gatewayExtensionData yang menangani
 * format payload heterogen dari berbagai bank partner secara aman.
 *
 * Mengabaikan parsing kelas dinamis via reflection dan hanya memetakan
 * data ekstensi ke tipe generik Map (atau mengembalikan JsonNode).
 */
public class GatewayExtensionDeserializer extends JsonDeserializer<Object> {

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node = mapper.readTree(p);

        if (node == null) {
            return null;
        }

        // Hapus logic Class.forName() & reflection instantiation sepenuhnya untuk mencegah RCE.
        // Jika bertipe object, deserialize ke java.util.Map generik.
        if (node.isObject()) {
            try {
                return mapper.treeToValue(node, java.util.Map.class);
            } catch (Exception e) {
                return node;
            }
        }

        return node;
    }
}