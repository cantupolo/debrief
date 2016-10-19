/*
 *    Debrief - the Open Source Maritime Analysis Application
 *    http://debrief.info
 *
 *    (C) 2000-2014, PlanetMayo Ltd
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the Eclipse Public License v1.0
 *    (http://www.eclipse.org/legal/epl-v10.html)
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 */
package org.mwc.debrief.satc_interface.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.mwc.cmap.core.CorePlugin;
import org.mwc.cmap.core.operations.CMAPOperation;
import org.mwc.cmap.core.property_support.RightClickSupport.RightClickContextItemGenerator;
import org.mwc.debrief.core.DebriefPlugin;
import org.mwc.debrief.satc_interface.SATC_Interface_Activator;
import org.mwc.debrief.satc_interface.data.SATC_Solution;
import org.mwc.debrief.satc_interface.data.wrappers.ContributionWrapper;
import org.mwc.debrief.satc_interface.utilities.conversions;
import org.mwc.debrief.satc_interface.wizards.NewStraightLegWizard;

import Debrief.Wrappers.FixWrapper;
import Debrief.Wrappers.SensorContactWrapper;
import Debrief.Wrappers.SensorWrapper;
import Debrief.Wrappers.TrackWrapper;
import MWC.GUI.Editable;
import MWC.GUI.Layer;
import MWC.GUI.Layers;
import MWC.GenericData.HiResDate;
import MWC.GenericData.TimePeriod;
import MWC.GenericData.TimePeriod.BaseTimePeriod;
import MWC.GenericData.Watchable;
import MWC.GenericData.WorldDistance;
import MWC.GenericData.WorldLocation;
import MWC.GenericData.WorldSpeed;
import MWC.Utilities.TextFormatting.FormatRNDateTime;

import com.planetmayo.debrief.satc.model.GeoPoint;
import com.planetmayo.debrief.satc.model.contributions.BaseContribution;
import com.planetmayo.debrief.satc.model.contributions.BearingMeasurementContribution;
import com.planetmayo.debrief.satc.model.contributions.BearingMeasurementContribution.BMeasurement;
import com.planetmayo.debrief.satc.model.contributions.CompositeStraightLegForecastContribution;
import com.planetmayo.debrief.satc.model.contributions.CourseAnalysisContribution;
import com.planetmayo.debrief.satc.model.contributions.CourseForecastContribution;
import com.planetmayo.debrief.satc.model.contributions.FrequencyMeasurement;
import com.planetmayo.debrief.satc.model.contributions.FrequencyMeasurementContribution;
import com.planetmayo.debrief.satc.model.contributions.FrequencyMeasurementContribution.FMeasurement;
import com.planetmayo.debrief.satc.model.contributions.LocationAnalysisContribution;
import com.planetmayo.debrief.satc.model.contributions.Range1959ForecastContribution;
import com.planetmayo.debrief.satc.model.contributions.RangeForecastContribution;
import com.planetmayo.debrief.satc.model.contributions.RangeForecastContribution.ROrigin;
import com.planetmayo.debrief.satc.model.contributions.SpeedAnalysisContribution;
import com.planetmayo.debrief.satc.model.contributions.SpeedForecastContribution;
import com.planetmayo.debrief.satc.model.contributions.StraightLegForecastContribution;
import com.planetmayo.debrief.satc.model.generator.IContributions;
import com.planetmayo.debrief.satc.model.generator.ISolver;
import com.planetmayo.debrief.satc.model.manager.ISolversManager;
import com.planetmayo.debrief.satc_rcp.SATC_Activator;

