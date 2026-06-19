package com.citygo.tourism.exception;

public class TripPayloadTooLargeException extends RuntimeException {
    private final long bytes;
    private final long maxBytes;

    public TripPayloadTooLargeException(long bytes, long maxBytes) {
        super("行程数据过大，请精简后重新保存。");
        this.bytes = bytes;
        this.maxBytes = maxBytes;
    }

    public long bytes() {
        return bytes;
    }

    public long maxBytes() {
        return maxBytes;
    }
}
