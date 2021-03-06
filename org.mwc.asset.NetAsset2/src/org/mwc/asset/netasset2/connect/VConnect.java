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
package org.mwc.asset.netasset2.connect;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.Vector;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.mwc.asset.netCore.common.Network;
import org.mwc.asset.netCore.common.Network.LightParticipant;
import org.mwc.asset.netCore.common.Network.LightScenario;

public class VConnect extends Composite implements IVConnect
{
	private final Table partTable;
	private final Button btnPing;
	private final ListViewer listServerViewer;
	private final ListViewer listScenarioViewer;
	private final List listServers;
	private final List listScenarios;
	private final TableViewer partViewer;
	private final Button btnDisconnect;
	// private Button btnManual;
	private final StringProvider _stringProvider;
	private final Button btnSelfHost;

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 * @param stringProvider
	 */
	public VConnect(final Composite parent, final int style)
	{
		this(parent, style, null);
	}

	public VConnect(final Composite parent, final int style, final StringProvider stringProvider)
	{
		super(parent, style);
		_stringProvider = stringProvider;
		setLayout(null);

		final Group grpConnection = new Group(this, SWT.NONE);
		grpConnection.setBounds(0, 0, 260, 225);

		final Label lblServers = new Label(grpConnection, SWT.NONE);
		lblServers.setBounds(10, 27, 59, 14);
		lblServers.setText("Servers");

		btnPing = new Button(grpConnection, SWT.FLAT);
		btnPing.setBounds(10, 3, 47, 21);
		btnPing.setText("Ping");

		final Label lblScenarios = new Label(grpConnection, SWT.NONE);
		lblScenarios.setText("Scenarios");
		lblScenarios.setBounds(106, 27, 59, 14);

		final Label lblParticipants = new Label(grpConnection, SWT.NONE);
		lblParticipants.setText("Participants");
		lblParticipants.setBounds(10, 113, 67, 14);

		partViewer = new TableViewer(grpConnection, SWT.BORDER | SWT.FULL_SELECTION);
		partTable = partViewer.getTable();
		partTable.setBounds(10, 129, 240, 81);

		final TableViewerColumn nameCol = new TableViewerColumn(partViewer, SWT.NONE);
		final TableColumn colName = nameCol.getColumn();
		colName.setWidth(50);
		colName.setText("Name");

		final TableViewerColumn catCol = new TableViewerColumn(partViewer, SWT.NONE);
		final TableColumn colCategory = catCol.getColumn();
		colCategory.setWidth(100);
		colCategory.setText("Category");

		final TableViewerColumn actCol = new TableViewerColumn(partViewer, SWT.NONE);
		final TableColumn colActivity = actCol.getColumn();
		colActivity.setWidth(100);
		colActivity.setText("Activity");

		listServerViewer = new ListViewer(grpConnection, SWT.BORDER | SWT.V_SCROLL);
		listServers = listServerViewer.getList();
		listServers.setBounds(10, 41, 100, 66);

		listScenarioViewer = new ListViewer(grpConnection, SWT.BORDER
				| SWT.V_SCROLL);
		listScenarios = listScenarioViewer.getList();
		listScenarios.setBounds(116, 41, 130, 66);

		btnDisconnect = new Button(grpConnection, SWT.FLAT);
		btnDisconnect.setText("Disconnect");
		btnDisconnect.setBounds(171, 10, 79, 31);

		btnSelfHost = new Button(grpConnection, SWT.CHECK);
		btnSelfHost.setBounds(76, 5, 79, 18);
		btnSelfHost.setText("Self host");

		// btnManual = new Button(grpConnection, SWT.FLAT);
		// btnManual.setBounds(63, 3, 67, 21);
		// btnManual.setText("Manual");

	}

	@Override
	protected void checkSubclass()
	{
		// Disable the check that prevents subclassing of SWT components
	}

	@Override
	public void addManualListener(final ClickHandler handler)
	{
		// btnManual.addSelectionListener(new SelectionListener()
		// {
		// public void widgetSelected(SelectionEvent e)
		// {
		// handler.clicked();
		// }
		//
		// public void widgetDefaultSelected(SelectionEvent e)
		// {
		// }
		// });
	}

	@Override
	public void addPingListener(final ClickHandler handler)
	{
		btnPing.addSelectionListener(new SelectionListener()
		{
			public void widgetSelected(final SelectionEvent e)
			{
				handler.clicked();
			}

			public void widgetDefaultSelected(final SelectionEvent e)
			{
			}
		});
	}

	@Override
	public void addDisconnectListener(final ClickHandler handler)
	{
		btnDisconnect.addSelectionListener(new SelectionListener()
		{
			public void widgetSelected(final SelectionEvent e)
			{
				handler.clicked();
			}

			public void widgetDefaultSelected(final SelectionEvent e)
			{
			}
		});
	}

