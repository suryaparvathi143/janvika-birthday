package com.januworld.birthday.photo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class PhotoRepository {
    private final JdbcTemplate jdbcTemplate;

    public PhotoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PhotoMetadata> findAll() {
        return jdbcTemplate.query("""
                SELECT id, file_name, content_type, file_size, created_at
                FROM gallery_photos
                ORDER BY created_at DESC
                """, (resultSet, rowNumber) -> new PhotoMetadata(
                resultSet.getLong("id"),
                resultSet.getString("file_name"),
                resultSet.getString("content_type"),
                resultSet.getLong("file_size"),
                resultSet.getTimestamp("created_at").toInstant()
        ));
    }

    public Optional<PhotoContent> findContent(long id) {
        return jdbcTemplate.query("""
                SELECT file_name, content_type, photo_data
                FROM gallery_photos
                WHERE id = ?
                """, resultSet -> resultSet.next()
                ? Optional.of(new PhotoContent(
                        resultSet.getString("file_name"),
                        resultSet.getString("content_type"),
                        resultSet.getBytes("photo_data")))
                : Optional.empty(), id);
    }

    public PhotoMetadata save(String fileName, String contentType, byte[] data) {
        Instant createdAt = Instant.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO gallery_photos
                        (file_name, content_type, file_size, photo_data, created_at)
                    VALUES (?, ?, ?, ?, ?)
                    """, new String[]{"id"});
            statement.setString(1, fileName);
            statement.setString(2, contentType);
            statement.setLong(3, data.length);
            statement.setBytes(4, data);
            statement.setTimestamp(5, Timestamp.from(createdAt));
            return statement;
        }, keyHolder);

        Number id = keyHolder.getKey();
        if (id == null) {
            throw new IllegalStateException("Database did not return a photo id");
        }
        return new PhotoMetadata(id.longValue(), fileName, contentType, data.length, createdAt);
    }
}
