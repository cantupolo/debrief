package com.planetmayo.debrief.satc.model.states;

import java.util.Date;

import com.planetmayo.debrief.satc.model.states.BaseRange.IncompatibleStateException;

public class BoundedState implements Comparable<BoundedState>
{
	private Date _time;
	private SpeedRange _speed;
	private CourseRange _course;
	private LocationRange _location;

	public BoundedState(Date time)
	{
		_time = time;
	}

	@Override
	public int compareTo(BoundedState o)
	{
		return getTime().compareTo(o.getTime());
	}

	/**
	 * apply all of the supplied state's constraints to ourselves
	 * 
	 * @param newState
	 */
	public void constrainTo(BoundedState newState) throws IncompatibleStateException
	{
		this.constrainTo(newState._speed);
		this.constrainTo(newState._course);
		this.constrainTo(newState._location);
	}

	/**
	 * apply the specified constraint to ourselves
	 * 
	 * @param range
	 */
	public void constrainTo(SpeedRange range) throws IncompatibleStateException
	{
		// do we have any speed constraints?
		if (_speed == null)
		{
			// no, better create some
			_speed = new SpeedRange(range);
		}
		else
		{ // yes, further constrain to this set
			_speed.constrainTo(range);
		}
	}
	/**
	 * apply the specified constraint to ourselves
	 * 
	 * @param range
	 */
	public void constrainTo(CourseRange range) throws IncompatibleStateException
	{
		// do we have any speed constraints?
		if (_course == null)
		{
			// no, better create some
			_course = new CourseRange(range);
		}
		else
		{ // yes, further constrain to this set
			_course.constrainTo(range);
		}
	}
	/**
	 * apply the specified constraint to ourselves
	 * 
	 * @param range
	 */
	public void constrainTo(LocationRange range) throws IncompatibleStateException
	{
		// do we have any speed constraints?
		if (_location == null)
		{
			// no, better create some
			_location = new LocationRange(range);
		}
		else
		{ // yes, further constrain to this set
			_location.constrainTo(range);
		}
	}

	public SpeedRange getSpeed()
	{
		return _speed;
	}

	public Date getTime()
	{
		return _time;
	}

	public void setTime(Date time)
	{
		_time = time;
	}

}
