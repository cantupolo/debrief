package org.mwc.debrief.satc_interface;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.progress.WorkbenchJob;
import org.mwc.cmap.core.property_support.EditableWrapper;
import org.mwc.cmap.core.ui_support.PartMonitor;
import org.mwc.debrief.satc_interface.data.SATC_Solution;
import org.osgi.framework.BundleContext;

import MWC.GUI.Editable;

import com.planetmayo.debrief.satc.model.manager.ISolversManager;
import com.planetmayo.debrief.satc_rcp.SATC_Activator;

/**
 * The activator class controls the plug-in life cycle
 */
public class SATC_Interface_Activator extends AbstractUIPlugin
{

	// The plug-in ID
	public static final String PLUGIN_ID = "org.mwc.debrief.satc_interface"; //$NON-NLS-1$

	// The shared instance
	private static SATC_Interface_Activator plugin;

	private PartMonitor _myPartMonitor;

	private ISelectionChangedListener _selectionChangeListener;

	/**
	 * The constructor
	 */
	public SATC_Interface_Activator()
	{
		_selectionChangeListener = new ISelectionChangedListener()
		{
			public void selectionChanged(final SelectionChangedEvent event)
			{
				// right, see what it is
				final ISelection sel = event.getSelection();
				if (sel instanceof StructuredSelection)
				{
					final StructuredSelection ss = (StructuredSelection) sel;
					final Object str = ss.getFirstElement();
					if (str instanceof EditableWrapper)
					{
						EditableWrapper ew = (EditableWrapper) str;
						Editable datum = ew.getEditable();
						if (datum instanceof SATC_Solution)
						{
							final SATC_Solution pw = (SATC_Solution) datum;
							editableSelected(ss, pw);
						}
					}
				}
			}
		};
	}

	protected void editableSelected(StructuredSelection sel, SATC_Solution pw)
	{
		// ok - it's been selected - tell the SATC manager
		ISolversManager solver = SATC_Activator.getDefault().getService(
				ISolversManager.class, true);
		solver.setActiveSolver(pw.getSolver());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext context) throws Exception
	{
		super.start(context);
		plugin = this;

		WorkbenchJob job = new WorkbenchJob("Just a UI Job")
		{

			@Override
			public IStatus runInUIThread(IProgressMonitor monitor)
			{
				initPartMonitor();
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	private void initPartMonitor()
	{
		if (_myPartMonitor == null)
		{
			// register ourselves as a part monitor (We need to know about selections,
			// to tie ourselves into the SATC code
			_myPartMonitor = new PartMonitor(PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getPartService());

			// also register as a selection listener
			_myPartMonitor.addPartListener(ISelectionProvider.class,
					PartMonitor.ACTIVATED, new PartMonitor.ICallback()
					{
						public void eventTriggered(final String type, final Object part,
								final IWorkbenchPart parentPart)
						{
							final ISelectionProvider iS = (ISelectionProvider) part;
							iS.addSelectionChangedListener(_selectionChangeListener);
						}
					});
			_myPartMonitor.addPartListener(ISelectionProvider.class,
					PartMonitor.DEACTIVATED, new PartMonitor.ICallback()
					{
						public void eventTriggered(final String type, final Object part,
								final IWorkbenchPart parentPart)
						{
							final ISelectionProvider iS = (ISelectionProvider) part;
							iS.removeSelectionChangedListener(_selectionChangeListener);
						}
					});
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	public void stop(BundleContext context) throws Exception
	{
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static SATC_Interface_Activator getDefault()
	{
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path
	 * 
	 * @param path
	 *          the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path)
	{
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
