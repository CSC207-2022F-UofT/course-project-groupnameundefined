package backend.service.imp;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

import backend.exception.exceptions.EntityNotFoundException;
import backend.form.CourseForm.*;
import com.google.gson.GsonBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import backend.model.Course;
import backend.model.Section;
import backend.model.SectionBlock;
import backend.repository.CourseRepository;
import backend.repository.SectionBlockRepository;
import backend.repository.SectionRepository;
import backend.service.CourseService;

import com.google.gson.Gson;

@Service
public class CourseServiceImp implements CourseService {

	private final Logger logger;

	private final CourseRepository courseRepository;
	private final SectionRepository sectionRepository;
	private final SectionBlockRepository sectionBlockRepository;

	public CourseServiceImp(
			Logger logger,
			CourseRepository courseRepository,
			SectionRepository sectionRepository,
			SectionBlockRepository sectionBlockRepository
	) {
		this.logger = logger;
		this.courseRepository = courseRepository;
		this.sectionRepository = sectionRepository;
		this.sectionBlockRepository = sectionBlockRepository;
	}

	@Value(value = "classpath:data/courses.json")
	private Resource courseResource;

	JSONParser parser = new JSONParser();

	@Override
	public List<Course> getAllCourses() {
		return courseRepository.findAll();
	}

	@Override
	public Course getCourseById(Long id) {
		Optional<Course> course = courseRepository.findById(id);
		if (course.isPresent()) {
			return course.get();
		}

		throw new EntityNotFoundException(String.format("Unable to find course with id '%d'.", id), Course.class);
	}

	@Override
	public List<Course> loadCourses(LoadCoursesForm input) {
		try {
			Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

			File courseDataFile = courseResource.getFile();
			Reader courseReader = new FileReader(courseDataFile);

			Object courseData = parser.parse(courseReader);

			JSONArray courses = (JSONArray) courseData;

			for (Object c : courses) {
				JSONObject _course = (JSONObject) c;
				JSONArray courseSections = (JSONArray) _course.get("sections");

				Course course = gson.fromJson(_course.toJSONString(), Course.class);

				if (!input.getLoadAll() && !input.getCourses().contains(course.getCode())) {
					continue;
				}

				courseRepository.save(course);

				for (Object s : courseSections) {
					JSONObject _section = (JSONObject) s;
					JSONArray sectionBlockData = (JSONArray) _section.get("sectionBlocks");

					Section section = gson.fromJson(_section.toJSONString(), Section.class);
					section.setCourse(course);
					sectionRepository.save(section);

					if (sectionBlockData != null) {
						for (Object secB : sectionBlockData) {
							JSONObject _sectionBlock = (JSONObject) secB;

							SectionBlock sectionBlock = gson.fromJson(_sectionBlock.toJSONString(), SectionBlock.class);
							sectionBlock.setSection(section);
							sectionBlockRepository.save(sectionBlock);
						}
					}
				}
			}
			return getAllCourses();
		} catch (IOException | ParseException e) {
			// Chained exception captured by APIExceptionHandler
			throw new RuntimeException(e);
		}
	}

}
