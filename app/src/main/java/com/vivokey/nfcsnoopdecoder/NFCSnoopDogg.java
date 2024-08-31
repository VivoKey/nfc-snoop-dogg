package com.vivokey.nfcsnoopdecoder;

import com.genymobile.scrcpy.util.Command;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.compress.compressors.deflate.DeflateCompressorInputStream;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class NFCSnoopDogg {

    private static final String BEGIN_MARKER = "BEGIN:NFCSNOOP_LOG_SUMMARY";
    private static final String END_MARKER = "END:NFCSNOOP_LOG_SUMMARY";

    public static void main(String[] args) {
        boolean verbose = args.length > 0 && args[0].equals("--verbose");

        System.out.println("NFC-SNOOP-DOGG 🐶");
        System.out.println("VivoKey © 2024\n");

        checkSnoopLogMode();

        try {
            mainLoop(verbose);
        } catch (Exception e) {
            System.err.print(android.util.Log.getStackTraceString(e));
        }
    }

    private static void mainLoop(boolean verbose) throws Exception {
        String tail = null;

        while (true) {
            String dumpsysOutput = Command.execReadOutput("dumpsys", "nfc");
            String base64 = findSnoopLogBuffer(dumpsysOutput);
            byte[] rawLog = extractSnoopLog(base64);
            String dump = parseSnoopLog(rawLog);

            if (tail == null) {
                tail = getTail(dump);
                continue;
            }

            printNewPackets(dump, tail, verbose);

            tail = getTail(dump);
        }
    }

    private static void checkSnoopLogMode() {
        try {
            if (!checkProperty("persist.nfc.snoop_log_mode") && !checkProperty("persist.nfc.nfcsnooplogmode")) {
                System.out.println("WARNING: Could not detect snoop log mode");
            }
        } catch (Exception e) {
            System.err.print(android.util.Log.getStackTraceString(e));
        }
    }

    private static boolean checkProperty(String property) throws IOException, InterruptedException {
        if ("enum full filtered".equals(Command.execReadOutput("getprop", "-T", property).trim())) {
            String mode = Command.execReadOutput("getprop", property).trim();

            if (mode.isEmpty()) {
                System.out.println("WARNING: " + property + " is not set");
            } else if ("filtered".equals(mode)) {
                System.out.println("WARNING: " + property + " is set to \"filtered\"");
            }

            return true;
        } else {
            return false;
        }
    }

    private static String findSnoopLogBuffer(String dumpsysOutput) {
        int startIndex = dumpsysOutput.lastIndexOf(BEGIN_MARKER);
        startIndex = dumpsysOutput.indexOf("\n", startIndex) + 1;

        int endIndex = dumpsysOutput.lastIndexOf(END_MARKER);
        endIndex = dumpsysOutput.lastIndexOf("\n", endIndex);

        return dumpsysOutput.substring(startIndex, endIndex);
    }

    private static byte[] extractSnoopLog(String base64) throws IOException {
        byte[] data = Base64.getDecoder().decode(base64.replace("\n", ""));

        return IOUtils.toByteArray(new DeflateCompressorInputStream(new ByteArrayInputStream(data, 9, data.length - 9)));
    }

    private static String parseSnoopLog(byte[] rawLog) {
        ByteBuffer buffer = ByteBuffer.wrap(rawLog).order(ByteOrder.LITTLE_ENDIAN);

        List<String> lines = new ArrayList<>();

        while (buffer.hasRemaining()) {
            lines.add(new NFCPacket(buffer).format());
        }

        return String.join("\n", lines) + "\n";
    }

    private static void printNewPackets(String dump, String tail, boolean verbose) throws DecoderException {
        int offset = dump.indexOf(tail) + tail.length();

        if (offset == dump.length()) {
            return;
        }

        String[] packets = dump.substring(offset).split("\n");

        for (String packet : packets) {
            NCIDecoder.NCIPacket nci = parsePacket(packet);

            if (verbose || nci instanceof NCIDecoder.NCIDataPacket) {
                printNciPacket(nci, verbose);
            }
        }
    }

    private static String getTail(String dump) {
        String[] lines = dump.split("\n");

        String[] tail = Arrays.copyOfRange(lines, lines.length / 2, lines.length);

        return String.join("\n", tail) + "\n";
    }

    private static NCIDecoder.NCIPacket parsePacket(String packet) throws DecoderException {
        String[] splitPacket = packet.split(",");

        boolean isReceived = splitPacket[1].trim().equals("true");
        NCIDecoder.PacketDirection dir = isReceived ? NCIDecoder.PacketDirection.NFCCToCPU : NCIDecoder.PacketDirection.CPUToNFCC;

        byte[] rawData = Hex.decodeHex(splitPacket[2].trim());

        return NCIDecoder.NCIPacketParser.parse(dir, rawData, 0);
    }

    private static void printNciPacket(NCIDecoder.NCIPacket packet, boolean verbose) {
        String prefix = packet.packetDirection == NCIDecoder.PacketDirection.NFCCToCPU ? "<--" : "-->";

        if (verbose) {
            prefix += String.format(" [%s]", packet.messageTypeDecoded);
        }

        if (packet instanceof NCIDecoder.NCIDataPacket) {
            printDataPacket(prefix, (NCIDecoder.NCIDataPacket) packet, verbose);
        } else if (packet instanceof NCIDecoder.NCIControlPacket) {
            printControlPacket(prefix, (NCIDecoder.NCIControlPacket) packet);
        }
    }

    private static void printDataPacket(String prefix, NCIDecoder.NCIDataPacket packet, boolean verbose) {
        System.out.printf("%s %s%n", prefix, byteArrayToHex(verbose ? packet.rawData : packet.payload));
    }

    private static void printControlPacket(String prefix, NCIDecoder.NCIControlPacket packet) {
        System.out.printf("%s [%s] %s%n", prefix, packet.getControlOpcodeTypeString(), byteArrayToHex(packet.rawData));
    }

    private static String byteArrayToHex(byte[] bytes) {
        return Hex.encodeHexString(bytes, false);
    }

}