	@Override
	public void addServerListener(final ServerSelected listener)
	{
		listServerViewer.addDoubleClickListener(new IDoubleClickListener()
		{
			public void doubleClick(final DoubleClickEvent event)
			{
				final ISelection sel = event.getSelection();
				final StructuredSelection ss = (StructuredSelection) sel;
				final InetAddress address = (InetAddress) ss.getFirstElement();
				listener.selected(address);
			}
		});
	}

	@Override
	public void disableServers()
	{
		Display.getDefault().asyncExec(new Runnable()
		{
			@Override
			public void run()
			{
				if (!listServers.isDisposed())
					listServers.setEnabled(false);
			}
		});
	}

	@Override
	public void enableServers()
	{
		Display.getDefault().asyncExec(new Runnable()
		{
			@Override
			public void run()
			{
				if (!listServers.isDisposed())
					listServers.setEnabled(true);
			}
		});
	}

	@Override
	public void disableScenarios()
	{
		Display.getDefault().asyncExec(new Runnable()
		{
			@Override
			public void run()
			{
				if (!listScenarios.isDisposed())
					listScenarios.setEnabled(false);
			}
		});
	}

	@Override
	public void enableScenarios()
	{
		Display.getDefault().asyncExec(new Runnable()
		{
			@Override
			public void run()
			{
				if (!listScenarios.isDisposed())
					listScenarios.setEnabled(true);
			}
		});
	}

	@Override
	public void addScenarioListener(final ScenarioSelected listener)
	{
		listScenarioViewer.addDoubleClickListener(new IDoubleClickListener()
		{
			public void doubleClick(final DoubleClickEvent event)
			{
				final ISelection sel = event.getSelection();
				final StructuredSelection ss = (StructuredSelection) sel;
				final LightScenario scenario = (LightScenario) ss.getFirstElement();
				listener.selected(scenario);
			}
		});
	}

	@Override
	public void setPartContentProvider(final IContentProvider provider)
	{
		partViewer.setContentProvider(provider);
	}

	@Override
	public void setPartLabelProvider(final IBaseLabelProvider labelProvider)
	{
		partViewer.setLabelProvider(labelProvider);
	}

	@Override
	public void addParticipantListener(final ParticipantSelected listener)
	{
		partViewer.addDoubleClickListener(new IDoubleClickListener()
		{
			public void doubleClick(final DoubleClickEvent event)
			{
				final ISelection sel = event.getSelection();
				final StructuredSelection ss = (StructuredSelection) sel;
				final LightParticipant part = (LightParticipant) ss.getFirstElement();
				listener.selected(part);
			}
		});
	}

	@Override
	public void setParticipants(final Vector<LightParticipant> listOfParticipants)
	{
		Display.getDefault().asyncExec(new Runnable()
		{
			@Override
			public void run()
			{
				partViewer.setInput(listOfParticipants);
			}
		});
	}

	@Override
	public void setScenarios(final Vector<LightScenario> results)
	{
		Display.getDefault().asyncExec(new Runnable()
		{
			@Override
			public void run()
			{
				listScenarioViewer.getList().removeAll();
				final Iterator<LightScenario> items = results.iterator();
				while (items.hasNext())
				{
					final Network.LightScenario ls = (Network.LightScenario) items.next();
					listScenarioViewer.add(ls);
				}
			}
		});
	}

	@Override
	public void setServers(final java.util.List<InetAddress> adds)
	{
		Display.getDefault().asyncExec(new Runnable()
		{
			@Override
			public void run()
			{
				final Iterator<InetAddress> items = adds.iterator();
				while (items.hasNext())
				{
					final InetAddress inetAddress = (InetAddress) items.next();
					listServerViewer.add(inetAddress);
				}
			}
		});
	}

	@Override
	public void disableParticipants()
	{
		Display.getDefault().asyncExec(new Runnable()
		{
			@Override
			public void run()
			{
				if (!partTable.isDisposed())
					partTable.setEnabled(false);
			}
		});
	}

	@Override
	public void enableParticipants()
	{
		Display.getDefault().asyncExec(new Runnable()
		{
			@Override
			public void run()
			{
				if (!partTable.isDisposed())
					partTable.setEnabled(true);
			}
		});
	}

	@Override
	public void enableDisconnect()
	{
		if (!btnDisconnect.isDisposed())
			btnDisconnect.setEnabled(true);
	}

	@Override
	public void disableDisconnect()
	{
		if (!btnDisconnect.isDisposed())
			btnDisconnect.setEnabled(false);
	}

	@Override
	public String getString(final String title, final String message)
	{
		return _stringProvider.getString(title, message);
	}

	@Override
	public void addSelfHostListener(final BooleanHandler handler)
	{
		btnSelfHost.addSelectionListener(new SelectionAdapter()
		{

			@Override
			public void widgetSelected(final SelectionEvent e)
			{
				handler.change(btnSelfHost.getSelection());
			}

		});
	}

	@Override
	public void clearServers()
	{
		Display.getDefault().asyncExec(new Runnable()
		{
			@Override
			public void run()
			{
				listServerViewer.getList().removeAll();
			}
		});
	}
}
