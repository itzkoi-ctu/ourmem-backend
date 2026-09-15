package koi.ourmemory.mapper;

import koi.ourmemory.dto.response.PhotoResponse;
import koi.ourmemory.entity.Photo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {NoteMapper.class})
public interface PhotoMapper {

    @Mapping(target = "sessionId", source = "session.id")
    @Mapping(target = "uploadedByName", source = "uploadedBy.displayName")
    @Mapping(target = "reactions", ignore = true)
    PhotoResponse toResponse(Photo photo);
}
