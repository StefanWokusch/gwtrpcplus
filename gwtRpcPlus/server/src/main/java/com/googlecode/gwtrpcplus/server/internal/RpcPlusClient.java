package com.googlecode.gwtrpcplus.server.internal;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.google.gwt.event.shared.HandlerRegistration;
import com.googlecode.gwtrpcplus.server.internal.util.Logger;

/**
 * A connected Client to the Server
 */
public class RpcPlusClient {
	/**
	 * Milliseconds when the client isn't sending anything, the Client will be marked as obsolete
	 */
	private static final long OBSOLETE_TIMEOUT = TimeUnit.MINUTES.toMillis(1);

	/**
	 * WARN: have to equals TimeoutTimer.TIMEOUT
	 */
	static final long KEEPALIVE_TIMEOUT = TimeUnit.SECONDS.toMillis(10);

	private Logger logger = new Logger(RpcPlusClient.class);

	private final Set<RpcPlusClientHandler> handlers = Collections.newSetFromMap(new ConcurrentHashMap<RpcPlusClientHandler, Boolean>());

	private final BlockingQueue<String> responses = new LinkedBlockingQueue<String>();

	private boolean disconnected = false;

	private long lastCall = System.currentTimeMillis();
	private long lastAnswer = System.currentTimeMillis();

	/**
	 * Handler for Answers
	 */
	public interface RpcPlusClientHandler {
		/**
		 * Called when a answer was created for a client
		 * 
		 * @param answer
		 *          the Answer to send
		 * @return true when the answer could be send, false for example on ws-disconnect
		 */
		boolean onAnswer(String answer);
	}

	public HandlerRegistration addHandler(final RpcPlusClientHandler handler) {
		handlers.add(handler);
		return new HandlerRegistration() {
			@Override
			public void removeHandler() {
				handlers.remove(handler);
			}
		};
	}

	/**
	 * @return true when the Client isn't called the last time and can be cleaned
	 */
	public boolean isObsolete() {
		// TODO Remove this check
		// Don't remove when an active websocketconnection is established
		if (!handlers.isEmpty())
			return false;

		return disconnected || (System.currentTimeMillis() - lastCall) > OBSOLETE_TIMEOUT;
	}

	/**
	 * Mark the Client as inUse
	 */
	public void touch() {
		lastCall = System.currentTimeMillis();
	}

	public void addResponse(String response) {
		if (disconnected) {
			logger.trace("Ignoring Response because of client disconnected");
			return;
		}
		for (RpcPlusClientHandler h : handlers) {
			if (h.onAnswer(response)) {
				touch();
				return;
			}
		}

		responses.add(response);
	}

	public String getResponse() {
		touch();
		lastAnswer = System.currentTimeMillis();
		return responses.poll();
	}

	public String getResponseAndWait() {
		try {
			// Timeout will be calculated by the maximum keepalive-time. After that it will subtract
			// something to be sure, the client will recieve it in time.
			long timeout = Math.min(KEEPALIVE_TIMEOUT, KEEPALIVE_TIMEOUT - (System.currentTimeMillis() - lastAnswer)) - 1000;
			logger.trace("waiting {} ms for responses", timeout);
			return responses.poll(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			return null;
		} finally {
			touch();
			lastAnswer = System.currentTimeMillis();
		}
	}

	public void disconnect() {
		if (!disconnected) {
			disconnected = true;
			responses.clear();

			// TODO Inform Serverpush-Methods

			// TODO Inform Handlers
		}
	}

	public void keepAlive() {
		if (!isObsolete() && !handlers.isEmpty() && System.currentTimeMillis() - lastAnswer > KEEPALIVE_TIMEOUT) {
			logger.info("Sending keepalive");
			addResponse(".");
		}
	}

}
