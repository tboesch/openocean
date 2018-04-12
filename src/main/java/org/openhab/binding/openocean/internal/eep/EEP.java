/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.openocean.internal.eep;

import static org.openhab.binding.openocean.OpenOceanBindingConstants.*;

import java.util.Arrays;
import java.util.Set;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.openocean.internal.messages.ERP1Message;
import org.openhab.binding.openocean.internal.transceiver.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Daniel Weber - Initial contribution
 */
public abstract class EEP {

    public static final int Zero = 0x00;
    protected static final int SenderIdLength = 4;
    protected static final int StatusLength = 1;
    protected static final int RORGLength = 1;

    protected int[] bytes;
    protected int[] optionalData;
    protected int[] senderId;
    protected int status;

    protected int[] destinationId;

    protected Logger logger = LoggerFactory.getLogger(EEP.class);

    private EEPType eepType = null;
    protected ERP1Message packet = null;

    public EEP() {
        // ctor for sending

        status = 0x00;
        senderId = null;
        bytes = null;
    }

    public EEP(ERP1Message packet) {
        // ctor for receiving

        // Todo validation??
        this.packet = packet;
        setData(packet.getPayload(RORGLength, getDataLength()));
        setSenderId(packet.getPayload(RORGLength + getDataLength(), SenderIdLength));
        setStatus(packet.getPayload(RORGLength + getDataLength() + SenderIdLength, 1)[0]);
        setOptionalData(packet.getOptionalPayload());
    }

    public EEP convertFromCommand(String channelId, Command command, State currentState, Configuration config) {
        if (!getSupportedChannels().contains(channelId)) {
            throw new IllegalArgumentException("Command " + command.toString() + " is not supported");
        }

        if (channelId.equals(CHANNEL_TEACHINCMD) && command == OnOffType.ON) {
            teachInQueryImpl(config);
        } else {
            convertFromCommandImpl(command, channelId, currentState, config);
        }
        return this;
    }

    public State convertToState(String channelId, Configuration config, State currentState) {
        if (!getSupportedChannels().contains(channelId)) {
            throw new IllegalArgumentException("Channel " + channelId + " is not supported");
        }

        if (channelId.equals(CHANNEL_RECEIVINGSTATE)) {
            return convertToReceivingState();
        }

        return convertToStateImpl(channelId, currentState, config);
    }

    public String convertToEvent(String channelId, String lastEvent, Configuration config) {
        if (!getSupportedChannels().contains(channelId)) {
            throw new IllegalArgumentException("Channel " + channelId + " is not supported");
        }

        return convertToEventImpl(channelId, lastEvent, config);
    }

    public EEP setData(int... bytes) {
        if (!validateData(bytes)) {
            throw new IllegalArgumentException();
        }

        this.bytes = Arrays.copyOf(bytes, bytes.length);
        return this;
    }

    public EEP setOptionalData(int... bytes) {
        if (bytes != null) {
            this.optionalData = Arrays.copyOf(bytes, bytes.length);
        }

        return this;
    }

    public EEP setSenderId(int[] senderId) {
        if (senderId == null || senderId.length != SenderIdLength) {
            throw new IllegalArgumentException();
        }

        this.senderId = Arrays.copyOf(senderId, senderId.length);
        return this;
    }

    public EEP setStatus(int status) {
        this.status = status;
        return this;
    }

    public final ERP1Message getERP1Message() {
        if (isValid()) {

            int optionalDataLength = 0;
            if (optionalData != null) {
                optionalDataLength = optionalData.length;
            }

            int[] payLoad = new int[RORGLength + getDataLength() + SenderIdLength + StatusLength + optionalDataLength];
            Arrays.fill(payLoad, Zero);
            payLoad[0] = getEEPType().getRORG().getValue();
            ERP1Message message = new ERP1Message(payLoad.length - optionalDataLength, optionalDataLength, payLoad);

            message.setPayload(Helper.concatAll(new int[] { getEEPType().getRORG().getValue() }, bytes, senderId,
                    new int[] { status }));

            message.setOptionalPayload(optionalData);

            return message;
        }

        return null;
    }

    protected boolean validateData(int[] bytes) {
        return bytes != null && bytes.length == getDataLength();
    }

    public boolean isValid() {
        return validateData(bytes) && senderId != null && senderId.length == SenderIdLength;
    }

    protected EEPType getEEPType() {
        if (eepType == null) {
            eepType = EEPType.getType(this.getClass());
        }

        return eepType;
    }

    protected int getDataLength() {
        if (getEEPType() != null) {
            return getEEPType().getRORG().getDataLength();
        }

        return 0;
    }

    public Set<String> getSupportedChannels() {
        return getEEPType().GetChannelIds();
    }

    protected void convertFromCommandImpl(Command command, String channelId, State currentState, Configuration config) {

    }

    protected State convertToStateImpl(String channelId, State currentState, Configuration config) {
        return UnDefType.UNDEF;
    }

    protected String convertToEventImpl(String channelId, String lastEvent, Configuration config) {
        return null;
    }

    protected void teachInQueryImpl(Configuration config) {

    }

    protected boolean getBit(int byteData, int bit) {
        int mask = (1 << bit);
        return (byteData & mask) != 0;
    }

    public ThingTypeUID getThingTypeUID() {
        return getEEPType().getThingTypeUID();
    }

    public int[] getSenderId() {
        return senderId;
    }

    public void addConfigPropertiesTo(DiscoveryResultBuilder discoveredThingResultBuilder) {
        discoveredThingResultBuilder.withProperty(PARAMETER_RECEIVINGEEPID, getEEPType().getId());
    }

    public EEP setDestinationId(int[] destinationId) {
        if (destinationId != null) {
            this.destinationId = Arrays.copyOf(destinationId, destinationId.length);
            setOptionalData(Helper.concatAll(new int[] { 0x01 }, destinationId, new int[] { 0xff, 0x00 }));
        }
        return this;
    }

    protected State convertToReceivingState() {
        if (this.optionalData == null || this.optionalData.length < 6) {
            return UnDefType.UNDEF;
        }

        return new StringType(String.format("Rssi %s, repeated %s", this.optionalData[5], this.status & 0b1111));
    }
}
