package br.com.andrewsilva.helpdesk.api.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import br.com.andrewsilva.helpdesk.api.entity.ChangeStatus;

public interface ChangeStatusRepositry extends MongoRepository<ChangeStatus, String> {

	Iterable<ChangeStatus> findByTicketIdOrderByDateChangeStatusDesc(String ticketId);

}
