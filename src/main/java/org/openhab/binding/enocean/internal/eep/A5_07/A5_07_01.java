/**
 * Copyright (c) 2010-2019 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.enocean.internal.eep.A5_07;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.enocean.internal.messages.ERP1Message;

/**
 *
 * @author Daniel Weber - Initial contribution
 */
public class A5_07_01 extends A5_07 {

    private final Integer PIR_OFF = 0x7f;

    public A5_07_01(ERP1Message packet) {
        super(packet);
    }

    @Override
    protected State getIllumination() {
        return UnDefType.UNDEF;
    }

    @Override
    protected State getMotion() {
        return getDB_1Value() <= PIR_OFF ? OnOffType.OFF : OnOffType.ON;
    }

    @Override
    protected State getSupplyVoltage() {
        if (!getBit(getDB_0Value(), 0)) {
            return UnDefType.UNDEF;
        }

        return getSupplyVoltage(getDB_3Value());
    }

}