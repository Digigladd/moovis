package net.busynot.moovis.eptica.supervision.api;

import akka.util.ByteString;
import com.lightbend.lagom.javadsl.api.deser.MessageSerializer;
import com.lightbend.lagom.javadsl.api.deser.SerializationException;
import com.lightbend.lagom.javadsl.api.deser.StrictMessageSerializer;
import com.lightbend.lagom.javadsl.api.transport.MessageProtocol;
import com.lightbend.lagom.javadsl.api.transport.NotAcceptable;
import com.lightbend.lagom.javadsl.api.transport.UnsupportedMediaType;

import java.util.List;

public class PlainTextSerializer implements StrictMessageSerializer<String> {

    private final NegotiatedSerializer<String, ByteString> serializer = new NegotiatedSerializer<String, ByteString>() {

        @Override
        public ByteString serialize(String s) throws SerializationException {
            return ByteString.fromString(s);
        }
    };

    private final NegotiatedDeserializer<String, ByteString> deserializer =
            bytes -> bytes.utf8String();

    @Override
    public NegotiatedSerializer<String, ByteString> serializerForRequest() {
        return serializer;
    }

    @Override
    public NegotiatedDeserializer<String, ByteString> deserializer(MessageProtocol protocol) throws UnsupportedMediaType {
        return deserializer;
    }

    @Override
    public NegotiatedSerializer<String, ByteString> serializerForResponse(List<MessageProtocol> acceptedMessageProtocols) throws NotAcceptable {
        return serializer;
    }
}
