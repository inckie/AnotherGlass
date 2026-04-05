package com.damn.anotherglass.shared.rpc

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.Strictness
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

// Basic JSON Lines message serializer
internal class JsonMessageSerializer(inputStream: InputStream, outputStream: OutputStream) :
    IMessageSerializer {
    private val gson: Gson
    private val writer: OutputStreamWriter
    private val reader: BufferedReader

    init {
        gson = GsonBuilder()
            .registerTypeAdapter(RPCMessage::class.java, RPCMessageDeserializer())
            .registerTypeAdapter(ByteArray::class.java, ByteArrayBase64Adapter())
            .setStrictness(Strictness.LENIENT)
            .create()
        writer = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
        reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
    }

    @Throws(Exception::class)
    override fun writeMessage(message: RPCMessage?) {
        gson.toJson(message, writer)
        writer.write("\n") // maybe use Record Separator code?
        writer.flush()
    }

    @Throws(Exception::class)
    override fun readMessage(): RPCMessage? {
        // do not read from the stream directly,
        // since it will look like multiple concatenated JSONs
        val line = reader.readLine()
        try {
            return gson.fromJson(line, RPCMessage::class.java)
        } catch (e: JsonParseException) {
            throw Exception("Unable to parse message: $line", e)
        } catch (e: NullPointerException) {
            throw Exception("Unable to parse message: $line", e)
        }
    }

    private class RPCMessageDeserializer : JsonDeserializer<RPCMessage?> {
        @Throws(JsonParseException::class)
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type?,
            context: JsonDeserializationContext
        ): RPCMessage {
            val jsonObject = json.getAsJsonObject()

            if (!jsonObject.has("service") || jsonObject.get("service")
                    .isJsonNull
            ) return RPCMessage(null, null) // disconnect message received


            val service = jsonObject.get("service").asString
            var type: String? = null
            if (jsonObject.has("type") && !jsonObject.get("type").isJsonNull) {
                type = jsonObject.get("type").asString
            }

            var payload: Any? = null
            if (type != null && jsonObject.has("payload") && !jsonObject.get("payload")
                    .isJsonNull
            ) {
                try {
                    val payloadClass = Class.forName(type) // todo: cache class lookup
                    payload = context.deserialize<Any?>(jsonObject.get("payload"), payloadClass)
                } catch (e: ClassNotFoundException) {
                    throw JsonParseException("Unable to find class: $type for RPCMessage payload", e)
                }
            }
            return RPCMessage(service, type, payload)
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private class ByteArrayBase64Adapter : JsonSerializer<ByteArray?>,
        JsonDeserializer<ByteArray?> {
        override fun serialize(
            src: ByteArray?,
            typeOfSrc: Type?,
            context: JsonSerializationContext?
        ): JsonElement {
            if (src == null) {
                return JsonNull.INSTANCE
            }
            return JsonPrimitive(Base64.encode(src))
        }

        @Throws(JsonParseException::class)
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): ByteArray? {
            if (json == null || json.isJsonNull) {
                return null
            }

            if (json.isJsonPrimitive && json.getAsJsonPrimitive().isString) {
                try {
                    return Base64.decode(json.asString)
                } catch (e: IllegalArgumentException) {
                    throw JsonParseException("Invalid Base64 byte[] value", e)
                }
            }

            if (json.isJsonArray) {
                val result = ByteArray(json.getAsJsonArray().size())
                for (i in 0..<json.getAsJsonArray().size()) {
                    val value = json.getAsJsonArray().get(i).asInt
                    if (value < -128 || value > 255) {
                        throw JsonParseException("byte[] element out of range: $value")
                    }
                    result[i] = value.toByte()
                }
                return result
            }

            throw JsonParseException("Expected Base64 string or numeric array for byte[]")
        }
    }

}
