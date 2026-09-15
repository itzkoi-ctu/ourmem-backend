package koi.ourmemory.mapper;

import koi.ourmemory.dto.response.NoteResponse;
import koi.ourmemory.entity.LoveNote;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NoteMapper {

    @Mapping(target = "writtenByName", source = "writtenBy.displayName")
    NoteResponse toResponse(LoveNote note);
}
