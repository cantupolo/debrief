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
package MWC.GUI.Swing;

import javax.swing.JTextArea;
import javax.swing.LookAndFeel;

/**
 * Created by IntelliJ IDEA.
 * User: Ian.Mayo
 * Date: 15-Oct-2004
 * Time: 14:17:55
 * To change this template use File | Settings | File Templates.
 */
public class MultiLineLabel extends JTextArea
{
  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
   * Constructs a new TextArea.  A default model is set, the initial string
   * is null, and rows/columns are set to 0.
   */
  public MultiLineLabel()
  {
  }

  /**
   * Reloads the pluggable UI.  The key used to fetch the
   * new interface is <code>getUIClassID()</code>.  The type of
   * the UI is <code>TextUI</code>.  <code>invalidate</code>
   * is called after setting the UI.
   */
  public void updateUI()
  {

    // get the parent to update
    super.updateUI();

    // setup the layout
    setLineWrap(true);
    setWrapStyleWord(true);
    setHighlighter(null);
    setEditable(false);

    // and the look & feel to look like a label
    LookAndFeel.installBorder(this, "Label.border");
    LookAndFeel.installColorsAndFont(this,"Label.background",
                                     "Label.foreground", "Label.font");
  }
}
