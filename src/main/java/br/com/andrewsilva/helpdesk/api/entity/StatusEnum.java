package br.com.andrewsilva.helpdesk.api.entity;

public enum StatusEnum {

	New, Assigned, Resolved, Approved, Disapproved, Close;

	public static StatusEnum getStatus(String status) {
		switch (status) {
		case "New":return New;
		case "Assigned": return Assigned;
		case "Resolved": return Resolved;
		case "Approved": return Approved;
		case "Disapproved": return Disapproved;
		case "Close": return Close;
		default: return New;
		}
	}
}
