package io.opencaesar.rosetta.sirius.validation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Return type for all {@link Constraint} methods.
 */
public class Result {
	
	public static final Result SUCCESS = new Result(true);
	
	/**
	 * Returns a "Success" result to indicate the constraint passed.
	 */
	public static Result success() {
		return SUCCESS;
	}
	
	/**
	 * Returns a failure result to indicate the constraint failed. The
	 * data value parameters are used to construct the error message for
	 * the constraint using a message format specified in the {@link Constraint}
	 * annotation.
	 */
	public static Result failure(Object... data) {
		return new Result(false, data);
	}
	
	private boolean ok;
	private List<Object> data;
	
	private Result(boolean ok, Object... data) {
		this.ok = ok;
		this.data = Collections.unmodifiableList(Arrays.asList(data));
	}
	
	public boolean isSuccess() {
		return ok;
	}
	
	public List<Object> getData() {
		return data;
	}
	
	public String toString() {
		return (ok ? "Success":"Failure") + " " + data.stream().map(Object::toString).collect(Collectors.joining(", "));
	}
}