package com.januworld.birthday.photo;

import java.time.Instant;

public record PhotoMetadata(
        Long id,
        String fileName,
        String contentType,
        long fileSize,
        Instant createdAt
) {}
