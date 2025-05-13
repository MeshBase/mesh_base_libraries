package io.github.meshbase.mesh_base_core.router;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

public class RawBytesBody implements MeshSerializer<RawBytesBody> {
  private static final int LENGTH_FIELD_SIZE = 4;

  private final byte[] content;

  public RawBytesBody(byte[] content) {
    this.content = content != null ? content : new byte[0];
  }

  public static RawBytesBody decode(byte[] data) {
    ByteBuffer buffer = ByteBuffer.wrap(data);
    int length = buffer.getInt();
    byte[] content = new byte[length];
    if (length > 0) {
      buffer.get(content);
    }
    return new RawBytesBody(content);
  }

  @Override
  public byte[] encode() {
    int length = content.length;
    ByteBuffer buffer = ByteBuffer.allocate(LENGTH_FIELD_SIZE + length);
    buffer.putInt(length);
    buffer.put(content);
    return buffer.array();
  }

  public byte[] getContent() {
    return content;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RawBytesBody that = (RawBytesBody) o;
    return Arrays.equals(content, that.content);
  }

  @Override
  public int hashCode() {
    return Objects.hash(Arrays.hashCode(content));
  }

  @Override
  public String toString() {
    return "RawBytesBody[length=" + content.length + "]=["+ Arrays.toString(content) +"]";
  }
}
