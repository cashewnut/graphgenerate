package edu.fdu.se.graphgenerate.model;

public class VarNode {

	private Long id;

	private String signal;

	public VarNode(Long id, String signal) {
		this.id = id;
		this.signal = signal;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSignal() {
		return signal;
	}

	public void setSignal(String signal) {
		this.signal = signal;
	}

}
