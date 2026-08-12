package com.davidblackcn.lorianarchorbit.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Locale;

final class JsonConfigSupport {
    private JsonConfigSupport() {
    }

    static JsonObject object(JsonObject parent, String key, List<String> warnings) {
        JsonElement value = parent.get(key);
        if (value != null && value.isJsonObject()) {
            return value.getAsJsonObject();
        }
        if (value != null) {
            warnings.add(key + " must be an object; using defaults");
        }
        JsonObject result = new JsonObject();
        parent.add(key, result);
        return result;
    }

    static boolean bool(JsonObject object, String key, boolean fallback, String path, List<String> warnings) {
        JsonElement value = object.get(key);
        if (value == null) {
            object.addProperty(key, fallback);
            return fallback;
        }
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) {
            return value.getAsBoolean();
        }
        warnings.add(path + " must be a boolean; using " + fallback);
        object.addProperty(key, fallback);
        return fallback;
    }

    static int integer(
            JsonObject object,
            String key,
            int fallback,
            int minimum,
            int maximum,
            String path,
            List<String> warnings
    ) {
        JsonElement value = object.get(key);
        int parsed = fallback;
        boolean valid = false;
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            try {
                parsed = value.getAsInt();
                valid = true;
            } catch (NumberFormatException ignored) {
                // The warning below reports the complete field path.
            }
        }
        if (!valid && value != null) {
            warnings.add(path + " must be an integer; using " + fallback);
        }
        int clamped = Math.max(minimum, Math.min(maximum, parsed));
        if (valid && clamped != parsed) {
            warnings.add(path + " was clamped from " + parsed + " to " + clamped);
        }
        object.addProperty(key, clamped);
        return clamped;
    }

    static <E extends Enum<E>> E enumValue(
            JsonObject object,
            String key,
            E fallback,
            Class<E> type,
            String path,
            List<String> warnings
    ) {
        JsonElement value = object.get(key);
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            try {
                E parsed = Enum.valueOf(type, value.getAsString().toUpperCase(Locale.ROOT));
                object.addProperty(key, parsed.name().toLowerCase(Locale.ROOT));
                return parsed;
            } catch (IllegalArgumentException ignored) {
                // The warning below reports the supported fallback.
            }
        }
        if (value != null) {
            warnings.add(path + " has an unsupported value; using " + fallback.name().toLowerCase(Locale.ROOT));
        }
        object.addProperty(key, fallback.name().toLowerCase(Locale.ROOT));
        return fallback;
    }

    static int version(JsonObject root) {
        JsonElement value = root.get("config_version");
        if (value == null) {
            return 0;
        }
        try {
            return value.getAsInt();
        } catch (RuntimeException exception) {
            throw new ConfigException("config_version must be an integer", exception);
        }
    }

    static void requireSupportedVersion(int version) {
        if (version < 0 || version > ConfigConstants.CURRENT_VERSION) {
            throw new ConfigException(
                    "Unsupported config_version " + version + "; supported through " + ConfigConstants.CURRENT_VERSION
            );
        }
    }
}
