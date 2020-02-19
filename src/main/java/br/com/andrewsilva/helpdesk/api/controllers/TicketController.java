package br.com.andrewsilva.helpdesk.api.controllers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mongodb.DuplicateKeyException;

import br.com.andrewsilva.helpdesk.api.entity.ChangeStatus;
import br.com.andrewsilva.helpdesk.api.entity.StatusEnum;
import br.com.andrewsilva.helpdesk.api.entity.Ticket;
import br.com.andrewsilva.helpdesk.api.entity.User;
import br.com.andrewsilva.helpdesk.api.responses.Response;
import br.com.andrewsilva.helpdesk.api.security.jwt.JwtTokenUtil;
import br.com.andrewsilva.helpdesk.api.service.TicketService;
import br.com.andrewsilva.helpdesk.api.service.UserService;

@RestController
@RequestMapping("/api/ticket")
@CrossOrigin
public class TicketController {
	@Autowired
	private TicketService ticketService;

	@Autowired
	private JwtTokenUtil jwtTokenUtil;

	@Autowired
	private UserService userService;

	@PostMapping
	@PreAuthorize("hasAnyRole('CUSTOMER')")
	public ResponseEntity<Response<Ticket>> create(HttpServletRequest request, @RequestBody Ticket ticket,
			BindingResult result) {

		Response<Ticket> response = new Response<Ticket>();
		try {
			validateCreateTicket(ticket, result);
			if (result.hasErrors()) {
				result.getAllErrors().forEach(error -> response.getErrors().add(error.getDefaultMessage()));
				return ResponseEntity.badRequest().body(response);
			}
			ticket.setStatus(StatusEnum.getStatus("New"));
			ticket.setUser(userFromRequest(request));
			ticket.setDate(new Date());
			ticket.setNumber(generateNumber());
			Ticket ticketPersitead = ticketService.createOrUpdate(ticket);
			response.setData(ticketPersitead);
		} catch (Exception e) {
			response.getErrors().add(e.getMessage());
			return ResponseEntity.badRequest().body(response);
		}
		return ResponseEntity.ok(response);
	}

	private void validateCreateTicket(Ticket ticket, BindingResult result) {
		if (ticket.getTitle() == null) {
			result.addError(new ObjectError("Ticket", "Title not informed."));
		}
	}

	private User userFromRequest(HttpServletRequest httpServletRequest) {
		String token = httpServletRequest.getHeader("Authorization");
		String email = jwtTokenUtil.getUsernameFromToken(token);
		return userService.findByEmail(email);
	}

	private Integer generateNumber() {
		Random random = new Random();
		return random.nextInt(9999);
	}

	@PutMapping
	@PreAuthorize("hasAnyRole('TECNICIAN')")
	public ResponseEntity<Response<Ticket>> update(HttpServletRequest request, @RequestBody Ticket ticket,
			BindingResult result) {

		Response<Ticket> response = new Response<Ticket>();
		try {
			validateUpdateTicket(ticket, result);
			if (result.hasErrors()) {
				result.getAllErrors().forEach(error -> response.getErrors().add(error.getDefaultMessage()));
				return ResponseEntity.badRequest().body(response);
			}
			Ticket currentTicket = ticketService.findById(ticket.getId()).get();
			currentTicket.setStatus(ticket.getStatus());
			currentTicket.setUser(ticket.getUser());
			currentTicket.setDate(ticket.getDate());
			currentTicket.setNumber(ticket.getNumber());
			if (ticket.getAssignedUser() != null) {
				currentTicket.setAssignedUser(ticket.getAssignedUser());
			}
			Ticket ticketPersisted = ticketService.createOrUpdate(currentTicket);
			response.setData(ticketPersisted);
		} catch (Exception e) {
			response.getErrors().add(e.getMessage());
			return ResponseEntity.badRequest().body(response);
		}
		return ResponseEntity.ok(response);
	}

	private void validateUpdateTicket(Ticket ticket, BindingResult result) {
		if (ticket.getId() == null) {
			result.addError(new ObjectError("Ticket", "Id not informed."));
		}
		if (ticket.getTitle() == null) {
			result.addError(new ObjectError("Ticket", "Title not informed."));
		}
	}

	@GetMapping(value = "{id}")
	@PreAuthorize("hasAnyRole('CUSTOMER','TECNICIAN')")
	public ResponseEntity<Response<Ticket>> findById(@PathVariable("id") String id) {

		Response<Ticket> response = new Response<Ticket>();
		Optional<Ticket> ticket = ticketService.findById(id);
		if (!ticket.isPresent()) {
			response.getErrors().add("Register not foung by id: " + id);
			return ResponseEntity.badRequest().body(response);
		}

		List<ChangeStatus> listChangeStatus = new ArrayList<ChangeStatus>();
		Iterable<ChangeStatus> changeCurrent = ticketService.listChangeStatus(ticket.get().getId());
		for (ChangeStatus changeStatus : changeCurrent) {
			changeStatus.setTicket(null);
			listChangeStatus.add(changeStatus);
		}
		ticket.get().setChanges(listChangeStatus);
		response.setData(ticket.get());
		return ResponseEntity.ok(response);
	}

	@DeleteMapping(value = "{id}")
	@PreAuthorize("hasAnyRole('CUSTOMER')")
	public ResponseEntity<Response<String>> delete(@PathVariable("id") String id) {

		Response<String> response = new Response<String>();
		Optional<Ticket> ticket = ticketService.findById(id);
		if (ticket == null) {
			response.getErrors().add("Register not foung by id: " + id);
			return ResponseEntity.badRequest().body(response);
		}

		ticketService.delete(id);
		return ResponseEntity.ok(new Response<String>());
	}

//	@GetMapping(value = "{id}")
//	@PreAuthorize("hasAnyRole('ADMIN')")
//	public ResponseEntity<Response<User>> findById(@PathVariable("id") String id) {
//
//		Response<User> response = new Response<User>();
//		Optional<User> user = userService.findById(id);
//		if (user == null) {
//			response.getErrors().add("Register not foung by id: " + id);
//			return ResponseEntity.badRequest().body(response);
//		}
//
//		response.setData(user.get());
//		return ResponseEntity.ok(response);
//	}
//
//	@DeleteMapping(value = "{id}")
//	@PreAuthorize("hasAnyRole('ADMIN')")
//	public ResponseEntity<Response<String>> delete(@PathVariable("id") String id) {
//
//		Response<String> response = new Response<String>();
//		Optional<User> user = userService.findById(id);
//		if (user == null) {
//			response.getErrors().add("Register not foung by id: " + id);
//			return ResponseEntity.badRequest().body(response);
//
//		}
//
//		userService.delete(id);
//		return ResponseEntity.ok(new Response<String>());
//	}
//
//	@GetMapping(value = "{page}/{count}")
//	@PreAuthorize("hasAnyRole('ADMIN')")
//	public ResponseEntity<Response<Page<User>>> findAll(@PathVariable("page") int page,
//			@PathVariable("count") int count) {
//		Response<Page<User>> response = new Response<Page<User>>();
//		Page<User> users = userService.findAll(page, count);
//		response.setData(users);
//		return ResponseEntity.ok(response);
//	}
}
