package backend.controller;

import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;

import backend.dto.CourseDto;
import backend.model.Block;
import backend.model.Course;

public interface CourseController {

    ResponseEntity<String> loadCourses();

    ResponseEntity<List<CourseDto>> getAllCourses();

}
