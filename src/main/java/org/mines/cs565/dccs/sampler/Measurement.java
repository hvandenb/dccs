/*
 * Course: CSCI-565-Distributed Computing Systems
 *
 * Student: Henri van den Bulk
 */
package org.mines.cs565.dccs.sampler;

import java.util.BitSet;
import java.util.Date;

import org.springframework.util.ObjectUtils;

import com.google.common.base.Ticker;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author Henri M.B. van den Bulk
 *
 */
@Data
@AllArgsConstructor
public class Measurement <T extends Number>{

	private long timeTick; // Maintain the time in epoch, e.g. time in nanoseconds since epoch
	
	/**
	 * Name of the measurement
	 */
	private final String name;

	private final T value;


	/**
	 * Create a new {@link Measurement} instance for the current time.
	 * @param name the name of the Measurement
	 * @param value the value of the Measurement
	 */
	public Measurement(String name, T value) {
		this(Ticker.systemTicker().read(), name, value);
	}
	
	/**
	 * Returns a Date that represents the internal representation of the timeTicker
	 * @return
	 */
	public Date getTimeStamp()
	{
		return new Date(timeTick);
	}
	
	@Override
	public String toString() {
		return "Measurement [name=" + this.name + ", value=" + this.value + ", timeTick="
				+ this.timeTick + "]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ObjectUtils.nullSafeHashCode(this.name);
		result = prime * result + ObjectUtils.nullSafeHashCode(this.timeTick);
		result = prime * result + ObjectUtils.nullSafeHashCode(this.value);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (obj instanceof Measurement) {
			Measurement<?> other = (Measurement<?>) obj;
			boolean rtn = true;
			rtn &= ObjectUtils.nullSafeEquals(this.name, other.name);
			rtn &= ObjectUtils.nullSafeEquals(this.timeTick, other.timeTick);
			rtn &= ObjectUtils.nullSafeEquals(this.value, other.value);
			return rtn;
		}
		return super.equals(obj);
	}
	

	/**
	 * Create a new {@link Measurement} with a different value.
	 * @param <S> the Measurement value type
	 * @param value the value of the new Measurement
	 * @return a new {@link Measurement} instance
	 */
	public <S extends Number> Measurement<S> set(S value) {
		return new Measurement<S>(this.getName(), value);
	}
	
}