public class CreateSolutionFromSensorData implements
		RightClickContextItemGenerator
{

	@Override
	public void generate(IMenuManager parent, final Layers theLayers,
			Layer[] parentLayers, Editable[] subjects)
	{
		ArrayList<SensorContactWrapper> validCuts = null;

		parent.add(new Separator());
		IMenuManager thisMenu = new MenuManager("SemiAuto TMA");
		parent.add(thisMenu);

		BaseTimePeriod runningPeriod = null;

		int sensorCounter = 0;
		
		for (int i = 0; i < subjects.length; i++)
		{
			Editable thisItem = subjects[i];
			if (thisItem instanceof SensorContactWrapper)
			{
				SensorContactWrapper scw = (SensorContactWrapper) thisItem;

				if (scw.getVisible())
				{
					if (validCuts == null)
						validCuts = new ArrayList<SensorContactWrapper>();

					// SPECIAL PROCESSING: if the user has accidentally selected both the
					// sensor and a block of cuts, they will get added twice.
					// so - double-check we haven't already loaded this cut
					if (!validCuts.contains(scw))
						validCuts.add(scw);
				}
			}
			else if (thisItem instanceof SensorWrapper)
			{
				SensorWrapper sw = (SensorWrapper) thisItem;
				
				sensorCounter ++;

				// only handle the first five sensors
				if(sensorCounter <= 5)
        {
          Enumeration<Editable> cuts = sw.elements();
          while (cuts.hasMoreElements())
          {
            SensorContactWrapper thisCut =
                (SensorContactWrapper) cuts.nextElement();
            if (thisCut.getVisible())
            {
              if (validCuts == null)
                validCuts = new ArrayList<SensorContactWrapper>();

              // SPECIAL PROCESSING: see above for duplicate cuts
              if (!validCuts.contains(thisCut))
                validCuts.add(thisCut);
            }
          }
				}
				else
				{
          CorePlugin.logError(Status.WARNING,
              "NOTE: we're only going to process the first five sensors", null);
				}
				  
			}
			else if ((thisItem instanceof ContributionWrapper)
					|| (thisItem instanceof SATC_Solution))
			{
				BaseTimePeriod period = null;

				if (thisItem instanceof ContributionWrapper)
				{
					ContributionWrapper cw = (ContributionWrapper) thisItem;
					BaseContribution theCont = cw.getContribution();
					period = new TimePeriod.BaseTimePeriod(new HiResDate(
							theCont.getStartDate()), new HiResDate(theCont.getFinishDate()));

				}
				else
				{
					SATC_Solution solution = (SATC_Solution) thisItem;

					if (solution.getSolver().getProblemSpace().getStartDate() != null)
						period = new TimePeriod.BaseTimePeriod(solution.getStartDTG(),
								solution.getEndDTG());
				}

				// have either of these objects produced a period?
				if (period != null)
				{
					// have we got a period already?
					if (runningPeriod == null)
					{
						// nope, let's use this one then
						runningPeriod = period;
					}
					else
					{
						// yes - well in that case -extend it with these values
						runningPeriod.extend(period.getStartDTG());
						runningPeriod.extend(period.getEndDTG());
					}
				}
			}
		}

		if (runningPeriod != null)
		{
			parent.add(new Separator());

			SATC_Solution solution = (SATC_Solution) parentLayers[0];
			String actionTitle = "Add new contribution";

			parent.add(new DoIt(
					"Add Speed Forecast for period covered by [selected legs]",
					new SpeedForecastContributionFromCuts(solution, actionTitle,
							theLayers, runningPeriod), "icons/16/speed.png"));
			parent.add(new DoIt(
					"Add Course Forecast for period covered by [selected legs]",
					new CourseForecastContributionFromCuts(solution, actionTitle,
							theLayers, runningPeriod), "icons/16/direction.png"));
			parent.add(new DoIt(
					"Add Straight Leg for period covered by [selected legs]",
					new CompositeStraightLegForecastContributionFromCuts(solution,
							actionTitle, theLayers, runningPeriod), "icons/16/leg.png"));

		}

		// ok, is it worth going for?
		if (validCuts != null)
		{
			final String title;
			if (validCuts.size() > 1)
				title = "sensor cuts";
			else
				title = "sensor cut";

			// right,stick in a separator
			thisMenu.add(new Separator());

			// see if there's an existing solution in there.
			SATC_Solution[] existingSolutions = findExistingSolutionsIn(theLayers);

			if ((existingSolutions != null) && (existingSolutions.length > 0))
			{
				for (int i = 0; i < existingSolutions.length; i++)
				{
					final SATC_Solution layer = existingSolutions[i];

					// create a top level menu item
					MenuManager thisD = new MenuManager("Add to " + layer.getName());
					thisMenu.add(thisD);

					// add the child items
					addItemsTo(layer, thisD, validCuts, title, null, "New ");
				}
			}

			DoIt wizardItem = new DoIt("Create new scenario from these bearings",
					new BearingMeasurementContributionFromCuts(null, title, theLayers,
							validCuts)
					{

						@Override
						public String getContributionName()
						{
							return "Bearing data";
						}
					}, "icons/16/scenario.png");
			thisMenu.add(wizardItem);
		}
	}

	protected void addItemsTo(final SATC_Solution solution,
			final MenuManager parent,
			final ArrayList<SensorContactWrapper> validItems, final String title,
			Layers layers, String verb1)
	{

		String actionTitle = "Add new contribution";

		// NOTE: we've removed the composite wizard, since it clashed with our new
		// strategy
		// where we have NULL has default value for course/speed forecast attributes
		//
		// DoIt wizardItem = new DoIt(
		// "Open Straight Leg Wizard for period covered by [" + title + "]",
		// new StraightLegWizardFromCuts(solution, actionTitle, layers, validItems),
		// "icons/wizard.png");
		// parent.add(wizardItem);
		// parent.add(new Separator());

		SensorContactWrapper first = validItems.get(0);

		// hmm, is there bearing data?
		if (first.getHasBearing())
		{
			parent.add(new DoIt("Contribute selected bearings to the scenario",
					new BearingMeasurementContributionFromCuts(solution, actionTitle,
							layers, validItems), "icons/16/bearing.png"));
		}
		if (first.getHasFrequency())
		{
			parent.add(new DoIt(
					"Contribute selected frequency measurements to the scenario",
					new FrequencyMeasurementContributionFromCuts(solution, actionTitle,
							layers, validItems), "icons/16/frequency.png"));
		}

		parent.add(new Separator());
		parent.add(new DoIt(verb1 + "Straight Leg for period covered by [" + title
				+ "]", new CompositeStraightLegForecastContributionFromCuts(solution,
				actionTitle, layers, validItems), "icons/16/leg.png"));
		parent.add(new Separator());
		parent.add(new DoIt(verb1 + "Speed Forecast for period covered by ["
				+ title + "]", new SpeedForecastContributionFromCuts(solution,
				actionTitle, layers, validItems), "icons/16/speed.png"));
		parent.add(new DoIt(verb1 + "Range Forecast for period covered by ["
				+ title + "]", new RangeForecastContributionFromCuts(solution,
				actionTitle, layers, validItems), "icons/16/range.png"));
		parent.add(new DoIt(verb1 + "Course Forecast for period covered by ["
				+ title + "]", new CourseForecastContributionFromCuts(solution,
				actionTitle, layers, validItems), "icons/16/direction.png"));

		// does this data have frequency?
		if (validItems.size() > 0)
		{
			SensorContactWrapper firstItem = validItems.get(0);
			if (firstItem.getHasFrequency())
			{
				// ok, it has freq. Add the revevant option
				parent.add(new Separator());
				parent.add(new DoIt(verb1
						+ "1959 Range Forecast for period covered by [" + title + "]",
						new Range1959ForecastContributionFromCuts(solution, actionTitle,
								layers, validItems), "icons/16/frequency.png"));
			}
		}

	}

	protected static class DoIt extends Action
	{
		private final IUndoableOperation _myOperation;

		DoIt(String title, IUndoableOperation operation, String path)
		{
			super(title);
			_myOperation = operation;
			this.setImageDescriptor(SATC_Interface_Activator.getImageDescriptor(path));
		}

		@Override
		public void run()
		{
			CorePlugin.run(_myOperation);
		}

	}

	/**
	 * put the operation firer onto the undo history. We've refactored this into a
	 * separate method so testing classes don't have to simulate the CorePlugin
	 * 
	 * @param operation
	 */
	protected void runIt(IUndoableOperation operation)
	{
		CorePlugin.run(operation);
	}

	private SATC_Solution[] findExistingSolutionsIn(Layers theLayers)
	{
		ArrayList<SATC_Solution> res = null;

		Enumeration<Editable> iter = theLayers.elements();
		while (iter.hasMoreElements())
		{
			Editable thisL = iter.nextElement();
			if (thisL instanceof SATC_Solution)
			{
				if (res == null)
					res = new ArrayList<SATC_Solution>();

				res.add((SATC_Solution) thisL);
			}
		}

		SATC_Solution[] list = null;
		if (res != null)
			list = res.toArray(new SATC_Solution[] {});

		return list;
	}

	private class BearingMeasurementContributionFromCuts extends
			CoreSolutionFromCuts
	{
		private ArrayList<SensorContactWrapper> _validCuts;

		public BearingMeasurementContributionFromCuts(
				SATC_Solution existingSolution, String title, Layers theLayers,
				ArrayList<SensorContactWrapper> validCuts)
		{
			super(existingSolution, title, theLayers, new TimePeriod.BaseTimePeriod(
					new HiResDate(validCuts.get(0).getDTG().getDate()), new HiResDate(
							validCuts.get(validCuts.size() - 1).getDTG())));
			_validCuts = validCuts;
		}

		protected BearingMeasurementContribution createContribution(String contName)
		{
			// ok, now collate the contriubtion
			final BearingMeasurementContribution bmc = new BearingMeasurementContribution();
			bmc.setName(contName);
			bmc.setBearingError(Math.toRadians(3.0));
			bmc.setAutoDetect(false);

			// add the bearing data
			Iterator<SensorContactWrapper> iter = _validCuts.iterator();
			while (iter.hasNext())
			{
				final SensorContactWrapper scw = (SensorContactWrapper) iter.next();
				WorldLocation theOrigin = scw.getOrigin();
				GeoPoint loc;

				if (theOrigin == null)
					theOrigin = scw.getCalculatedOrigin(scw.getSensor().getHost());

				if (theOrigin != null)
				{

					loc = conversions.toPoint(theOrigin);

					double brg = Math.toRadians(scw.getBearing());
					Date date = scw.getDTG().getDate();
					Double theRange = null;
					if (scw.getRange() != null)
						theRange = scw.getRange().getValueIn(WorldDistance.METRES);

					final BMeasurement thisM = new BMeasurement(loc, brg, date, theRange);

					// give it the respective color
					thisM.setColor(scw.getColor());

					// ok, store it.
					bmc.addMeasurement(thisM);
				}
			}

			// we also want to try to store the ownship state data (course/speed) - to
			// help spot target zigs
			SensorContactWrapper first = _validCuts.get(0);
			TrackWrapper host = first.getSensor().getHost();
			Collection<Editable> hostFixes = host
					.getItemsBetween(new HiResDate(bmc.getStartDate()),
							new HiResDate(bmc.getFinishDate()));
			for (Iterator<Editable> iterator = hostFixes.iterator(); iterator
					.hasNext();)
			{
				FixWrapper editable = (FixWrapper) iterator.next();
				long time = editable.getDateTimeGroup().getDate().getTime();
				double courseDegs = editable.getCourseDegs();
				double speedKts = editable.getSpeed();
				bmc.addState(new BearingMeasurementContribution.HostState(time,
						courseDegs, speedKts, editable.getLocation().getLat(), editable
								.getLocation().getLong()));
			}

			return bmc;
		}

	}

	private class SpeedForecastContributionFromCuts extends
			ForecastContributionFromCuts
	{
		public SpeedForecastContributionFromCuts(SATC_Solution existingSolution,
				String title, Layers theLayers,
				ArrayList<SensorContactWrapper> validCuts)
		{
			super(existingSolution, title, theLayers, validCuts);
		}

		public SpeedForecastContributionFromCuts(SATC_Solution existingSolution,
				String title, Layers theLayers, TimePeriod period)
		{
			super(existingSolution, title, theLayers, period);
		}

		@Override
		protected BaseContribution getContribution()
		{
			return new SpeedForecastContribution();
		}

	}

	/**
	 * conveneince function. we've pulled it out of the class so that we can use
	 * it in testing
	 * 
	 * @param validCuts
	 * @return
	 */
	public static BaseContribution generate1959(
			ArrayList<SensorContactWrapper> validCuts)
	{

		// ok, now collate the contriubtion
		final Range1959ForecastContribution bmc = new Range1959ForecastContribution();

		// add the bearing data
		Iterator<SensorContactWrapper> iter = validCuts.iterator();
		while (iter.hasNext())
		{
			final SensorContactWrapper scw = (SensorContactWrapper) iter.next();

			Date date = scw.getDTG().getDate();
			double freq = scw.getFrequency();

			final FrequencyMeasurement thisM = new FrequencyMeasurement(date,
					conversions.toPoint(scw.getLocation()), freq);

			// give it the respective color
			thisM.setColor(scw.getColor());

			// ok, store it.
			bmc.addMeasurement(thisM);
		}

		return bmc;
	}

	private class Range1959ForecastContributionFromCuts extends
			ForecastContributionFromCuts
	{
		private final ArrayList<SensorContactWrapper> _validCuts;

		public Range1959ForecastContributionFromCuts(
				SATC_Solution existingSolution, String title, Layers theLayers,
				ArrayList<SensorContactWrapper> validCuts)
		{
			super(existingSolution, title, theLayers, validCuts);

			_validCuts = validCuts;
		}

		@Override
		protected BaseContribution getContribution()
		{
			BaseContribution res = generate1959(_validCuts);
			return res;

		}

	}

	private class CourseForecastContributionFromCuts extends
			ForecastContributionFromCuts
	{
		public CourseForecastContributionFromCuts(SATC_Solution existingSolution,
				String title, Layers theLayers,
				ArrayList<SensorContactWrapper> validCuts)
		{
			super(existingSolution, title, theLayers, validCuts);
		}

		public CourseForecastContributionFromCuts(SATC_Solution existingSolution,
				String title, Layers theLayers, TimePeriod period)
		{
			super(existingSolution, title, theLayers, period);
		}

		@Override
		protected BaseContribution getContribution()
		{
			return new CourseForecastContribution();
		}

	}

	private class RangeForecastContributionFromCuts extends
			ForecastContributionFromCuts
	{
		private ArrayList<SensorContactWrapper> _validCuts;

		public RangeForecastContributionFromCuts(SATC_Solution existingSolution,
				String title, Layers theLayers,
				ArrayList<SensorContactWrapper> validCuts)
		{
			super(existingSolution, title, theLayers, validCuts);
			_validCuts = validCuts;
		}

		@Override
		protected BaseContribution getContribution()
		{
			RangeForecastContribution rfc = new RangeForecastContribution();

			// now set the range origins

			// add the bearing data
			Iterator<SensorContactWrapper> iter = _validCuts.iterator();
			while (iter.hasNext())
			{
				final SensorContactWrapper scw = (SensorContactWrapper) iter.next();
				WorldLocation theOrigin = scw.getOrigin();
				GeoPoint loc;

				if (theOrigin == null)
					theOrigin = scw.getCalculatedOrigin(scw.getSensor().getHost());

				loc = conversions.toPoint(theOrigin);

				RangeForecastContribution.ROrigin newR = new ROrigin(loc, scw.getDTG()
						.getDate());
				rfc.addThis(newR);

			}

			return rfc;
		}

	}

	@SuppressWarnings("unused")
	private class StraightLegWizardFromCuts extends ForecastContributionFromCuts
	{
		private NewStraightLegWizard _wizard;

		public StraightLegWizardFromCuts(SATC_Solution existingSolution,
				String title, Layers theLayers,
				ArrayList<SensorContactWrapper> validCuts)
		{
			// sort out the straight leg component
			super(existingSolution, title, theLayers, validCuts);

		}

		public IStatus execute(IProgressMonitor monitor, IAdaptable info)
		{
			// find out the period
			TimePeriod period = super.getPeriod();

			initSolver();

			// ok, sort out the wizard
			_wizard = new NewStraightLegWizard(period);
			WizardDialog wd = new WizardDialog(Display.getCurrent().getActiveShell(),
					_wizard);
			wd.setTitle("New Straight Leg Forecast");
			wd.open();

			if (wd.getReturnCode() == WizardDialog.OK)
			{
				// ok, are there any?
				ArrayList<BaseContribution> conts = _wizard.getContributions();
				if (conts.size() > 0)
				{
					Iterator<BaseContribution> iter = conts.iterator();
					while (iter.hasNext())
					{
						BaseContribution baseContribution = (BaseContribution) iter.next();
						getSolver().addContribution(baseContribution);
					}
				}
			}

			return Status.CANCEL_STATUS;

		}

		@Override
		protected BaseContribution getContribution()
		{
			return null;
		}
	}

	private class CompositeStraightLegForecastContributionFromCuts extends
			ForecastContributionFromCuts
	{
		public CompositeStraightLegForecastContributionFromCuts(
				SATC_Solution existingSolution, String title, Layers theLayers,
				ArrayList<SensorContactWrapper> validCuts)
		{
			super(existingSolution, title, theLayers, validCuts);
		}

		public CompositeStraightLegForecastContributionFromCuts(
				SATC_Solution existingSolution, String title, Layers theLayers,
				TimePeriod period)
		{
			super(existingSolution, title, theLayers, period);
		}

		@Override
		protected BaseContribution getContribution()
		{
			return new CompositeStraightLegForecastContribution();
		}

	}

	@SuppressWarnings("unused")
	private class StraightLegForecastContributionFromCuts extends
			ForecastContributionFromCuts
	{
		public StraightLegForecastContributionFromCuts(
				SATC_Solution existingSolution, String title, Layers theLayers,
				ArrayList<SensorContactWrapper> validCuts)
		{
			super(existingSolution, title, theLayers, validCuts);
		}

		public StraightLegForecastContributionFromCuts(
				SATC_Solution existingSolution, String title, Layers theLayers,
				TimePeriod period)
		{
			super(existingSolution, title, theLayers, period);
		}

		@Override
		protected BaseContribution getContribution()
		{
			return new StraightLegForecastContribution();
		}

	}

	private abstract class ForecastContributionFromCuts extends
			CoreSolutionFromCuts
	{
		public ForecastContributionFromCuts(SATC_Solution existingSolution,
				String title, Layers theLayers,
				ArrayList<SensorContactWrapper> validCuts)
		{
			super(existingSolution, title, theLayers, new TimePeriod.BaseTimePeriod(
					new HiResDate(validCuts.get(0).getDTG().getDate()), new HiResDate(
							validCuts.get(validCuts.size() - 1).getDTG())));
		}

		public ForecastContributionFromCuts(SATC_Solution existingSolution,
				String title, Layers theLayers, TimePeriod timePeriod)
		{
			super(existingSolution, title, theLayers, timePeriod);
		}

		protected final BaseContribution createContribution(String contName)
		{
			// ok, now collate the contriubtion
			BaseContribution bmc = getContribution();

			// ok, do some formatting
			bmc.setName(contName);

			// and the dates
			bmc.setStartDate(thePeriod.getStartDTG().getDate());
			bmc.setFinishDate(thePeriod.getEndDTG().getDate());

			return bmc;
		}

		abstract protected BaseContribution getContribution();

	}

	private abstract class CoreSolutionFromCuts extends CMAPOperation
	{

		private SATC_Solution _targetSolution;
		private final Layers _theLayers;
		protected TimePeriod thePeriod;

		public CoreSolutionFromCuts(SATC_Solution existingSolution, String title,
				Layers theLayers, BaseContribution existingContribution)
		{
			super(title);
			_targetSolution = existingSolution;
			_theLayers = theLayers;
			thePeriod = new TimePeriod.BaseTimePeriod(new HiResDate(
					existingContribution.getStartDate()), new HiResDate(
					existingContribution.getFinishDate()));

		}

		public CoreSolutionFromCuts(SATC_Solution existingSolution, String title,
				Layers theLayers, TimePeriod thePeriod)
		{
			super(title);
			_targetSolution = existingSolution;
			_theLayers = theLayers;
			this.thePeriod = thePeriod;
		}

		protected TimePeriod getPeriod()
		{
			return thePeriod;
		}

		public String getDefaultSolutionName()
		{ // grab a name
			Date firstCutDate = thePeriod.getStartDTG().getDate();
			String formattedName = FormatRNDateTime.toString(firstCutDate.getTime());
			return formattedName;
		}

		@Override
		public boolean canUndo()
		{
			return false;
		}

		public IStatus execute(IProgressMonitor monitor, IAdaptable info)
		{

			// also, check that the maintain contributions window is open
			CorePlugin.openView(DebriefPlugin.SATC_MAINTAIN_CONTRIBUTIONS);

			initSolver();

			String contName = getContributionName();
			if (contName != null)
			{
				// do we wish to collate any other information?
				otherSteps();

				// ok = now get our specific contribution
				BaseContribution bmc = createContribution(contName);

				// and store it - if it worked
				if (bmc != null)
				{
					_targetSolution.addContribution(bmc);
				}
			}

			// also, check that the maintain contributions window is open - the user
			// is bound to be interested
			CorePlugin.openView(DebriefPlugin.SATC_MAINTAIN_CONTRIBUTIONS);

			// tell anybody that's interested that we have a current solver
			ISolversManager solversManager = SATC_Activator.getDefault().getService(
					ISolversManager.class, false);
			if (solversManager != null)
			{
				solversManager.setActiveSolver(_targetSolution.getSolver());
			}

			return Status.OK_STATUS;
		}

		protected void otherSteps()
		{
			// don't bother, we're an empty case
		}

		protected void initSolver()
		{
			// ok, do we have an existing solution
			if (_targetSolution == null)
			{
				String solutionName = getDefaultSolutionName();

				// create a new solution

				final ISolversManager solvMgr = SATC_Activator.getDefault().getService(
						ISolversManager.class, true);
				final ISolver newSolution = solvMgr.createSolver(solutionName);

				_targetSolution = new SATC_Solution(newSolution);
				_theLayers.addThisLayer(_targetSolution);

				// ok, give it the default contributions
				initialiseSolver();
			}
		}

		protected String getContributionName()
		{
			String res = null;
			// grab a name
			// create input box dialog
			InputDialog inp = new InputDialog(Display.getCurrent().getActiveShell(),
					"New contribution", "What is the name of this contribution",
					"name here", null);

			// did he cancel?
			if (inp.open() == InputDialog.OK)
			{
				// get the results
				res = inp.getValue();
			}
			return res;
		}

		private void initialiseSolver()
		{
			IContributions theConts = _targetSolution.getSolver().getContributions();
			theConts.addContribution(new LocationAnalysisContribution());
			theConts.addContribution(new SpeedAnalysisContribution());
			theConts.addContribution(new CourseAnalysisContribution());
		}

		public SATC_Solution getSolver()
		{
			return _targetSolution;
		}

		abstract protected BaseContribution createContribution(String contName);

		@Override
		public IStatus undo(IProgressMonitor monitor, IAdaptable info)
				throws ExecutionException
		{
			// duh, ignore
			return null;
		}
	}

	private class FrequencyMeasurementContributionFromCuts extends
			CoreSolutionFromCuts
	{
		private ArrayList<SensorContactWrapper> _validCuts;
		private double _soundSpeed;

		public FrequencyMeasurementContributionFromCuts(
				SATC_Solution existingSolution, String title, Layers theLayers,
				ArrayList<SensorContactWrapper> validCuts)
		{
			super(existingSolution, title, theLayers, new TimePeriod.BaseTimePeriod(
					new HiResDate(validCuts.get(0).getDTG().getDate()), new HiResDate(
							validCuts.get(validCuts.size() - 1).getDTG())));
			_validCuts = validCuts;
		}

		protected void otherSteps()
		{
			String res = null;
			// grab a name
			// create input box dialog
			InputDialog inp = new InputDialog(Display.getCurrent().getActiveShell(),
					"New frequency contribution",
					"What is Speed of Sound for this location (m/sec)", "Speed here",
					null);

			// did he cancel?
			if (inp.open() == InputDialog.OK)
			{
				// get the results
				res = inp.getValue();

				// try to convert to double
				_soundSpeed = Double.parseDouble(res);
			}
		}

		protected FrequencyMeasurementContribution createContribution(
				String contName)
		{
			// ok, now collate the contriubtion
			final FrequencyMeasurementContribution fmc = new FrequencyMeasurementContribution();
			fmc.setName(contName);

			// store the user-specified sound speed
			fmc.setSoundSpeed(_soundSpeed);

			boolean firstCut = true;

			// add the bearing data
			Iterator<SensorContactWrapper> iter = _validCuts.iterator();
			while (iter.hasNext())
			{
				final SensorContactWrapper scw = (SensorContactWrapper) iter.next();
				SensorWrapper sensor = scw.getSensor();
				final TrackWrapper host = sensor.getHost();

				Date date = scw.getDTG().getDate();

				if (scw.getHasFrequency())
				{
					double freq = scw.getFrequency();
					final FMeasurement thisM = new FMeasurement(date, freq);

					// give it the respective color
					thisM.setColor(scw.getColor());

					// ok, store it.
					fmc.addMeasurement(thisM);

					// is this the first cut?
					if (firstCut)
					{
						// ok, store the base frequency
						fmc.setBaseFrequency(sensor.getBaseFrequency());
						firstCut = false;
					}

					// we need to get the host status at this time
					Watchable[] statList = host.getNearestTo(scw.getTime());

					// do we know ownship state at this time?
					if (statList.length > 0)
					{
						Watchable stat = statList[0];
						double crseRads = stat.getCourse();
						double spdMs = new WorldSpeed(stat.getSpeed(), WorldSpeed.Kts)
								.getValueIn(WorldSpeed.M_sec);
						thisM.setState(crseRads, spdMs);
					}

					// also try to find out the sensor locaiton
					WorldLocation origin = scw.getCalculatedOrigin(host);
					thisM.setOrigin(conversions.toPoint(origin));

				}
				else
				{
					SATC_Activator.log(Status.WARNING, "Expected freq data at:" + date
							+ " to contain freq, it doesn't", null);
				}
			}

			return fmc;
		}

	}

}
