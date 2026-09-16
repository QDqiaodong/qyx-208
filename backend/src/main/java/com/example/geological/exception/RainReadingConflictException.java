package com.example.geological.exception;

/**
 * 同一桶同一自然日的读数被两名值班并发改动/作废：
 * 先提交的生效并把版本号 +1，后提交的版本对不上，报 409，
 * 提示刷新后重试——同一条读数的并发改动只成一次。
 */
public class RainReadingConflictException extends RuntimeException {

    public RainReadingConflictException(String message) {
        super(message);
    }
}
