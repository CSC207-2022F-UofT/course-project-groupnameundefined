package backend;

import backend.form.HabitForm;
import backend.form.StudentProfileForm;
import backend.form.UserForm;
import backend.model.StudentProfile;
import backend.model.User;
import backend.repository.StudentProfileRepository;
import backend.service.HabitService;
import backend.service.StudentProfileService;
import backend.service.UserService;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInRelativeOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class StudentProfileMatchingTests extends ControllerIntegrationTest {

	@Autowired
	private Logger logger;

	@Autowired
	private UserService userService;

	@Autowired
	private StudentProfileService studentProfileService;

	@Autowired
	private HabitService habitService;

	@Autowired
	private StudentProfileRepository studentProfileRepository;

	/**
	 * Sets up users and student profiles before testing matches
	 */
	@BeforeEach
	void matchingTests_setup() {
		for (int i = 1; i < 6; i++) {
			UserForm.RegisterForm registerForm = new UserForm.RegisterForm(
					"Test Name " + i,
					"test.name" + i + "@email.com",
					"1234567Abc",
					"012345678" + i
			);
			User user = userService.registerUser(registerForm);

			StudentProfileForm.CreateStudentProfileForm cspForm = new StudentProfileForm.CreateStudentProfileForm(
					user.getId(),
					"CS",
					"Woodsworth",
					2020
			);
			StudentProfile studentProfile = studentProfileService.createStudentProfile(cspForm);

			HabitForm.CreateHabitForm createHabitForm = new HabitForm.CreateHabitForm(
					studentProfile.getId(),
					i,
					i
			);
			habitService.createHabit(createHabitForm);
		}
	}

	@Test
	@Order(1)
	@DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
	public void matchByHabits_asc_expectSuccess() throws Exception {
		List<StudentProfile> studentProfiles = studentProfileService.matchStudentProfiles(2L, "HABIT");
		List<Long> studentProfileIds = studentProfiles.stream().map(StudentProfile::getId).toList();

		assertThat(studentProfileIds, containsInRelativeOrder(1L, 3L, 4L, 5L));
	}

	@Test
	@Order(2)
	@DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
	public void matchByHabits_desc_expectSuccess() throws Exception {
		List<StudentProfile> studentProfiles = studentProfileService.matchStudentProfiles(4L, "HABIT");
		List<Long> studentProfileIds = studentProfiles.stream().map(StudentProfile::getId).toList();

		assertThat(studentProfileIds, containsInRelativeOrder(3L, 5L, 2L, 1L));
	}

	@Test
	@Order(3)
	@DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
	public void matchByCourses_asc_expectSuccess() throws Exception {
		// Assign courses to match target (which student profile to get matches for)
		Set<String> targetCourses2 = Set.of("a", "b", "c", "d");
		// Student Profiles #1 has courses exactly the same as target, #2 has one less, and so on.
		Set<String> courses1 = Set.of("a", "b", "c", "d");
		Set<String> courses3 = Set.of("a", "b", "c", "e");
		Set<String> courses4 = Set.of("a", "b", "e", "f");
		Set<String> courses5 = Set.of("a", "e", "f", "g");

		List<Set<String>> courseData = List.of(courses1, targetCourses2, courses3, courses4, courses5);

		for (long i = 1L; i <= 5L; i++) {
			StudentProfile studentProfile = studentProfileService.getStudentProfileById(i);
			studentProfile.setCourseCodes(courseData.get((int) i - 1));
			studentProfileRepository.save(studentProfile);
		}

		List<StudentProfile> studentProfiles = studentProfileService.matchStudentProfiles(2L, "COURSE");
		List<Long> studentProfileIds = studentProfiles.stream().map(StudentProfile::getId).toList();

		logger.info(studentProfileIds.toString());
		assertThat(studentProfileIds, containsInRelativeOrder(1L, 3L, 4L, 5L));
	}

	@Test
	@Order(4)
	@DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
	public void matchStudentProfilesByCourses_desc_expectSuccess() throws Exception {
		// Assign courses to match target (which student profile to get matches for)
		Set<String> targetCourses2 = Set.of("a", "b", "c", "d");
		// Student Profiles #5 has courses exactly the same as target, #4 has one less, and so on.
		Set<String> courses1 = Set.of("a", "e", "f", "g");
		Set<String> courses3 = Set.of("a", "b", "e", "f");
		Set<String> courses4 = Set.of("a", "b", "c", "e");
		Set<String> courses5 = Set.of("a", "b", "c", "d");

		List<Set<String>> courseData = List.of(courses1, targetCourses2, courses3, courses4, courses5);
		for (long i = 1L; i <= 5L; i++) {
			StudentProfile studentProfile = studentProfileService.getStudentProfileById(i);
			studentProfile.setCourseCodes(courseData.get((int) i - 1));
			studentProfileRepository.save(studentProfile);
		}

		List<StudentProfile> studentProfiles = studentProfileService.matchStudentProfiles(2L, "COURSE");
		List<Long> studentProfileIds = studentProfiles.stream().map(StudentProfile::getId).toList();

		assertThat(studentProfileIds, containsInRelativeOrder(5L, 4L, 3L, 1L));
	}

	@Test
	@Order(5)
	@DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
	public void matchStudentProfiles_expectSuccess() throws Exception {
		// Make Habit #5 has same attrs as Habit #4
		HabitForm.UpdateHabitForm input = new HabitForm.UpdateHabitForm(
				5L,
				4,
				4
		);

		habitService.updateHabit(input);

		// Assign courses to match target (which student profile to get matches for)
		Set<String> targetCourses2 = Set.of("a", "b", "c", "d");

		// Reverse StudentProfiles #4 and #5 given that they have the same habit attrs
		// (StudentProfile #5 has a closer match so should come before #4)
		Set<String> courses4 = Set.of("a", "b", "c", "e");
		Set<String> courses5 = Set.of("a", "b", "c", "d");

		// Rest are irrelevant
		Set<String> courses1 = Set.of("a", "b", "c", "d");
		Set<String> courses3 = Set.of("a", "b", "c", "d");

		List<Set<String>> courseData = List.of(courses1, targetCourses2, courses3, courses4, courses5);
		for (long i = 1L; i <= 5L; i++) {
			StudentProfile studentProfile = studentProfileService.getStudentProfileById(i);
			studentProfile.setCourseCodes(courseData.get((int) i - 1));
			studentProfileRepository.save(studentProfile);
		}

		List<StudentProfile> studentProfiles = studentProfileService.matchStudentProfiles(2L, "BOTH");
		List<Long> studentProfileIds = studentProfiles.stream().map(StudentProfile::getId).toList();

		assertThat(studentProfileIds, containsInRelativeOrder(1L, 3L, 5L, 4L));
	}

}
