package backend.mappers;

import java.util.List;
import java.util.Set;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import backend.dto.SectionDto;
import backend.model.Section;

@Mapper(uses = {CourseMapper.class})
public interface SectionMapper {

	SectionDto toDto(Section section);

	List<SectionDto> toDtoList(List<Section> sections);
	
}
