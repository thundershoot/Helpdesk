package br.com.andrewsilva.helpdesk.api.controllers;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.aggregation.AccumulatorOperators.Sum;
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

import br.com.andrewsilva.helpdesk.api.dto.Summary;
import br.com.andrewsilva.helpdesk.api.entity.ChangeStatus;
import br.com.andrewsilva.helpdesk.api.entity.ProfileEnum;
import br.com.andrewsilva.helpdesk.api.entity.StatusEnum;
import br.com.andrewsilva.helpdesk.api.entity.Ticket;
import br.com.andrewsilva.helpdesk.api.entity.User;
import br.com.andrewsilva.helpdesk.api.responses.Response;
import br.com.andrewsilva.helpdesk.api.security.jwt.JwtTokenUtil;
import br.com.andrewsilva.helpdesk.api.service.TicketService;
import br.com.andrewsilva.helpdesk.api.service.UserService;
import io.jsonwebtoken.ExpiredJwtException;

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
		String jwtToken = null;
		String username = null;
		String requestTokenHeader = httpServletRequest.getHeader("Authorization");
		if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
			jwtToken = requestTokenHeader.substring(7);
			try {
				username = jwtTokenUtil.getUsernameFromToken(jwtToken);
			} catch (IllegalArgumentException e) {
				System.out.println("Unable to get JWT Token");
			} catch (ExpiredJwtException e) {
				System.out.println("JWT Token has expired");
			}
		} else {
			System.out.println("JWT Token does not begin with Bearer String");
		}
		return userService.findByEmail(username);
	}

	private Integer generateNumber() {
		Random random = new Random();
		return random.nextInt(9999);
	}

	@PutMapping
	@PreAuthorize("hasAnyRole('CUSTOMER')")
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
			ticket.setStatus(currentTicket.getStatus());
			ticket.setUser(currentTicket.getUser());
			ticket.setDate(currentTicket.getDate());
			ticket.setNumber(currentTicket.getNumber());
			if (currentTicket.getAssignedUser() != null) {
				ticket.setAssignedUser(currentTicket.getAssignedUser());
			}
			Ticket ticketPersisted = ticketService.createOrUpdate(ticket);
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

	@GetMapping(value = "{page}/{count}")
	@PreAuthorize("hasAnyRole('CUSTOMER','TECNICIAN')")
	public ResponseEntity<Response<Page<Ticket>>> findAll(HttpServletRequest request, @PathVariable("page") int page,
			@PathVariable("count") int count) {
		Response<Page<Ticket>> response = new Response<Page<Ticket>>();
		Page<Ticket> tickets = null;
		User userRequest = userFromRequest(request);
		if (userRequest.getProfile().equals(ProfileEnum.ROLE_TECNICIAN)) {
			tickets = ticketService.listTicket(page, count);
		} else if (userRequest.getProfile().equals(ProfileEnum.ROLE_CUSTOMER)) {
			tickets = ticketService.findByCurrentUser(page, count, userRequest.getId());
		}

		response.setData(tickets);
		return ResponseEntity.ok(response);
	}

	@GetMapping(value = "{page}/{count}/{number}/{title}/{status}/{priority}/{assigned}")
	@PreAuthorize("hasAnyRole('CUSTOMER','TECNICIAN')")
	public ResponseEntity<Response<Page<Ticket>>> findByParams(HttpServletRequest request,
			@PathVariable("page") int page, @PathVariable("count") int count, @PathVariable("number") Integer number,
			@PathVariable("title") String title, @PathVariable("status") String status,
			@PathVariable("priority") String priority, @PathVariable("assigned") boolean assigned) {

		title = title.equals("uninformed") ? "" : title;
		status = status.equals("uninformed") ? "" : status;
		priority = title.equals("uninformed") ? "" : priority;

		Response<Page<Ticket>> response = new Response<Page<Ticket>>();
		Page<Ticket> tickets = null;
		User userRequest = userFromRequest(request);

		if (number > 0) {
			tickets = ticketService.findByNumber(page, count, number);
		} else {
			if (userRequest.getProfile().equals(ProfileEnum.ROLE_TECNICIAN)) {
				if (assigned) {
					tickets = ticketService.findByParameterAndAssignedUser(page, count, title, status, priority,
							userRequest.getId());
				} else {
					tickets = ticketService.findByParameters(page, count, title, status, priority);
				}

			} else if (userRequest.getProfile().equals(ProfileEnum.ROLE_CUSTOMER)) {
				tickets = ticketService.findByParametersCurrentUser(page, count, title, status, priority,
						userRequest.getId());
			}
		}

		response.setData(tickets);
		return ResponseEntity.ok(response);
	}

	@PutMapping(value = "{id}/{status}")
	@PreAuthorize("hasAnyRole('CUSTOMER','TECNICIAN')")
	public ResponseEntity<Response<Ticket>> changeStatus(@PathVariable("id") String id,
			@PathVariable("status") String status, HttpServletRequest request, @RequestBody Ticket ticket,
			BindingResult result) {

		Response<Ticket> response = new Response<Ticket>();
		try {
			validateChangeStatusTicket(id, status, result);
			if (result.hasErrors()) {
				result.getAllErrors().forEach(error -> response.getErrors().add(error.getDefaultMessage()));
				return ResponseEntity.badRequest().body(response);
			}
			Ticket currentTicket = ticketService.findById(ticket.getId()).get();
			currentTicket.setStatus(StatusEnum.getStatus(status));
			if (status.equals("Assigned")) {
				currentTicket.setAssignedUser(userFromRequest(request));
			}

			Ticket ticketPersisted = ticketService.createOrUpdate(currentTicket);
			ChangeStatus changeStatus = new ChangeStatus();
			changeStatus.setUserChange(userFromRequest(request));
			changeStatus.setDateChangeStatus(new Date());
			changeStatus.setStatus(StatusEnum.getStatus(status));
			changeStatus.setTicket(ticketPersisted);
			ticketService.createChangeStatus(changeStatus);
			response.setData(ticketPersisted);
		} catch (Exception e) {
			response.getErrors().add(e.getMessage());
			return ResponseEntity.badRequest().body(response);
		}
		return ResponseEntity.ok(response);
	}

	private void validateChangeStatusTicket(String id, String status, BindingResult result) {
		if (id == null || id.equals("")) {
			result.addError(new ObjectError("Ticket", "Id not informed."));
		}
		if (status == null || status.equals("")) {
			result.addError(new ObjectError("Ticket", "Status not informed."));
		}
	}

	@GetMapping(value = "/summary")
	public ResponseEntity<Response<Summary>> findSummary() {
		Response<Summary> response = new Response<Summary>();

		Summary summary = new Summary();
		int amountNew = 0;
		int amountResolved = 0;
		int amountApproved = 0;
		int amountDisapproved = 0;
		int amountAssigned = 0;
		int amountClosed = 0;

		Iterable<Ticket> tickets = ticketService.findAll();
		if (tickets != null) {
			for (Iterator<Ticket> iterator = tickets.iterator(); iterator.hasNext();) {
				Ticket ticket = (Ticket) iterator.next();
				if (ticket.getStatus().equals(StatusEnum.New)) {
					amountNew++;
				}
				if (ticket.getStatus().equals(StatusEnum.Resolved)) {
					amountResolved++;
				}
				if (ticket.getStatus().equals(StatusEnum.Approved)) {
					amountApproved++;
				}
				if (ticket.getStatus().equals(StatusEnum.Disapproved)) {
					amountDisapproved++;
				}
				if (ticket.getStatus().equals(StatusEnum.Assigned)) {
					amountAssigned++;
				}
				if (ticket.getStatus().equals(StatusEnum.Close)) {
					amountClosed++;
				}
			}
		}
		summary.setAmountNew(amountNew);
		summary.setAmountResolved(amountResolved);
		summary.setAmountApproved(amountApproved);
		summary.setAmountDisapproved(amountDisapproved);
		summary.setAmountAssigned(amountAssigned);
		summary.setAmountClosed(amountClosed);
		response.setData(summary);
		return ResponseEntity.ok(response);

	}

}
