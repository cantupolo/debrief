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
package org.mwc.asset.netCore.core;

import java.io.IOException;
import java.util.Vector;

import org.mwc.asset.netCore.common.Network;
import org.mwc.asset.netCore.common.Network.LightScenario;
import org.mwc.asset.netCore.common.Network.ScenControl;

import ASSET.Participants.ParticipantDecidedListener;
import ASSET.Participants.ParticipantDetectedListener;
import ASSET.Participants.ParticipantMovedListener;
import ASSET.Scenario.ScenarioSteppedListener;

public interface IMClient
{

	public abstract void releasePart(String scenario, int partId);

	public abstract void controlPart(String scenario, int id, double courseDegs,
			double speedKts, double depthM);

	public abstract void getScenarioList(final Network.AHandler<Vector<LightScenario>> handler);

	public abstract void stopListenPart(String scenarioName, int participantId);

	public abstract void step(String scenarioName);

	public abstract void stopListenScen(String scenarioName);

	public abstract void listenScen(String scenarioName, ScenarioSteppedListener listener);

	public abstract void listenPart(String scenarioName, int participantId, ParticipantMovedListener moveL,
			ParticipantDecidedListener decider, ParticipantDetectedListener detector);

	public abstract void stop();

	public abstract void connect(String target) throws IOException;

	public abstract java.util.List<java.net.InetAddress> discoverHosts();

	public abstract void controlScen(ScenControl sc);

	void stop(String scenarioName);

}
