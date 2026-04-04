package com.damn.anotherglass.shared.rpc;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.damn.anotherglass.shared.BinaryData;
import com.damn.anotherglass.shared.notifications.NotificationData;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class JsonMessageSerializerTest {

    @Test
    public void readMessage_acceptsBase64Bytes() throws Exception {
        String json = "{\"service\":\"notifications\",\"type\":\"com.damn.anotherglass.shared.notifications.NotificationData\",\"payload\":{\"action\":\"Posted\",\"icon\":{\"bytes\":\"AQID/w==\",\"mimeType\":\"image/png\"}}}";

        JsonMessageSerializer serializer = new JsonMessageSerializer(
                new ByteArrayInputStream((json + "\n").getBytes(StandardCharsets.UTF_8)),
                new ByteArrayOutputStream());

        RPCMessage message = serializer.readMessage();
        NotificationData payload = (NotificationData) message.payload;

        assertEquals("notifications", message.service);
        assertEquals(NotificationData.Action.Posted, payload.action);
        assertArrayEquals(new byte[]{1, 2, 3, -1}, payload.icon.bytes);
        assertEquals("image/png", payload.icon.mimeType);
    }

    @Test
    public void readMessage_acceptsLegacyNumericArrayBytes() throws Exception {
        String json = "{\"service\":\"notifications\",\"type\":\"com.damn.anotherglass.shared.notifications.NotificationData\",\"payload\":{\"action\":\"Posted\",\"icon\":{\"bytes\":[1,2,3,-1],\"mimeType\":\"image/png\"}}}";

        JsonMessageSerializer serializer = new JsonMessageSerializer(
                new ByteArrayInputStream((json + "\n").getBytes(StandardCharsets.UTF_8)),
                new ByteArrayOutputStream());

        RPCMessage message = serializer.readMessage();
        NotificationData payload = (NotificationData) message.payload;

        assertArrayEquals(new byte[]{1, 2, 3, -1}, payload.icon.bytes);
    }

    @Test
    public void writeMessage_serializesBytesAsBase64String() throws Exception {
        NotificationData payload = new NotificationData();
        payload.action = NotificationData.Action.Posted;
        payload.icon = new BinaryData(new byte[]{1, 2, 3, -1}, "image/png");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        JsonMessageSerializer serializer = new JsonMessageSerializer(
                new ByteArrayInputStream(new byte[0]),
                output);

        serializer.writeMessage(new RPCMessage("notifications", payload));

        String jsonLine = output.toString(StandardCharsets.UTF_8);
        JsonObject root = new Gson().fromJson(jsonLine, JsonObject.class);
        String encodedBytes = root
                .getAsJsonObject("payload")
                .getAsJsonObject("icon")
                .get("bytes")
                .getAsString();

        assertEquals("AQID/w==", encodedBytes);
        assertTrue(jsonLine.endsWith("\n"));
    }
}

