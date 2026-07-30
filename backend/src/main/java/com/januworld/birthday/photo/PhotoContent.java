package com.januworld.birthday.photo;

public record PhotoContent(
        String fileName,
        String contentType,
        byte[] data
) {}
