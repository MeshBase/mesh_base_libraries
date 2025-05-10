package com.example.mesh_base.router;


import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.util.UUID;
import java.util.function.Function;

public class ProtocolTest {
    private byte[] getDummyByteArray() {
        return new byte[]{
                0x00, 0x00, 0x00, 0x01,  // messageType = 1
                0x00, 0x00, 0x00, 0x04,  // remainingHops = 4
                0x00, 0x00, 0x00, 0x71,  // messageId = 113
                (byte) 0xdd, (byte) 0x91, (byte) 0xa1, (byte) 0xc8, (byte) 0x5f, (byte) 0x6a, (byte) 0x44, (byte) 0x30,  // sender (UUID high bits)
                (byte) 0x81, (byte) 0x5f, (byte) 0xf3, (byte) 0xe1, (byte) 0xc8, (byte) 0x78, (byte) 0x0f, (byte) 0xc8,  // sender (UUID low bits)
                0x00, 0x00, 0x00, 0x24, // bodyLength = 36
                0x00, 0x00, 0x00, 0x04,  // command = 4
                0x00,  // isBroadcast = false
                (byte) 0xa9, (byte) 0xc5, (byte) 0xc7, (byte) 0x10, (byte) 0x07, (byte) 0x53, (byte) 0x43, (byte) 0x43,  // destination (UUID high bits)
                (byte) 0xab, (byte) 0x51, (byte) 0x31, (byte) 0x4e, (byte) 0xc4, (byte) 0x23, (byte) 0xb7, (byte) 0x32,  // destination (UUID low bits)
                0x00, 0x00, 0x00, 0x0B,  // msg length = 11
                (byte) 0x68, (byte) 0x65, (byte) 0x6c, (byte) 0x6c, (byte) 0x6f, (byte) 0x20, (byte) 0x77, (byte) 0x6f,  // "hello"
                (byte) 0x72, (byte) 0x6c, (byte) 0x64   // "world"
        };
    }

    private MeshProtocol<SendMessageBody> getDummySendMessageBody() {
        SendMessageBody messageBody = new SendMessageBody(4,
                false,
                "hello world"
        );
        return new ConcreteMeshProtocol<>(
                1,
                4,
                113,
                UUID.fromString("a9c5c710-0753-4343-ab51-314ec423b732"),
                UUID.fromString("dd91a1c8-5f6a-4430-815f-f3e1c8780fc8"),
                messageBody
        );
    }

    @Test
    public void testIfProtocolProperlySerializesAndDeserializesData() {
        Function<byte[], SendMessageBody> bodyDecoder = SendMessageBody::decode;
        MeshProtocol<SendMessageBody> actualDecode = MeshProtocol.decode(getDummyByteArray(), bodyDecoder);
        MeshProtocol<SendMessageBody> expectedDecode = getDummySendMessageBody();

        Assertions.assertEquals(expectedDecode, actualDecode);

        byte[] actualEncode = getDummySendMessageBody().encode();
        byte[] expectedEncode = getDummyByteArray();

        Assertions.assertArrayEquals(expectedEncode, actualEncode);
    }
}
