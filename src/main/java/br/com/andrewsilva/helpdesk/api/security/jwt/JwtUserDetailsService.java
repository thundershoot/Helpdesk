package br.com.andrewsilva.helpdesk.api.security.jwt;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import br.com.andrewsilva.helpdesk.api.entity.User;
import br.com.andrewsilva.helpdesk.api.service.UserService;

@Service
public class JwtUserDetailsService implements UserDetailsService {

	@Autowired
	private UserService usuarioService;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User usuario = usuarioService.findByEmail(username);

		if (usuario == null) {
			throw new UsernameNotFoundException("User not found with username: " + username);
		} else {
			return JwtUserFactory.create(usuario);
		}
	}

}
