package br.com.andrewsilva.helpdesk.api.security.jwt;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.bionexo.internacionalapi.models.Usuario;
import com.bionexo.internacionalapi.services.UsuarioService;

@Service
public class JwtUserDetailsService implements UserDetailsService {

	@Autowired
	private UsuarioService usuarioService;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Optional<Usuario> usuario = usuarioService.buscarPorLogin(username);

		if (usuario.isPresent()) {
			return JwtUserFactory.create(usuario.get());

		}

		throw new UsernameNotFoundException("User not found with username: " + username);

	}

}
