package com.planetmayo.debrief.satc.model.contributions;

import java.util.Date;
import java.util.Iterator;

import com.planetmayo.debrief.satc.model.states.BaseRange.IncompatibleStateException;
import com.planetmayo.debrief.satc.model.states.BoundedState;
import com.planetmayo.debrief.satc.model.states.CourseRange;
import com.planetmayo.debrief.satc.model.states.ProblemSpace;

public class CourseForecastContribution extends BaseContribution
{
	private static final long serialVersionUID = 1L;

	public static final String MIN_COURSE = "minCourse";

	public static final String MAX_COURSE = "maxCourse";

	protected int _minCourse;

	protected int _maxCourse = 360;

	protected int _estimate;

	@Override
	public void actUpon(ProblemSpace space) throws IncompatibleStateException
	{
		// create a bounded state representing our values
		final CourseRange myR = new CourseRange(_minCourse, _maxCourse);

		// remember if we've found items at our start/end times
		boolean needToInjectStart = true;
		boolean needToInjectFinish = true;

		// loop through the states
		final Iterator<BoundedState> sIter = space.states().iterator();
		while (sIter.hasNext())
		{
			// get the next state
			final BoundedState state = sIter.next();

			boolean constrainIt = false;

			// is this one of our end-terms?
			final Date thisT = state.getTime();

			// do we have a start time?
			if (this.getStartDate() != null)
			{
				if (thisT.equals(this.getStartDate()))
				{
					// cool, store it
					needToInjectStart = false;
				}
			}

			// do we have an end time?
			if (this.getFinishDate() != null)
			{
				if (thisT.equals(this.getFinishDate()))
				{
					needToInjectFinish = false;
				}
			}

			// ok, special in-range processing
			constrainIt = checkInDatePeriod(thisT);

			if (constrainIt)
				state.constrainTo(myR);

		}

		// ok, did we find our end terms?
		if (needToInjectStart)
		{
			final BoundedState startState = new BoundedState(this.getStartDate());
			startState.constrainTo(myR);
			space.add(startState);
		}

		// ok, did we find our end terms?
		if (needToInjectFinish)
		{
			final BoundedState endState = new BoundedState(this.getFinishDate());
			endState.constrainTo(myR);
			space.add(endState);
		}

	}

	@Override
	public ContributionDataType getDataType()
	{
		return ContributionDataType.FORECAST;
	}

	public int getEstimate()
	{
		return _estimate;
	}

	@Override
	public String getEstimateStr()
	{
		return "" + _estimate;
	}

	@Override
	public String getHardConstraints()
	{
		return "" + _minCourse + (_minCourse == _maxCourse ? "" : " - " + _maxCourse);
	}

	public int getMaxCourse()
	{
		return _maxCourse;
	}

	public int getMinCourse()
	{
		return _minCourse;
	}

	public void setEstimate(int estimate)
	{
		int oldEstimate = _estimate;
		this._estimate = estimate;
		firePropertyChange(ESTIMATE, oldEstimate, estimate);
	}

	public void setMaxCourse(int maxCourse)
	{
		int oldMaxCourse = _maxCourse;
		String oldConstraints = getHardConstraints();
		this._maxCourse = maxCourse;
		firePropertyChange(MAX_COURSE, oldMaxCourse, maxCourse);
		firePropertyChange(HARD_CONSTRAINTS, oldConstraints, getHardConstraints());
	}

	public void setMinCourse(int minCourse)
	{
		int oldMinCourse = _minCourse;
		String oldConstraints = getHardConstraints();
		this._minCourse = minCourse;
		firePropertyChange(MIN_COURSE, oldMinCourse, minCourse);
		firePropertyChange(HARD_CONSTRAINTS, oldConstraints, getHardConstraints());
	}

}
