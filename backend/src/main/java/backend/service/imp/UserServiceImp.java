package backend.service.imp;

import java.util.List;
import java.util.Optional;

import backend.exception.exceptions.EntityExistException;
import backend.exception.exceptions.EntityNotFoundException;
import backend.exception.exceptions.InvalidCredentialsException;
import backend.form.AppointmentRequestForm.*;
import backend.form.UserForm.*;

import backend.model.AppointmentBlock;
import backend.model.AppointmentRequest;
import backend.model.Timetable;
import backend.service.AppointmentRequestService;
import backend.service.UserService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import backend.model.User;
import backend.repository.UserRepository;

@Service
public class UserServiceImp implements UserService {

	private final Logger logger;

	private final UserRepository userRepository;

	private final AppointmentRequestService appointmentRequestService;

	@Autowired
	public UserServiceImp(Logger logger, UserRepository userRepository,
						  AppointmentRequestService appointmentRequestService) {
		this.logger = logger;
		this.userRepository = userRepository;
		this.appointmentRequestService = appointmentRequestService;

	}

	@Override
	public User registerUser(RegisterForm input) {
		if (userRepository.existsByName(input.getName())) {
			throw new EntityExistException(String.format("User with name '%s' already exist.", input.getName()), User.class);
		} else if (userRepository.existsByEmail(input.getEmail())) {
			throw new EntityExistException(String.format("User with email '%s' already exist.", input.getEmail()), User.class);
		} else if (userRepository.existsByPhone(input.getPhone())) {
			throw new EntityExistException(String.format("User with phone '%s' already exist.", input.getPhone()), User.class);
		} else {
			User user = new User(input.getName(), input.getEmail(), input.getPassword(), input.getPhone());
			return userRepository.save(user);
		}
	}

	@Override
	public Optional<User> authenticateUser(String email, String password) {
		try {
			User user = this.getUserByEmail(email);
			if (user.getPassword().equals(password)) {
				return Optional.of(user);
			}
		} catch (EntityNotFoundException e) {
			return Optional.empty();
		}

		return Optional.empty();
	}

	@Override
	public User loginUser(LoginForm input) {
		Optional<User> _user = authenticateUser(input.getEmail(), input.getPassword());
		if (_user.isEmpty()) {
			throw new InvalidCredentialsException("Invalid Credentials. Please check your email and password.");
		}

		User user = _user.get();
		user.setLoginStatus(true);

		return userRepository.save(user);
	}

	@Override
	public Long logoutUser(Long id) {
		User user = this.getUserById(id);
		user.setLoginStatus(false);
		userRepository.save(user);

		return id;
	}

	@Override
	public List<User> getAllUsers() {
		return userRepository.findAll();
	}

	@Override
	public User getUserById(Long id) {
		Optional<User> user = userRepository.findById(id);
		if (user.isPresent()) {
			return user.get();
		}

		throw new EntityNotFoundException(String.format("Unable to find user with id '%d'.", id), User.class);
	}

	@Override
	public User getUserByEmail(String email) {
		Optional<User> user = userRepository.findByEmail(email);
		if (user.isPresent()) {
			return user.get();
		}

		throw new EntityNotFoundException(String.format("Unable to find user with email '%s'.", email), User.class);
	}

	@Override
	public AppointmentRequest sendAptRequest(CreateAppointmentRequestForm input) {
		AppointmentRequest apt = appointmentRequestService.createAppointmentRequest(input);

		User user = apt.getFrom();
		Timetable timetable = user.getStudentProfile().getTimetable();
		AppointmentBlock aptBlock = new AppointmentBlock();
		timetable.addBlock(aptBlock);
		userRepository.save(user);

		return apt;
	}

	@Override
	public AppointmentRequest updateAptRequest(UpdateAppointmentRequestForm input) {
		AppointmentRequest apt = appointmentRequestService.updateAppointmentRequest(input);

		User user = apt.getFrom();
		Timetable timetable = user.getStudentProfile().getTimetable();
		AppointmentBlock aptBlock = new AppointmentBlock();
		timetable.addBlock(aptBlock);
		userRepository.save(user);

		return apt;
	}

}
