package br.com.andrewsilva.helpdesk;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.andrewsilva.helpdesk.api.entity.ProfileEnum;
import br.com.andrewsilva.helpdesk.api.entity.User;
import br.com.andrewsilva.helpdesk.api.repository.UserRepository;

@SpringBootApplication
public class HelpdeskApplication {

	public static void main(String[] args) {
		SpringApplication.run(HelpdeskApplication.class, args);
	}

	@Bean
	CommandLineRunner init(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			initUser(userRepository, passwordEncoder);
		};
	}

	private void initUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		User admin = new User();
		admin.setEmail("admin@helpdesk.com");
		admin.setPassword(passwordEncoder.encode("123456"));
		admin.setProfile(ProfileEnum.ROLE_ADMIN);

		User find = userRepository.findByEmail("admin@helpdesk.com");
		if (find == null) {
			userRepository.save(admin);
		}
		
		User customer = new User();
		customer.setEmail("customer@helpdesk.com");
		customer.setPassword(passwordEncoder.encode("123456"));
		customer.setProfile(ProfileEnum.ROLE_CUSTOMER);

		find = null;
		
		find = userRepository.findByEmail("customer@helpdesk.com");
		if (find == null) {
			userRepository.save(customer);
		}

		User tecnician = new User();
		tecnician.setEmail("tecnician@helpdesk.com");
		tecnician.setPassword(passwordEncoder.encode("123456"));
		tecnician.setProfile(ProfileEnum.ROLE_TECNICIAN);

		find = null;
		
		find = userRepository.findByEmail("tecnician@helpdesk.com");
		if (find == null) {
			userRepository.save(tecnician);
		}
	}

}
