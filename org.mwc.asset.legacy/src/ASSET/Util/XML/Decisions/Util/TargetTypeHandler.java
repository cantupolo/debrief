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
package ASSET.Util.XML.Decisions.Util;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 *
 * @author
 * @version 1.0
 */

abstract public class TargetTypeHandler extends MWC.Utilities.ReaderWriter.XML.MWCXMLReader
{

  static private final String type = "TargetType";
  static private final String ANDoperation = "AND";
  
  private Boolean doAND = null;
  
//  static private final String TYPE_NAME = "Type";
//  static private final String ATTRIBUTE_NAME = "Name";

  ASSET.Models.Decision.TargetType tt = new ASSET.Models.Decision.TargetType();

  public TargetTypeHandler(final String myType)
  {
    super(myType);

    addAttributeHandler(new HandleBooleanAttribute(ANDoperation)
    {
      public void setValue(String name, final boolean val)
      {
      	doAND = val;
      }
    });
    
    addHandler(new TypeHandler()
    {
      public void addType(final String attr)
      {
        if (tt == null)
          tt = new ASSET.Models.Decision.TargetType();

        // see if this is an environment
        String val = ASSET.Participants.Category.checkEnv(attr);

        // no, see if it's a force
        if (val == null)
          val = ASSET.Participants.Category.checkForce(attr);

        // no, see if it's a type
        if (val == null)
          val = ASSET.Participants.Category.checkType(attr);

        tt.addTargetType(val);
      }
    });

  }

  public TargetTypeHandler()
  {
    this(type);

  }

//  private void addThis(final String type)
//  {
//    tt.addTargetType(type);
//  }

  public void elementClosed()
  {
    // pass to parent
    setTargetType(tt);
    
    // test to comparison
    if(doAND != null)
    	tt.setANDOperation(doAND);

    // restart
    tt = null;
  }

  abstract public void setTargetType(ASSET.Models.Decision.TargetType type);


  /**
   * get just the element as XML
   */
  static public org.w3c.dom.Element getElement(final ASSET.Models.Decision.TargetType targetType,
                                               final org.w3c.dom.Document doc)
  {
    return getElement(type, targetType, doc);
  }

  static public org.w3c.dom.Element getElement(final String elementName, final ASSET.Models.Decision.TargetType type1,
                                               final org.w3c.dom.Document doc)
  {
    // create the element to store it in
    final org.w3c.dom.Element el = doc.createElement(elementName);

    // step through targets
    final java.util.Collection<String> col = type1.getTargets();
    if (col != null)
    {
      //
      final java.util.Iterator<String> it = col.iterator();

      // step through the types
      while (it.hasNext())
      {
        final String nx = (String) it.next();
        TypeHandler.exportThis(nx, el, doc);
      }
    }

    return el;
  }

  static public void exportThis(final Object toExport, final org.w3c.dom.Element parent,
                                final org.w3c.dom.Document doc)
  {
    exportThis(type, toExport, parent, doc);
  }

  static public void exportThis(final String elementName, final Object toExport, final org.w3c.dom.Element parent,
                                final org.w3c.dom.Document doc)
  {
    // convert it
    final ASSET.Models.Decision.TargetType type1 = (ASSET.Models.Decision.TargetType) toExport;
    

    // is it a real object?
    if (type1 != null)
    {
      // get it as an element
      final org.w3c.dom.Element el = getElement(elementName, type1, doc);

      el.setAttribute(ANDoperation, writeThis(type1.getANDOperation()));
      
      // add to parent
      parent.appendChild(el);
    }
  }

  static abstract public class TypeHandler extends MWC.Utilities.ReaderWriter.XML.MWCXMLReader
  {
    static final private String thisType = "Type";
    static final private String ATTRIBUTE = "Name";
//    private String _thisType;

    public TypeHandler()
    {
      super(thisType);
      addAttributeHandler(new HandleAttribute(ATTRIBUTE)
      {
        public void setValue(String name, final String val)
        {
          addType(val);
        }
      });
    }

    abstract public void addType(String attr);

    static public void exportThis(final String type1, final org.w3c.dom.Element parent, final org.w3c.dom.Document doc)
    {
      final org.w3c.dom.Element myE = doc.createElement(thisType);
      myE.setAttribute(ATTRIBUTE, type1);
      parent.appendChild(myE);
    }

  }


}