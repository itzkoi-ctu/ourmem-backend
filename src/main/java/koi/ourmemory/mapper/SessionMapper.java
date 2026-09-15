package koi.ourmemory.mapper;

import koi.ourmemory.dto.request.CreateSessionRequest;
import koi.ourmemory.dto.response.SessionResponse;
import koi.ourmemory.entity.PhotoSession;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface SessionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "coverPhotoUrl", ignore = true)
    @Mapping(target = "isPublic", constant = "false")
    @Mapping(target = "videoUrl", ignore = true)
    @Mapping(target = "videoThumbnailUrl", ignore = true)
    @Mapping(target = "videoPublicId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "photos", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PhotoSession toEntity(CreateSessionRequest request);

    @Mapping(target = "createdByName", source = "createdBy.displayName")
    @Mapping(target = "photoCount", expression = "java(session.getPhotos() != null ? session.getPhotos().size() : 0)")
    SessionResponse toResponse(PhotoSession session);
}
