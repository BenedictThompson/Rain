package net.azurewebsites.thehen101.raiblockswallet.rain.transaction;

public abstract class Transaction {
	private final Type type;
	private final String work;
	
	public Transaction(Type type, String work) {
		this.type = type;
		this.work = work;
	}
	
	public final Type getType() {
		return this.type;
	}
	
	public final String getWork() {
		return this.work;
	}
	
	abstract String calculateSignature();
	
	public abstract String getAsJSON();
	
	public enum Type {
		OPEN, SEND, RECEIVE, CHANGE
	}
}
