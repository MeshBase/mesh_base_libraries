package com.example.mesh_base.router;

import java.nio.ByteBuffer;
import java.util.Objects;

public class SendMessageBody implements MeshSerializer<SendMessageBody> {
  private final int command;
  private final boolean isBroadcast;
  private final String msg;

  public SendMessageBody(int command, boolean isBroadcast, String msg) {
    this.command = command;
    this.isBroadcast = isBroadcast;
    this.msg = msg;
  }

  public static SendMessageBody decode(byte[] data) {
    ByteBuffer buffer = ByteBuffer.wrap(data);
    int command = buffer.getInt();
    boolean isBroadcast = buffer.get() == 1;

    int msgLength = buffer.getInt();
    byte[] msgBytes = new byte[msgLength];
    String msg = "";

    if (msgLength > 0) {
      buffer.get(msgBytes);
      msg = new String(msgBytes);
    }

    return new SendMessageBody(command, isBroadcast, msg);
  }

  @Override
  public byte[] encode() {
    int messageLength = msg.length();
    ByteBuffer buffer = ByteBuffer.allocate(4 + 1 + 4 + messageLength);

    buffer.putInt(command);
    buffer.put((byte) (isBroadcast ? 1 : 0));

    buffer.putInt(messageLength);
    buffer.put(msg.getBytes());
    return buffer.array();
  }

  public int getCommand() {
    return command;
  }

  public boolean isBroadcast() {
    return isBroadcast;
  }

  public String getMsg() {
    return msg;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SendMessageBody that = (SendMessageBody) o;
    return command == that.command &&
            isBroadcast == that.isBroadcast &&
            Objects.equals(msg, that.msg);
  }

  @Override
  public int hashCode() {
    return Objects.hash(command, isBroadcast, msg);
  }
}
