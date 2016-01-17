package org.debrief.limpet_integration;

import info.limpet.IContext;
import info.limpet.IOperation;
import info.limpet.IStore;
import info.limpet.IStoreGroup;
import info.limpet.IStoreItem;
import info.limpet.data.operations.admin.OperationsLibrary;
import info.limpet.data.store.IGroupWrapper;
import info.limpet.ui.RCPContext;
import info.limpet.ui.data_provider.data.LimpetWrapper;
import info.limpet.ui.editors.DataManagerEditor;
import info.limpet.ui.editors.RCPOperationsLibrary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.debrief.limpet_integration.adapters.LimpetTrack;
import org.debrief.limpet_integration.data.StoreWrapper;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.mwc.cmap.core.property_support.RightClickSupport.AlternateRightClickContextItemGenerator;

import MWC.GUI.Editable;
import MWC.GUI.Layer;
import MWC.GUI.Layers;

public class DebriefLimpetMenuGenerator implements
    AlternateRightClickContextItemGenerator
{

  final IContext myContext;

  public DebriefLimpetMenuGenerator()
  {
    myContext = new RCPContext();
  }

  private ArrayList<IAdapterFactory> _adapters;
  private TopLevelTarget _topLevel;

  @Override
  public void generate(IMenuManager parent, final Layers theLayers,
      final Collection<Editable> parentLayers,
      final Collection<Editable> subjects)
  {

    // see if we can learn about the top level for the data
    Layer topLayer = null;

    // build up a selection
    List<IStoreItem> selection = new ArrayList<IStoreItem>();
    Iterator<Editable> iter = subjects.iterator();
    while (iter.hasNext())
    {
      Editable editable = (Editable) iter.next();
      // see if it's of interest
      if (editable instanceof LimpetWrapper)
      {
        LimpetWrapper wrapper = (LimpetWrapper) editable;
        IStoreItem item = (IStoreItem) wrapper.getSubject();
        selection.add(item);
      }
      else
      {
        // hmm, do we have another way of getting it?
        if (_adapters != null)
        {
          Iterator<IAdapterFactory> aIter = _adapters.iterator();
          while (aIter.hasNext())
          {
            IAdapterFactory iAdapterFactory = (IAdapterFactory) aIter.next();
            Object match = iAdapterFactory.getAdapter(editable,
                IStoreItem.class);
            if (match != null)
            {
              // ok, we've got an item
              selection.add((IStoreItem) match);
              break;
            }
          }
        }
      }
    }

    // ok, have we found anything suitable?
    IStore destination = null;

    // ok, find a store, where we can put the results
    if (parentLayers.size() > 0)
    {
      Editable parentE = parentLayers.iterator().next();
      if (parentE instanceof IGroupWrapper)
      {
        IGroupWrapper tgt = (IGroupWrapper) parentE;

        final IStoreGroup group = tgt.getGroup();

        // TODO: we shouldn't have difference between IStore and IStoreGroup
        destination = new StoreGroupAsStore(group);

        // hmm, can we find the top level layer?
        if (group instanceof LimpetTrack)
        {
          LimpetTrack lt = (LimpetTrack) group;
          topLayer = lt.getTrack();
        }

      }
      else if (parentE instanceof StoreWrapper)
      {
        StoreWrapper store = (StoreWrapper) parentE;
        destination = store.getStore();
      }
      else if (parentE instanceof IStore)
      {
        TopLevelTarget tgt = (TopLevelTarget) parentE;
        destination = tgt;
      }
    }

    // hmm, do we need to offer a top-level destination?
    if (destination == null)
    {
      // do we have a top level object?
      if (_topLevel == null)
      {
        // look in the layers
        _topLevel = (TopLevelTarget) theLayers
            .findLayer(TopLevelTarget.LAYER_NAME);

        // hmm, did it work?
        if (_topLevel == null)
        {
          // ok, create a deferred one
          _topLevel = new TopLevelTarget(theLayers);
        }
      }

      destination = _topLevel;
    }

    // get the list of operations
    HashMap<String, List<IOperation<?>>> ops = OperationsLibrary
        .getOperations();

    // and the RCP-specific operations
    HashMap<String, List<IOperation<?>>> rcpOps = RCPOperationsLibrary
        .getOperations();
    ops.putAll(rcpOps);

    // did we find anything?
    Iterator<String> hIter = ops.keySet().iterator();

    // change listener
    final Layer finalDestination = topLayer;

    Runnable changeListener = new Runnable()
    {

      @Override
      public void run()
      {
        if (finalDestination != null)
        {
          theLayers.fireExtended(null, finalDestination);
        }
        else
        {
          theLayers.fireExtended();
        }
      }
    };

    while (hIter.hasNext())
    {
      // ok, we're in a menu grouping
      String name = (String) hIter.next();

      // create a new menu tier
      MenuManager newM = new MenuManager(name);
      parent.add(newM);

      // now loop through this set of operations
      List<IOperation<?>> values = ops.get(name);

      DataManagerEditor.showThisList(selection, newM, values, destination,
          myContext, changeListener);
    }

  }

  public void addAdapterFactory(IAdapterFactory newFactory)
  {
    if (_adapters == null)
    {
      _adapters = new ArrayList<IAdapterFactory>();
    }
    _adapters.add(newFactory);
  }

}
