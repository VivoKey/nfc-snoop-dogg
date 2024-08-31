/*
 * MIT License
 * 
 * Copyright (c) 2022 snake-4
 * Copyright (c) 2024 VivoKey
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.vivokey.nfcsnoopdecoder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class NCIDecoder {
    // Enums
    enum PacketDirection {
        CPUToNFCC,
        NFCCToCPU
    }

    enum NCIPacketType {
        Data,
        ControlCommand,
        ControlResponse,
        ControlNotification,
        RFU
    }

    enum GroupTypes {
        NCI_Core,
        RF_Management,
        NFCEE_Management,
        Proprietary,
        RFU
    }

    enum NCI_CoreOpcodes {
        CORE_RESET,
        CORE_INIT,
        CORE_SET_CONFIG,
        CORE_GET_CONFIG,
        CORE_CONN_CREATE,
        CORE_CONN_CLOSE,
        CORE_CONN_CREDITS,
        CORE_GENERIC_ERROR,
        CORE_INTERFACE_ERROR,
        RFU
    }

    enum RF_ManagementOpcodes {
        RF_DISCOVER_MAP,
        RF_SET_LISTEN_MODE_ROUTING,
        RF_GET_LISTEN_MODE_ROUTING,
        RF_DISCOVER,
        RF_DISCOVER_SELECT,
        RF_INTF_ACTIVATED,
        RF_DEACTIVATE,
        RF_FIELD_INFO,
        RF_T3T_POLLING,
        RF_NFCEE_ACTION,
        RF_NFCEE_DISCOVERY_REQ,
        RF_PARAMETER_UPDATE,
        RFU
    }

    enum NFCEE_ManagementOpcodes {
        NFCEE_DISCOVER,
        NFCEE_MODE_SET,
        RFU
    }

    // NCIPacket classes
    static class NCIPacketParser {
        public static NCIPacket parse(PacketDirection direction, byte[] data, long timestamp) {
            switch (NCIPacket.decodeMessageType(data[0])) {
                case Data:
                    return new NCIDataPacket(direction, data, timestamp);
                case ControlNotification:
                case ControlCommand:
                case ControlResponse:
                    return new NCIControlPacket(direction, data, timestamp);
                default:
                    return new NCIUnknownPacket(direction, data, timestamp);
            }
        }
    }

    static class NCIControlPacket extends NCIPacket {
        public byte groupID;
        public byte opcodeID;
        public byte payloadLength;
        public byte[] payload;

        public String getControlOpcodeTypeString() {
            String retVal = "";
            switch (nciEnumMemberOrRFU(GroupTypes.class, groupID)) {
                case NCI_Core:
                    retVal += nciEnumMemberOrRFU(NCI_CoreOpcodes.class, opcodeID).toString();
                    break;
                case RF_Management:
                    retVal += nciEnumMemberOrRFU(RF_ManagementOpcodes.class, opcodeID).toString();
                    break;
                case NFCEE_Management:
                    retVal += nciEnumMemberOrRFU(NFCEE_ManagementOpcodes.class, opcodeID).toString();
                    break;
                case Proprietary:
                    retVal += "GID_Proprietary";
                    break;
                case RFU:
                    retVal += "GID_RFU";
                    break;
            }
            switch (messageTypeDecoded) {
                case ControlCommand:
                    retVal += "_CMD";
                    break;
                case ControlResponse:
                    retVal += "_RSP";
                    break;
                case ControlNotification:
                    retVal += "_NTF";
                    break;
            }

            return retVal;
        }

        public NCIControlPacket(PacketDirection direction, byte[] data, long timestamp) {
            super(direction, data, timestamp);
            this.groupID = (byte)(rawData[0] & 0b0000_1111);
            this.opcodeID = (byte)(rawData[1] & 0b0011_1111);
            this.payloadLength = rawData[2];
            this.payload = Arrays.copyOfRange(rawData, 3, 3 + this.payloadLength);
        }
    }

    static class NCIDataPacket extends NCIPacket {
        public byte connID;
        public byte payloadLength;
        public byte[] payload;

        public NCIDataPacket(PacketDirection direction, byte[] data, long timestamp) {
            super(direction, data, timestamp);
            this.connID = (byte)(rawData[0] & 0b0000_1111);
            this.payloadLength = rawData[2];
            this.payload = Arrays.copyOfRange(rawData, 3, 3 + this.payloadLength);
        }
    }

    static class NCIUnknownPacket extends NCIPacket {
        public NCIUnknownPacket(PacketDirection direction, byte[] data, long timestamp) {
            super(direction, data, timestamp);
        }
    }

    static abstract class NCIPacket {
        public byte packetBoundaryFlag;
        public NCIPacketType messageTypeDecoded;
        public PacketDirection packetDirection;
        public long timestamp;
        public byte[] rawData;

        protected NCIPacket(PacketDirection direction, byte[] data, long timestamp) {
            this.packetDirection = direction;
            this.rawData = data;
            this.timestamp = timestamp;
            this.packetBoundaryFlag = (byte)((rawData[0] & 0b0001_0000) >> 4);
            this.messageTypeDecoded = decodeMessageType(rawData[0]);
        }

        public static NCIPacketType decodeMessageType(byte firstByte) {
            byte mt = (byte)((firstByte & 0b1110_0000) >> 5);
            return nciEnumMemberOrRFU(NCIPacketType.class, mt);
        }
    }

    // Utility method for enum handling
    private static <T extends Enum<T>> T nciEnumMemberOrRFU(Class<T> enumClass, int value) {
        T[] constants = enumClass.getEnumConstants();
        if (value >= 0 && value < constants.length - 1) {
            return constants[value];
        }
        return constants[constants.length - 1]; // Return RFU
    }
}

