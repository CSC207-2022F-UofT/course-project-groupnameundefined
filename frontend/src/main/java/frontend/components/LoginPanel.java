package frontend.components;

import frontend.exception.ResponseException;
import frontend.schema.UserSchema;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class LoginPanel extends JPanel implements ActionListener {

	private final WebClient webClient;
	private final UserSchema userSchema;
	private final Logger logger;

	private MainPanel mainPanel;

	private static JLabel emailLabel;
	private static JTextField emailField;
	private static JLabel emailError;
	private static JLabel passwordLabel;
	private static JPasswordField passwordField;
	private static JLabel passwordError;
	private static JButton registerButton;
	private static JButton loginButton;
	private static JLabel successLabel;

	@Autowired
	public LoginPanel(WebClient webClient, UserSchema userSchema, Logger logger) {
		this.webClient = webClient;
		this.userSchema = userSchema;
		this.logger = logger;
	}

	public void initialize(MainPanel mainPanel) {
		this.mainPanel = mainPanel;

		this.setBounds(0, 0, 500, 500);
		this.setBackground(new Color(128, 128, 255));
		this.setLayout(null);

		emailLabel = new JLabel("Email: ");
		emailLabel.setBounds(150, 90, 70, 20);

		emailField = new JTextField();
		emailField.setBounds(160, 115, 170, 20);

		emailError = new JLabel("");
		emailError.setBounds(165, 135, 300, 20);

		passwordLabel = new JLabel("Password: ");
		passwordLabel.setBounds(150, 155, 193, 28);

		passwordField = new JPasswordField();
		passwordField.setBounds(160, 180, 170, 20);

		passwordError = new JLabel("");
		passwordError.setBounds(165, 200, 300, 20);

		registerButton = new JButton("Sign Up");
		registerButton.setBounds(160, 230, 80, 30);
		registerButton.addActionListener(this);

		loginButton = new JButton("Sign In");
		loginButton.setBounds(250, 230, 80, 30);
		loginButton.addActionListener(this);

		successLabel = new JLabel("", SwingConstants.CENTER);
		successLabel.setBounds(45, 270, 400, 20);

		this.add(emailLabel);
		this.add(emailField);
		this.add(emailError);
		this.add(passwordLabel);
		this.add(passwordField);
		this.add(passwordError);
		this.add(registerButton);
		this.add(loginButton);
		this.add(successLabel);

		this.setVisible(true);
	}

	/**
	 * Sets this panel to be invisible and return nothing.
	 */
	public void close() {
		this.setVisible(false);
	}

	/**
	 * Cleans all error messages shown in the JLabels and all information typed into the fields.
	 */
	public void cleanAll() {
		emailField.setText("");
		emailError.setText("");
		passwordField.setText("");
		passwordError.setText("");
		successLabel.setText("");
	}

	/**
	 * Only cleans the error messages shown in the JLabels.
	 */
	public void cleanErrors() {
		emailError.setText("");
		passwordError.setText("");
		successLabel.setText("");
	}

	/**
	 * Ban the register button in the login panel.
	 */
	public void disableRegisterButton() {
		registerButton.setEnabled(false);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == loginButton) {
			logger.info("login button clicked");
			this.cleanErrors();
			String email = emailField.getText();
			String password = new String(passwordField.getPassword());

			if (email == null || email.isEmpty() || email.isBlank()) {
				emailError.setText("Email cannot be empty or white spaces");
			} else if (password == null || password.isEmpty() || password.isBlank()) {
				passwordError.setText("Password cannot be empty or white spaces");
			} else {
				Map<String, String> body = new HashMap<>();
				body.put("email", email);
				body.put("password", password);
				Mono<UserSchema> response = webClient.post()
						.uri("/user/login")
						.body(BodyInserters.fromValue(body))
						.retrieve()
						.bodyToMono(UserSchema.class)
						.doOnError(ResponseException.class, exception -> {
							if (exception.hasFieldErrors()) {
								for (Map<String, String> fieldError : exception.getErrors()) {
									logger.error(fieldError.get("message"));
									if (fieldError.get("field").equals("email")) {
										emailError.setText(fieldError.get("message"));
									} else if (fieldError.get("field").equals("password")) {
										passwordError.setText(fieldError.get("message"));
									}
								}
							} else {
								logger.error(exception.getMessage());
								successLabel.setText(exception.getMessage());
							}
						})
						.onErrorComplete();

				response.subscribe(v -> {
					userSchema.setId(v.getId());
					userSchema.setName(v.getName());
					userSchema.setEmail(v.getEmail());
					userSchema.setPhone(v.getPhone());
					userSchema.setLoginStatus(v.getLoginStatus());
					userSchema.setJoinedTime(v.getJoinedTime());
					userSchema.setLastActiveTime(v.getLastActiveTime());

					successLabel.setText("Login Successfully!");
					JOptionPane.showMessageDialog(null,
							"Login successfully!");
//					mainPanel.setPanel("NextPanel");
//					this.cleanAll();
//					this.close();
				});
			}
		} else if (e.getSource() == registerButton) {
			mainPanel.getRegisterPanel().initialize(mainPanel);
			mainPanel.setPanel("RegisterPanel");
			this.cleanAll();
			this.close();
		}
	}
}
