package com.damn.anotherglass.shared.rpc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

// Basic JSON Lines message serializer
class JsonMessageSerializer implements IMessageSerializer {
    private final Gson gson;
    private final OutputStreamWriter writer;
    private final BufferedReader reader;

    public JsonMessageSerializer(InputStream inputStream, OutputStream outputStream) {
        gson = new GsonBuilder()
                .registerTypeAdapter(RPCMessage.class, new RPCMessageDeserializer())
                .setStrictness(Strictness.LENIENT)
                .create();
        writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    @Override
    public void writeMessage(RPCMessage message) throws Exception {
        gson.toJson(message, writer);
        writer.write("\n"); // maybe use Record Separator code?
        writer.flush();
    }

    @Override
    public RPCMessage readMessage() throws Exception {
        // do not read from the stream directly,
        // since it will looks like multiple concatenated jsons
        String line = reader.readLine();
        return gson.fromJson(line, RPCMessage.class);
    }

    private static class RPCMessageDeserializer implements JsonDeserializer<RPCMessage> {
        @Override
        public RPCMessage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();

            String service = jsonObject.get("service").getAsString();
            String type = null;
            if (jsonObject.has("type") && !jsonObject.get("type").isJsonNull()) {
                type = jsonObject.get("type").getAsString();
            }

            Object payload = null;
            if (type != null && jsonObject.has("payload") && !jsonObject.get("payload").isJsonNull()) {
                try {
                    Class<?> payloadClass = Class.forName(type); // todo: cache class lookup
                    payload = context.deserialize(jsonObject.get("payload"), payloadClass);
                } catch (ClassNotFoundException e) {
                    throw new JsonParseException("Unable to find class: " + type + " for RPCMessage payload", e);
                }
            }
            return new RPCMessage(service, type, payload);
        }
    }
}
