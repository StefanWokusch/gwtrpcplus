package de.joe.core.rpc.shared;

public interface ReturnHandler<Type> {
	void answer(Type obj);
}
