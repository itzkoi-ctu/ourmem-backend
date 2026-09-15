package koi.ourmemory.mapper;

import koi.ourmemory.dto.request.CreateMilestoneRequest;
import koi.ourmemory.dto.response.MilestoneResponse;
import koi.ourmemory.entity.Milestone;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Mapper(componentModel = "spring")
public interface MilestoneMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Milestone toEntity(CreateMilestoneRequest request);

    @Mapping(target = "daysUntil", expression = "java(calculateDaysUntil(milestone.getTargetDate()))")
    MilestoneResponse toResponse(Milestone milestone);

    default long calculateDaysUntil(LocalDate targetDate) {
        return ChronoUnit.DAYS.between(LocalDate.now(), targetDate);
    }
}
